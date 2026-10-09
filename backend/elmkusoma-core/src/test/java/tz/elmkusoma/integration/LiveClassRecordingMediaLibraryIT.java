package tz.elmkusoma.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Live class recording -> Media Library, driven through the real signed LiveKit webhook.
 *
 * <p>This is the end-to-end proof that a finished recording is no longer just a
 * {@code live_classes.recording_url} string: the webhook must produce a real, tenant-scoped
 * {@code media_assets} row keyed on {@code source_type=LIVE_CLASS}. It also pins the failure
 * path, which previously only wrote a log line and left the {@code egress:<id>} marker in place
 * so the download endpoint reported "still processing" forever.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class LiveClassRecordingMediaLibraryIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    private static final UUID INSTITUTION_ID = TestDataSeeder.INSTITUTION_ID;
    private static final UUID CLASS_ID = TestDataSeeder.CLASS_ID;

    private ResultActions postSignedWebhook(String body) throws Exception {
        return mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .header("Authorization", TestTokens.webhookAuthHeader(body))
                .content(body));
    }

    private void setRecordingUrl(String url) {
        jdbcTemplate.update("UPDATE live_classes SET recording_url = ? WHERE id = ?", url, CLASS_ID);
    }

    private String storedRecordingUrl() {
        return jdbcTemplate.queryForObject(
                "SELECT recording_url FROM live_classes WHERE id = ?", String.class, CLASS_ID);
    }

    @Test
    void egressEndedWithFile_PublishesReadyAssetLinkedToTheLiveClass() throws Exception {
        deleteAsset();
        setRecordingUrl(null);

        String body = """
                {"event":"egress_ended","egress":{"id":"egress-it-ready","roomName":"liveclass-%s",
                 "status":"EGRESS_COMPLETE",
                 "file_results":[{"location":"https://storage.example/rec-%s.mp4","duration":1200}]}}
                """.formatted(CLASS_ID, CLASS_ID);

        postSignedWebhook(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        var assets = mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse("LIVE_CLASS", CLASS_ID);
        assertEquals(1, assets.size(), "the recording must produce exactly one library asset");

        var asset = assets.get(0);
        assertEquals("RECORDING", asset.getMediaType());
        assertEquals("READY", asset.getStatus());
        assertEquals("INSTITUTION", asset.getVisibility());
        // Tenancy comes from the live class, so a recording cannot be filed under another tenant.
        assertEquals(INSTITUTION_ID, asset.getInstitutionId());
        assertEquals(CLASS_ID, asset.getSourceId());
        assertEquals("https://storage.example/rec-%s.mp4".formatted(CLASS_ID), asset.getFileUrl());
        // The class itself now resolves a playable URL rather than the transient egress marker.
        assertEquals("https://storage.example/rec-%s.mp4".formatted(CLASS_ID), storedRecordingUrl());
    }

    @Test
    void repeatedCompletionWebhook_RefreshesTheSameAssetInsteadOfDuplicating() throws Exception {
        deleteAsset();
        setRecordingUrl(null);

        for (int i = 1; i <= 2; i++) {
            String body = """
                    {"event":"egress_ended","egress":{"id":"egress-it-%d","roomName":"liveclass-%s",
                     "status":"EGRESS_COMPLETE",
                     "file_results":[{"location":"https://storage.example/rec-dup-%d.mp4","duration":600}]}}
                    """.formatted(i, CLASS_ID, i);
            postSignedWebhook(body).andExpect(status().isOk());
        }

        var assets = mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse("LIVE_CLASS", CLASS_ID);
        assertEquals(1, assets.size(), "idempotency: (source_type, source_id) must converge on one asset");
        assertEquals("https://storage.example/rec-dup-2.mp4", assets.get(0).getFileUrl());
    }

    @Test
    void egressEndedWithoutFile_RecordsFailedAssetAndClearsTheEgressMarker() throws Exception {
        deleteAsset();
        setRecordingUrl("egress:egress-it-failed");

        // Terminal success with no file_results: nothing playable was produced.
        String body = """
                {"event":"egress_ended","egress":{"id":"egress-it-failed","roomName":"liveclass-%s",
                 "status":"EGRESS_COMPLETE"}}
                """.formatted(CLASS_ID);

        postSignedWebhook(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        var assets = mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse("LIVE_CLASS", CLASS_ID);
        assertEquals(1, assets.size(), "a failed recording must be visible as a real asset");
        assertEquals("FAILED", assets.get(0).getStatus());

        // The egress marker is LiveKit bookkeeping, not a URL: leaving it behind made the
        // download endpoint report "still processing" indefinitely.
        assertNull(storedRecordingUrl(), "the egress:<id> marker must be cleared on failure");
    }

    @Test
    void terminalEgressFailure_MarksTheAssetFailed() throws Exception {
        deleteAsset();
        setRecordingUrl("egress:egress-it-hard-fail");

        String body = """
                {"event":"egress_ended","egress":{"id":"egress-it-hard-fail","roomName":"liveclass-%s",
                 "status":"EGRESS_FAILED"}}
                """.formatted(CLASS_ID);

        postSignedWebhook(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        var assets = mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse("LIVE_CLASS", CLASS_ID);
        assertTrue(assets.isEmpty() || "FAILED".equals(assets.get(0).getStatus()),
                "a terminal egress failure must never leave a READY asset behind");
    }

    @Test
    void publishedAsset_IsVisibleThroughTheMediaLibraryForItsTenant() throws Exception {
        deleteAsset();
        setRecordingUrl(null);

        String body = """
                {"event":"egress_ended","egress":{"id":"egress-it-lib","roomName":"liveclass-%s",
                 "status":"EGRESS_COMPLETE",
                 "file_results":[{"location":"https://storage.example/rec-lib.mp4","duration":300}]}}
                """.formatted(CLASS_ID);
        postSignedWebhook(body).andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id, media_type, status, visibility, institution_id, source_type, source_id "
                        + "FROM media_assets WHERE source_type = 'LIVE_CLASS' AND source_id = ?", CLASS_ID);

        assertNotNull(row.get("id"));
        assertEquals("RECORDING", row.get("media_type"));
        assertEquals("READY", row.get("status"));
        assertEquals(INSTITUTION_ID, row.get("institution_id"));
        assertEquals("LIVE_CLASS", row.get("source_type"));
    }

    private void deleteAsset() {
        jdbcTemplate.update(
                "DELETE FROM media_assets WHERE source_type = 'LIVE_CLASS' AND source_id = ?", CLASS_ID);
    }
}
