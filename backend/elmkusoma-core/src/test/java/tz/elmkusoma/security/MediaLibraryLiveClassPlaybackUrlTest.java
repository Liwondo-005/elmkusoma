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
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Media Library "Open" for a live class recording must go through the application, not the bucket.
 *
 * <p>Every media endpoint returned {@code media_assets.file_url} verbatim, and both Media Library
 * pages did {@code window.open(item.fileUrl)}. For a {@code LIVE_CLASS} recording that value is a
 * private Backblaze B2 object URL, so the browser fetched the bucket directly and B2 answered with
 * an {@code UnauthorizedAccess} XML document. The upload itself had succeeded; only the browser's
 * direct access was refused, because the bucket is (correctly) private.</p>
 *
 * <p>These tests pin the contract that replaces it: a {@code LIVE_CLASS} asset never serialises its
 * storage location at all, and instead carries {@code playbackUrl} - the ordinary authorised
 * {@code /v1/live-session/classes/{id}/recording/playback} route, keyed by the server-side
 * {@code source_id}. Ordinary teacher-uploaded media keeps its existing {@code fileUrl} behaviour.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class MediaLibraryLiveClassPlaybackUrlTest {

    private static final String STORAGE_URL =
            "https://s3.test-region.backblazeb2.com/private-bucket/liveclass/record.mp4";
    /** Marker that only the private bucket's host would ever contain. */
    private static final String STORAGE_HOST = "backblazeb2.com";
    private static final String PLAYBACK_SUFFIX = "/recording/playback";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private InstitutionRepository institutionRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private InstitutionMembershipRepository membershipRepository;
    @Autowired private LiveClassParticipantRepository participantRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Institution institution;
    private User learner;
    private UUID liveClassId;

    @BeforeEach
    void setUp() {
        String run = UUID.randomUUID().toString().substring(0, 8);
        institution = institutionRepository.save(Institution.builder()
                .name("PlaybackUrl " + run)
                .code("PU-" + run)
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());

        learner = userRepository.save(User.builder()
                .email("playback-learner-" + run + "@test.com")
                .passwordHash("test-hash")
                .firstName("Playback")
                .lastName("Learner")
                .role(User.Role.STUDENT)
                .institutionId(institution.getId())
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(learner.getId())
                .institutionId(institution.getId())
                .role(InstitutionMembership.Role.STUDENT)
                .isActive(true)
                .build());

        liveClassId = UUID.randomUUID();
        // Entitlement for a RECORDING asset is participation in the recorded session, so the
        // learner must actually have attended - membership alone deliberately is not enough.
        participantRepository.saveAndFlush(LiveClassParticipant.builder()
                .liveClassId(liveClassId)
                .userId(learner.getId())
                .role("LEARNER")
                .joinedAt(java.time.LocalDateTime.now())
                .isDeleted(false)
                .build());
    }

    private String learnerToken() {
        return TestTokens.userToken(learner.getEmail());
    }

    private MediaAsset saveLiveClassRecording(String title) {
        MediaAsset asset = MediaAsset.builder()
                .title(title)
                .description("Live class recording - " + title)
                .mediaType("RECORDING")
                .fileUrl(STORAGE_URL)
                .status("READY")
                .visibility("INSTITUTION")
                .sourceType("LIVE_CLASS")
                .sourceId(liveClassId)
                .build();
        asset.setInstitutionId(institution.getId());
        return mediaAssetRepository.saveAndFlush(asset);
    }

    /** Ordinary teacher-uploaded media: no live-class origin, direct URL is the whole point. */
    private MediaAsset saveTeacherUpload(String title) {
        MediaAsset asset = MediaAsset.builder()
                .title(title)
                .mediaType("DOCUMENT")
                .fileUrl("https://cdn.example/notes.pdf")
                .status("READY")
                .visibility("INSTITUTION")
                .build();
        asset.setInstitutionId(institution.getId());
        return mediaAssetRepository.saveAndFlush(asset);
    }

    private String playbackRouteFor(UUID classId) {
        return "/v1/live-session/classes/" + classId + PLAYBACK_SUFFIX;
    }

    // ------------------------------------------------------------------
    // LIVE_CLASS: the storage location must never leave the server
    // ------------------------------------------------------------------

    @Test
    void liveClassRecording_detailExposesTheApplicationRouteNotTheStorageUrl() throws Exception {
        MediaAsset recording = saveLiveClassRecording("Open Me");

        String body = mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + learnerToken())
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileUrl").doesNotExist())
                .andExpect(jsonPath("$.data.playbackUrl", endsWith(playbackRouteFor(liveClassId))))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains(STORAGE_HOST),
                "the Media Library detail response leaked the private bucket URL");
    }

    @Test
    void liveClassRecording_listExposesTheApplicationRouteNotTheStorageUrl() throws Exception {
        MediaAsset recording = saveLiveClassRecording("Listed Recording");

        String body = mockMvc.perform(get("/v1/media")
                        .header("Authorization", "Bearer " + learnerToken())
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(body.contains(recording.getId().toString()),
                "the entitled learner must still see their recording in the list");
        assertTrue(body.contains(playbackRouteFor(liveClassId)),
                "the Media Library list must carry the authorised playback route");
        assertFalse(body.contains(STORAGE_HOST),
                "the Media Library list leaked the private bucket URL");
    }

    @Test
    void liveClassRecording_downloadUrlIsTheApplicationRouteNotTheStorageUrl() throws Exception {
        MediaAsset recording = saveLiveClassRecording("Downloadable Recording");

        String body = mockMvc.perform(get("/v1/media/" + recording.getId() + "/download-url")
                        .header("Authorization", "Bearer " + learnerToken())
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl", endsWith(playbackRouteFor(liveClassId))))
                .andExpect(jsonPath("$.data.playbackUrl").value(true))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains(STORAGE_HOST),
                "the download endpoint leaked the private bucket URL");
    }

    /** The playback path is derived from the persisted origin, never from anything the client sent. */
    @Test
    void liveClassRecording_playbackRouteIsKeyedByThePersistedSourceId() throws Exception {
        MediaAsset recording = saveLiveClassRecording("Keyed Recording");

        String body = mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + learnerToken())
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(body.contains("/" + liveClassId + PLAYBACK_SUFFIX),
                "playback must be keyed by the server-side source_id, not by client input");
    }

    // ------------------------------------------------------------------
    // Non-LIVE_CLASS: behaviour must be untouched
    // ------------------------------------------------------------------

    @Test
    void teacherUploadedMedia_keepsItsDirectFileUrl() throws Exception {
        MediaAsset upload = saveTeacherUpload("Teacher Notes");

        mockMvc.perform(get("/v1/media/" + upload.getId())
                        .header("Authorization", "Bearer " + learnerToken())
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileUrl").value("https://cdn.example/notes.pdf"))
                .andExpect(jsonPath("$.data.playbackUrl").doesNotExist());

        mockMvc.perform(get("/v1/media/" + upload.getId() + "/download-url")
                        .header("Authorization", "Bearer " + learnerToken())
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").value("https://cdn.example/notes.pdf"))
                .andExpect(jsonPath("$.data.playbackUrl").value(false));
    }

    // ------------------------------------------------------------------
    // host-state: same rule, plus an accurate lifecycle
    // ------------------------------------------------------------------

    @Test
    void hostState_neverReturnsTheStorageLocation() throws Exception {
        jdbcTemplate.update("UPDATE live_classes SET recording_url = ?, status = 'COMPLETED', "
                        + "ended_at = now() WHERE id = ?", STORAGE_URL, TestDataSeeder.CLASS_ID);

        String body = mockMvc.perform(get("/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + "/host-state")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingUrl",
                        endsWith("/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + PLAYBACK_SUFFIX)))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains(STORAGE_HOST),
                "host-state handed the teacher a private bucket URL for a LIVE_CLASS recording");
    }

    /**
     * A marker left on a class that has already ended means the stop was asked for and the upload
     * is still finalising - not that a capture is running. Reported as {@code RECORDING} it kept
     * the REC indicator on and offered a Stop button for a job LiveKit had already finished.
     */
    @Test
    void hostState_reportsFinalizingForAnEndedClassStillHoldingTheMarker() throws Exception {
        jdbcTemplate.update("UPDATE live_classes SET recording_url = 'egress:eg-finalizing', "
                        + "status = 'COMPLETED', ended_at = now() WHERE id = ?", TestDataSeeder.CLASS_ID);

        mockMvc.perform(get("/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + "/host-state")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingState").value("FINALIZING"))
                .andExpect(jsonPath("$.data.recordingActive").value(false));
    }

    /** The same marker on a class that is still running is a live capture, and must read as one. */
    @Test
    void hostState_reportsRecordingWhileTheClassIsStillLive() throws Exception {
        jdbcTemplate.update("UPDATE live_classes SET recording_url = 'egress:eg-live', "
                        + "status = 'IN_PROGRESS', ended_at = null WHERE id = ?", TestDataSeeder.CLASS_ID);

        mockMvc.perform(get("/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + "/host-state")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingState").value("RECORDING"))
                .andExpect(jsonPath("$.data.recordingActive").value(true));
    }

    /**
     * The same marker on a still-running class whose stop was already accepted: the capture is
     * over, only the file is outstanding. Without the persisted flag this could not be told from
     * the case above, and a reload after a mid-class stop handed the teacher a live REC
     * indicator and a Stop button for a finished job.
     */
    @Test
    void hostState_reportsFinalizingForAStopRequestedWhileTheClassIsStillLive() throws Exception {
        jdbcTemplate.update("UPDATE live_classes SET recording_url = 'egress:eg-stop-accepted', "
                        + "recording_stop_requested = TRUE, status = 'IN_PROGRESS', ended_at = null "
                        + "WHERE id = ?", TestDataSeeder.CLASS_ID);

        mockMvc.perform(get("/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + "/host-state")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingState").value("FINALIZING"))
                .andExpect(jsonPath("$.data.recordingActive").value(false))
                .andExpect(jsonPath("$.data.recordingStopRequested").value(true));
    }
}
