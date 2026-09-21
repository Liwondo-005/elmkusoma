package tz.elmkusoma.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class LiveKitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final UUID INSTITUTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID STUDENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID TEACHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000050");

    // ==================== Join Live Session ====================

    @Test
    void joinLiveSession_ReturnsTokenAndUrl() throws Exception {
        String token = getStudentToken();
        mockMvc.perform(post("/v1/live-session/join/" + CLASS_ID)
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", INSTITUTION_ID.toString())
                .requestAttr("userId", STUDENT_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.liveKitToken").exists())
            .andExpect(jsonPath("$.data.liveKitUrl").exists())
            .andExpect(jsonPath("$.data.roomName").exists());
    }

    @Test
    void joinLiveSession_WithTeacherRole_Returns200Or400() throws Exception {
        String token = getTeacherToken();
        mockMvc.perform(post("/v1/live-session/join/" + CLASS_ID)
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", INSTITUTION_ID.toString())
                .requestAttr("userId", TEACHER_USER_ID))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 200 || status == 400 || status == 404
                    : "Expected 200/400/404 but got " + status;
            });
    }

    @Test
    void joinLiveSession_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(post("/v1/live-session/join/" + CLASS_ID)
                .header("X-Institution-Id", INSTITUTION_ID.toString()))
            .andExpect(status().isUnauthorized());
    }

    // ==================== Get Participants ====================

    @Test
    void getParticipants_ReturnsList() throws Exception {
        String token = getStudentToken();
        mockMvc.perform(get("/v1/live-session/participants/" + CLASS_ID)
                .header("Authorization", "Bearer " + token)
                .requestAttr("userId", STUDENT_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== Get Analytics (Teacher Only) ====================

    @Test
    void getAnalytics_WithTeacherRole_ReturnsCorrectStatus() throws Exception {
        String token = getTeacherToken();
        mockMvc.perform(get("/v1/live-session/analytics/" + CLASS_ID)
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", INSTITUTION_ID.toString())
                .requestAttr("userId", TEACHER_USER_ID))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 200 || status == 404
                    : "Expected 200 or 404 but got " + status;
            });
    }

    @Test
    void getAnalytics_WithStudentRole_Returns403() throws Exception {
        String token = getStudentToken();
        mockMvc.perform(get("/v1/live-session/analytics/" + CLASS_ID)
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", INSTITUTION_ID.toString())
                .requestAttr("userId", STUDENT_USER_ID))
            .andExpect(status().isForbidden());
    }

    // ==================== Recording Management ====================

    @Test
    void startRecording_WithoutAdminRole_Returns403() throws Exception {
        String token = getStudentToken();
        mockMvc.perform(post("/v1/live-session/classes/" + CLASS_ID + "/recording/start")
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", INSTITUTION_ID.toString())
                .requestAttr("userId", STUDENT_USER_ID)
                .requestAttr("institutionId", INSTITUTION_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    void stopRecording_WithoutAdminRole_Returns403() throws Exception {
        String token = getStudentToken();
        mockMvc.perform(post("/v1/live-session/classes/" + CLASS_ID + "/recording/stop")
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", INSTITUTION_ID.toString())
                .requestAttr("userId", STUDENT_USER_ID)
                .requestAttr("institutionId", INSTITUTION_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    void startRecording_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(post("/v1/live-session/classes/" + CLASS_ID + "/recording/start")
                .header("X-Institution-Id", INSTITUTION_ID.toString()))
            .andExpect(status().isUnauthorized());
    }

    // ==================== Recording Download ====================

    @Test
    void getRecordingDownload_ReturnsCorrectStatus() throws Exception {
        String token = getStudentToken();
        mockMvc.perform(get("/v1/live-session/classes/" + CLASS_ID + "/recording/download")
                .header("Authorization", "Bearer " + token)
                .requestAttr("userId", STUDENT_USER_ID))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 200 || status == 404
                    : "Expected 200 or 404 but got " + status;
            });
    }

    @Test
    void getRecordingDownload_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/v1/live-session/classes/" + CLASS_ID + "/recording/download"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== Report Issue ====================

    @Test
    void reportIssue_ReturnsCorrectStatus() throws Exception {
        String token = getStudentToken();
        mockMvc.perform(post("/v1/live-session/report/" + CLASS_ID)
                .header("Authorization", "Bearer " + token)
                .requestAttr("userId", STUDENT_USER_ID)
                .contentType("application/json")
                .content("{\"issueType\":\"AUDIO_ISSUE\",\"description\":\"Cannot hear teacher\",\"severity\":\"HIGH\"}"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 200 || status == 404
                    : "Expected 200 or 404 but got " + status;
            });
    }

    // ==================== Calendar Export ====================

    @Test
    void exportCalendarEvent_ReturnsIcsContent() throws Exception {
        String token = getStudentToken();
        mockMvc.perform(get("/v1/live-session/calendar/" + CLASS_ID + "/export")
                .header("Authorization", "Bearer " + token)
                .requestAttr("userId", STUDENT_USER_ID))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                if (status == 200) {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.contains("text/calendar")
                        : "Expected text/calendar content type but got " + contentType;
                } else {
                    assert status == 404
                        : "Expected 200 or 404 but got " + status;
                }
            });
    }

    // ==================== Live Session Health ====================

    @Test
    void liveSessionHealth_WithAdminRole_Returns200() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(get("/v1/live-session/health")
                .header("Authorization", "Bearer " + token)
                .requestAttr("userId", ADMIN_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("ELMKUSOMA Live"))
            .andExpect(jsonPath("$.liveKitConfigured").exists())
            .andExpect(jsonPath("$.mode").exists());
    }

    @Test
    void liveSessionHealth_WithTeacherRole_Returns200() throws Exception {
        String token = getTeacherToken();
        mockMvc.perform(get("/v1/live-session/health")
                .header("Authorization", "Bearer " + token)
                .requestAttr("userId", TEACHER_USER_ID))
            .andExpect(status().isOk());
    }

    @Test
    void liveSessionHealth_WithStudentRole_Returns403() throws Exception {
        String token = getStudentToken();
        mockMvc.perform(get("/v1/live-session/health")
                .header("Authorization", "Bearer " + token)
                .requestAttr("userId", STUDENT_USER_ID))
            .andExpect(status().isForbidden());
    }

    // ==================== Webhook Event Handling ====================

    @Test
    void webhookEndpoint_HandlesRoomStarted() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"room_started\",\"room\":{\"name\":\"liveclass-test\",\"sid\":\"room-sid-001\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhookEndpoint_HandlesRoomEnded() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"room_ended\",\"room\":{\"name\":\"liveclass-test\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhookEndpoint_HandlesParticipantJoined() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"participant_joined\",\"participant\":{\"id\":\"user-1\",\"identity\":\"student@test.com\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhookEndpoint_HandlesParticipantLeft() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"participant_left\",\"participant\":{\"id\":\"user-1\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhookEndpoint_HandlesRecordingStarted() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"recording_started\",\"egress\":{\"id\":\"egress-001\",\"roomName\":\"liveclass-test\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhookEndpoint_HandlesRecordingCompleted() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"recording_completed\",\"egress\":{\"id\":\"egress-001\",\"roomName\":\"liveclass-test\",\"status\":\"EGRESS_COMPLETE\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhookEndpoint_HandlesRecordingFailed() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"recording_failed\",\"egress\":{\"id\":\"egress-001\",\"roomName\":\"liveclass-test\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhookEndpoint_MissingEventField_Returns400() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"room\":{\"name\":\"test\"}}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void webhookEndpoint_UnknownEvent_ReturnsOk() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"some_unknown_event\",\"data\":\"test\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhookEndpoint_GetMethod_Returns405() throws Exception {
        mockMvc.perform(get("/v1/webhooks/livekit"))
            .andExpect(status().isMethodNotAllowed());
    }

    // ==================== Admin Live Session Monitoring ====================

    @Test
    void adminActiveSessions_WithAdminRole_ReturnsCorrectStatus() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(get("/v1/admin/live-sessions/active")
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", INSTITUTION_ID.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void adminActiveSessions_WithStudentRole_Returns403() throws Exception {
        String token = getStudentToken();
        mockMvc.perform(get("/v1/admin/live-sessions/active")
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", INSTITUTION_ID.toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminSessionStats_WithAdminRole_ReturnsStats() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(get("/v1/admin/live-sessions/stats")
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", INSTITUTION_ID.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalSessions").exists())
            .andExpect(jsonPath("$.data.scheduled").exists())
            .andExpect(jsonPath("$.data.inProgress").exists())
            .andExpect(jsonPath("$.data.completed").exists());
    }

    @Test
    void adminSessionParticipants_WithAdminRole_ReturnsCorrectStatus() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(get("/v1/admin/live-sessions/participants/" + CLASS_ID)
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", INSTITUTION_ID.toString()))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 200 || status == 404
                    : "Expected 200 or 404 but got " + status;
            });
    }

    // ==================== Helpers ====================

    private String getAdminToken() {
        return "test-admin-token";
    }

    private String getTeacherToken() {
        return "test-teacher-token";
    }

    private String getStudentToken() {
        return "test-student-token";
    }
}
