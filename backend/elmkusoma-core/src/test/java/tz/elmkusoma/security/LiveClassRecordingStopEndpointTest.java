package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stop-recording: prompt, idempotent, and explicit about what it did <em>not</em> do.
 *
 * <p>The endpoint used to block the request thread for up to 15 x 1.5s waiting for the object
 * upload to finish - the button spun for half a minute on a large recording, and a double click
 * answered 400 "No active recording found". It also returned 200 "Recording stopped" while the
 * {@code egress:<id>} marker was still in place, so the very next host-state read reported the
 * recording as active again.</p>
 *
 * <p>These cases pin the replacement contract: a stop request is accepted and answered at once,
 * repeating it is not an error, and a 200 means "stop accepted" rather than "the video is
 * published". With {@code livekit.egress.enabled=false} under the test profile, a marker is never
 * resolvable here, which is precisely the state these tests exercise.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class LiveClassRecordingStopEndpointTest {

    private static final String STORAGE_HOST = "backblazeb2.com";
    private static final String STORAGE_URL =
            "https://s3.test-region.backblazeb2.com/private-bucket/liveclass/final.mp4";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    /**
     * The library asset for the seeded class is what decides whether the recording reads as
     * AVAILABLE. {@code LiveClassRecordingMediaLibraryIT} is not transactional and deliberately
     * leaves a published asset behind, so it must be cleared here or these assertions would
     * depend on which test class ran first.
     */
    @BeforeEach
    void clearLibraryState() {
        jdbcTemplate.update("DELETE FROM media_assets WHERE source_type = 'LIVE_CLASS' AND source_id = ?",
                TestDataSeeder.CLASS_ID);
    }

    private void setRecordingUrl(String url) {
        jdbcTemplate.update("UPDATE live_classes SET recording_url = ? WHERE id = ?",
                url, TestDataSeeder.CLASS_ID);
    }

    private String stopUrl() {
        return "/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + "/recording/stop";
    }

    /** Nothing was ever recorded: repeating the request must succeed and say so. */
    @Test
    void stopWithNoRecording_isIdempotentAndReportsNone() throws Exception {
        setRecordingUrl(null);

        mockMvc.perform(post(stopUrl())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("NONE"))
                .andExpect(jsonPath("$.data.egressId").doesNotExist());
    }

    /** A resolved location is not a published recording: say PUBLISHING, not AVAILABLE. */
    @Test
    void stopOnAnAlreadyResolvedRecording_doesNotClaimItIsPublished() throws Exception {
        setRecordingUrl(STORAGE_URL);

        String body = mockMvc.perform(post(stopUrl())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PUBLISHING"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains(STORAGE_HOST),
                "the stop response leaked the private bucket location");
    }

    /**
     * With egress disabled the stop cannot be confirmed, so the honest answer is 500 - and it must
     * still never be mistaken for success, nor leak where the file would live. It must also leave
     * no "stop requested" flag behind: nothing was stopped, so the next host-state read may still
     * truthfully report a running capture.
     */
    @Test
    void stopWithAnUnconfirmableEgress_isAnErrorNotASilentSuccess() throws Exception {
        setRecordingUrl("egress:" + UUID.randomUUID());
        jdbcTemplate.update("UPDATE live_classes SET recording_stop_requested = FALSE WHERE id = ?",
                TestDataSeeder.CLASS_ID);

        mockMvc.perform(post(stopUrl())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.success").value(false));

        assertEquals(false, jdbcTemplate.queryForObject(
                        "SELECT recording_stop_requested FROM live_classes WHERE id = ?", Boolean.class,
                        TestDataSeeder.CLASS_ID),
                "a refused stop must not be recorded as an accepted one");
    }

    /** The authorization rule is unchanged by any of the lifecycle work. */
    @Test
    void stop_byANonOwnerTeacher_isStillForbidden() throws Exception {
        setRecordingUrl("egress:" + UUID.randomUUID());

        mockMvc.perform(post(stopUrl())
                        .header("Authorization", "Bearer " + TestTokens.studentToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
