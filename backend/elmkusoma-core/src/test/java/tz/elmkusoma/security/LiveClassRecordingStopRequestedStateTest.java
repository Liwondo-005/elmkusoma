package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.service.LiveKitService;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The persisted "stop requested" flag: what makes a stopped capture distinguishable from a
 * running one without asking LiveKit.
 *
 * <p>{@code live_classes.recording_url} holds the same {@code egress:<id>} marker while a job
 * runs and after an accepted stop whose file is still uploading. Host-state could only infer
 * the difference from the class status, so a teacher who pressed Stop and reloaded while the
 * class was still live was told {@code recordingActive: true}, shown the red REC indicator and
 * offered a Stop button for a job LiveKit had already finished (V161).</p>
 *
 * <p>LiveKit is mocked here because these cases are about <em>our</em> persisted state, not
 * about LiveKit: with {@code livekit.egress.enabled=false} under the test profile no stop can
 * ever be accepted, so the accepted path would otherwise be unreachable.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class LiveClassRecordingStopRequestedStateTest {

    private static final String PUBLISHED_URL =
            "https://s3.test-region.backblazeb2.com/private-bucket/liveclass/already-published.mp4";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private LiveClassRepository liveClassRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private LiveKitService liveKitService;

    @BeforeEach
    void resetRecordingState() {
        // A published library asset from a non-transactional suite would change the state
        // vocabulary; keep these assertions independent of run order.
        jdbcTemplate.update("DELETE FROM media_assets WHERE source_type = 'LIVE_CLASS' AND source_id = ?",
                TestDataSeeder.CLASS_ID);
        LiveClass liveClass = currentClass();
        liveClass.setRecordingUrl(null);
        liveClass.setRecordingStopRequested(false);
        liveClass.setStatus("IN_PROGRESS");
        liveClass.setEndedAt(null);
        liveClassRepository.save(liveClass);
    }

    private LiveClass currentClass() {
        return liveClassRepository.findById(TestDataSeeder.CLASS_ID).orElseThrow();
    }

    private void givenMarker(String egressId) {
        LiveClass liveClass = currentClass();
        liveClass.setRecordingUrl("egress:" + egressId);
        liveClass.setRecordingStopRequested(false);
        liveClassRepository.save(liveClass);
    }

    private boolean storedFlag() {
        entityManager.flush();
        return jdbcTemplate.queryForObject(
                "SELECT recording_stop_requested FROM live_classes WHERE id = ?", Boolean.class,
                TestDataSeeder.CLASS_ID);
    }

    private String url(String action) {
        return "/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + "/recording/" + action;
    }

    private String hostStateUrl() {
        return "/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + "/host-state";
    }

    /**
     * The whole point of the flag: an accepted stop is remembered, and the very next host-state
     * read on a still-running class no longer claims a capture is running.
     */
    @Test
    void anAcceptedStopIsPersistedAndHostStateStopsClaimingTheCapture() throws Exception {
        when(liveKitService.isAvailable()).thenReturn(true);
        when(liveKitService.isRecordingConfigured()).thenReturn(true);
        when(liveKitService.stopRecording("eg-accepted")).thenReturn(true);
        givenMarker("eg-accepted");

        mockMvc.perform(post(url("stop"))
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("FINALIZING"))
                .andExpect(jsonPath("$.data.egressId").value("eg-accepted"))
                .andExpect(jsonPath("$.data.recordingActive").value(false))
                .andExpect(jsonPath("$.data.stopRequested").value(true));

        assertEquals(true, storedFlag(), "the accepted stop was not persisted");
        assertEquals("egress:eg-accepted", jdbcTemplate.queryForObject(
                "SELECT recording_url FROM live_classes WHERE id = ?", String.class,
                TestDataSeeder.CLASS_ID),
                "the egress marker must survive an unresolved stop");

        // Host continuity: the same class, still IN_PROGRESS, read back as a fresh request.
        mockMvc.perform(get(hostStateUrl())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingActive").value(false))
                .andExpect(jsonPath("$.data.recordingState").value("FINALIZING"))
                .andExpect(jsonPath("$.data.recordingStopRequested").value(true));

        // A new capture cannot start while the previous file is still finalising, and the
        // refusal must not be mistaken for "already recording".
        mockMvc.perform(post(url("start"))
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    /** A stop that LiveKit refuses must leave no flag behind: nothing was actually stopped. */
    @Test
    void aRefusedStopDoesNotPersistTheFlag() throws Exception {
        when(liveKitService.stopRecording(anyString())).thenReturn(false);
        givenMarker("eg-refused");

        mockMvc.perform(post(url("stop"))
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.success").value(false));

        assertEquals(false, storedFlag(), "a failed stop must not read as an accepted one");
        mockMvc.perform(get(hostStateUrl())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingActive").value(true))
                .andExpect(jsonPath("$.data.recordingState").value("RECORDING"));
    }

    /**
     * A stale flag must never outlive its recording: starting a new capture resets it, or the
     * next capture would be reported as already stopping.
     */
    @Test
    void startingANewCaptureClearsAStaleStopFlag() throws Exception {
        when(liveKitService.isAvailable()).thenReturn(true);
        when(liveKitService.isRecordingConfigured()).thenReturn(true);
        when(liveKitService.startRecording(TestDataSeeder.CLASS_ID)).thenReturn("eg-new");
        LiveClass liveClass = currentClass();
        liveClass.setRecordingUrl(PUBLISHED_URL);
        liveClass.setRecordingStopRequested(true);
        liveClassRepository.save(liveClass);

        mockMvc.perform(post(url("start"))
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.egressId").value("eg-new"));

        assertEquals(false, storedFlag(), "the new capture inherited a stop meant for the old one");

        mockMvc.perform(get(hostStateUrl())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingActive").value(true))
                .andExpect(jsonPath("$.data.recordingState").value("RECORDING"));
    }

    /** Without a marker there is nothing to stop, so the flag must never be raised by it. */
    @Test
    void stopWithoutAMarkerNeverRaisesTheFlag() throws Exception {
        mockMvc.perform(post(url("stop"))
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("NONE"))
                .andExpect(jsonPath("$.data.stopRequested").value(false));

        assertEquals(false, storedFlag());
    }
}
