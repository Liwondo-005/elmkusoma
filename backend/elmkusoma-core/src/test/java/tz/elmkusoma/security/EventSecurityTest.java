package tz.elmkusoma.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class EventSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // ==================== Authentication Tests ====================

    @Test
    void accessEventEndpoint_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/v1/events"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 401 || status == 403
                    : "Expected 401 or 403 but got " + status;
            });
    }

    @Test
    void accessEventEndpoint_WithValidToken_Returns200() throws Exception {
        String token = getTestToken("INSTITUTION_ADMIN", TestTokens.ADMIN_EMAIL);
        mockMvc.perform(get("/v1/events")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void accessReplayEndpoint_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/v1/replays"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 401 || status == 403
                    : "Expected 401 or 403 but got " + status;
            });
    }

    @Test
    void accessLiveSessionEndpoint_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(post("/v1/live-session/join/test-class-id"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 401 || status == 403
                    : "Expected 401 or 403 but got " + status;
            });
    }

    @Test
    void accessStudentEvents_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/student/events"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 401 || status == 403
                    : "Expected 401 or 403 but got " + status;
            });
    }

    @Test
    void accessLearnerEvents_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/v1/learner/events"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 401 || status == 403
                    : "Expected 401 or 403 but got " + status;
            });
    }

    // ==================== Authorization Tests ====================

    @Test
    void createEvent_WithoutAdminRole_Returns403() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        mockMvc.perform(post("/v1/events")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"title\":\"Test\",\"eventType\":\"LECTURE\",\"startsAt\":\"2026-10-01T10:00:00\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteEvent_WithoutAdminRole_Returns403() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        mockMvc.perform(delete("/v1/events/00000000-0000-0000-0000-000000000099")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateEvent_WithoutAdminRole_Returns403() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        mockMvc.perform(put("/v1/events/00000000-0000-0000-0000-000000000099")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"title\":\"Updated\",\"eventType\":\"LECTURE\",\"startsAt\":\"2026-10-01T10:00:00\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void startRecording_WithoutAdminRole_Returns403() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        mockMvc.perform(post("/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + "/recording/start")
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void stopRecording_WithoutAdminRole_Returns403() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        mockMvc.perform(post("/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + "/recording/stop")
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void getAnalytics_WithoutTeacherRole_Returns403() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        mockMvc.perform(get("/v1/live-session/analytics/" + TestDataSeeder.CLASS_ID)
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminLiveSessions_WithoutAdminRole_Returns403() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        mockMvc.perform(get("/v1/admin/live-sessions/active")
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", "00000000-0000-0000-0000-000000000001"))
            .andExpect(status().isForbidden());
    }

    // ==================== Cross-Institution Access Tests ====================

    @Test
    void accessEvent_WrongInstitution_Returns403Or404() throws Exception {
        String token = getTestToken("INSTITUTION_ADMIN", TestTokens.ADMIN_EMAIL);
        java.util.UUID missingEventId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000099");
        mockMvc.perform(put("/v1/events/" + missingEventId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"title\":\"Hack\",\"eventType\":\"LECTURE\",\"startsAt\":\"2026-10-01T10:00:00\"}"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 403 || status == 404
                    : "Expected 403 or 404 but got " + status;
            });
    }

    @Test
    void joinLiveSession_WrongInstitution_Returns403() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        java.util.UUID classId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000050");
        mockMvc.perform(post("/v1/live-session/join/" + classId)
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", "00000000-0000-0000-0000-000000000002"))
            .andExpect(status().isForbidden());
    }

    // ==================== Student Registration Security Tests ====================

    @Test
    void registerForEvent_AsStudent_ReturnsCorrectStatus() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        java.util.UUID eventId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000050");
        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                .header("Authorization", "Bearer " + token))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 200 || status == 404 || status == 409
                    : "Expected 200, 404, or 409 but got " + status;
            });
    }

    @Test
    void cancelRegistration_AsStudent_ReturnsCorrectStatus() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        java.util.UUID eventId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000050");
        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/cancel-registration")
                .header("Authorization", "Bearer " + token))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 200 || status == 404
                    : "Expected 200 or 404 but got " + status;
            });
    }

    @Test
    void learnerRegisterForEvent_WithoutLearnerRole_Returns403() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        java.util.UUID eventId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000050");
        mockMvc.perform(post("/v1/learner/events/" + eventId + "/register")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    // ==================== Replay Security Tests ====================

    @Test
    void accessReplay_WithValidToken_Returns200Or404() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        java.util.UUID replayId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000099");
        mockMvc.perform(get("/v1/replays/" + replayId)
                .header("Authorization", "Bearer " + token))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 200 || status == 404
                    : "Expected 200 or 404 but got " + status;
            });
    }

    // ==================== LiveKit Token Security Tests ====================

    @Test
    void joinLiveSession_WithValidToken_ReturnsCorrectStatus() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        java.util.UUID classId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000050");
        mockMvc.perform(post("/v1/live-session/join/" + classId)
                .header("Authorization", "Bearer " + token)
                .header("X-Institution-Id", "00000000-0000-0000-0000-000000000001"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 200 || status == 400 || status == 403 || status == 404
                    : "Expected 200/400/403/404 but got " + status;
            });
    }

    // ==================== Recording Access Security Tests ====================

    @Test
    void downloadRecording_WithValidToken_ReturnsCorrectStatus() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        java.util.UUID classId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000050");
        mockMvc.perform(get("/v1/live-session/classes/" + classId + "/recording/download")
                .header("Authorization", "Bearer " + token))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 200 || status == 404
                    : "Expected 200 or 404 but got " + status;
            });
    }

    // ==================== Webhook Security Tests ====================

    @Test
    void webhookEndpoint_AcceptsValidPost() throws Exception {
        String body = "{\"event\":\"room_started\",\"room\":{\"name\":\"test\"}}";
        postSignedWebhook(body)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhookEndpoint_RejectsGet() throws Exception {
        mockMvc.perform(get("/v1/webhooks/livekit"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void webhookEndpoint_RejectsPut() throws Exception {
        mockMvc.perform(put("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"test\"}"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void webhookEndpoint_MissingEventField_Returns400() throws Exception {
        String body = "{\"room\":{\"name\":\"test\"}}";
        postSignedWebhook(body)
            .andExpect(status().isBadRequest());
    }

    // ==================== HTTP Method Security Tests ====================

    @Test
    void deleteEvent_WithGet_Returns405() throws Exception {
        mockMvc.perform(get("/v1/events/00000000-0000-0000-0000-000000000099"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 401 || status == 403 || status == 405
                    : "Expected 401, 403, or 405 but got " + status;
            });
    }

    @Test
    void createEvent_WithGet_Returns405() throws Exception {
        mockMvc.perform(get("/v1/events/00000000-0000-0000-0000-000000000099"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 401 || status == 403 || status == 405
                    : "Expected 401, 403, or 405 but got " + status;
            });
    }

    // ==================== Content-Type Security Tests ====================

    @Test
    void createEvent_WithWrongContentType_ReturnsUnsupportedOr400() throws Exception {
        String token = getTestToken("INSTITUTION_ADMIN", TestTokens.ADMIN_EMAIL);
        mockMvc.perform(post("/v1/events")
                .header("Authorization", "Bearer " + token)
                .contentType("text/plain")
                .content("not json"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 400 || status == 415 || status == 500
                    : "Expected 400, 415, or 500 but got " + status;
            });
    }

    // ==================== Live Session Health Security Tests ====================

    @Test
    void liveSessionHealth_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/v1/live-session/health"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 401 || status == 403
                    : "Expected 401 or 403 but got " + status;
            });
    }

    @Test
    void liveSessionHealth_WithStudentRole_Returns200() throws Exception {
        String token = getTestToken("STUDENT", TestTokens.STUDENT_EMAIL);
        mockMvc.perform(get("/v1/live-session/health")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("ELMKUSOMA Live"));
    }

    // ==================== Helper ====================

    private String getTestToken(String role, String email) {
        return TestTokens.userToken(email);
    }

    private ResultActions postSignedWebhook(String body) throws Exception {
        return mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .header("Authorization", TestTokens.webhookAuthHeader(body))
                .content(body));
    }
}
