package tz.elmkusoma.integration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tz.elmkusoma.testutil.TestTokens;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class EventLifecycleE2ETest {

    @Autowired
    private MockMvc mockMvc;

    private static final UUID INSTITUTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** static: JUnit creates a new test instance per method, so instance fields do not carry across @Order phases. */
    private static String createdEventId;

    // ==================== Phase 1: Provider Creates Event ====================

    @Test
    @Order(1)
    void providerCreatesEvent() throws Exception {
        String adminToken = getAdminToken();
        MvcResult result = mockMvc.perform(post("/v1/events")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content("{\"title\":\"E2E Test Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"2026-12-01T10:00:00\",\"description\":\"E2E lifecycle test event\",\"maxParticipants\":50}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("E2E Test Event"))
            .andExpect(jsonPath("$.data.eventType").value("LECTURE"))
            .andReturn();

        createdEventId = extractIdFromResponse(result);
    }

    // ==================== Phase 2: Admin Views Event List ====================

    @Test
    @Order(2)
    void adminViewsEventList() throws Exception {
        String adminToken = getAdminToken();
        mockMvc.perform(get("/v1/events")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== Phase 3: Admin Views Event Details ====================

    @Test
    @Order(3)
    void adminViewsEventDetails() throws Exception {
        String adminToken = getAdminToken();
        mockMvc.perform(get("/v1/events/" + createdEventId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("E2E Test Event"));
    }

    // ==================== Phase 4: Admin Updates Event, then Publishes ====================

    @Test
    @Order(4)
    void adminUpdatesEvent() throws Exception {
        String adminToken = getAdminToken();
        mockMvc.perform(put("/v1/events/" + createdEventId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content("{\"title\":\"E2E Test Event Updated\",\"eventType\":\"LECTURE\",\"startsAt\":\"2026-12-01T11:00:00\",\"description\":\"Updated description\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("E2E Test Event Updated"));

        mockMvc.perform(post("/v1/events/" + createdEventId + "/publish")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    // ==================== Phase 5: Student Discovers Upcoming Events ====================

    @Test
    @Order(5)
    void studentDiscoversUpcomingEvents() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(get("/api/v1/student/events")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== Phase 6: Student Views Event Details ====================

    @Test
    @Order(6)
    void studentViewsEventDetails() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(get("/api/v1/student/events/" + createdEventId)
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 7: Student Registers for Event ====================

    @Test
    @Order(7)
    void studentRegistersForEvent() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(post("/api/v1/student/events/" + createdEventId + "/register")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 8: Duplicate Registration is Idempotent ====================

    @Test
    @Order(8)
    void duplicateRegistrationIsIdempotent() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(post("/api/v1/student/events/" + createdEventId + "/register")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk());
    }

    // ==================== Phase 9: Student Views Registered Events ====================

    @Test
    @Order(9)
    void studentViewsRegisteredEvents() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(get("/api/v1/student/events/registered")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== Phase 10: Admin Views Event Registrations ====================

    @Test
    @Order(10)
    void adminViewsEventRegistrations() throws Exception {
        String adminToken = getAdminToken();
        mockMvc.perform(get("/v1/events/" + createdEventId + "/registrations")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== Phase 11: Learner Discovers Published Events ====================

    @Test
    @Order(11)
    void learnerDiscoversPublishedEvents() throws Exception {
        String learnerToken = getLearnerToken();
        mockMvc.perform(get("/v1/learner/events")
                .header("Authorization", "Bearer " + learnerToken)
                .param("eventType", "LECTURE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 12: Learner Registers for Event ====================

    @Test
    @Order(12)
    void learnerRegistersForEvent() throws Exception {
        String learnerToken = getLearnerToken();
        mockMvc.perform(post("/v1/learner/events/" + createdEventId + "/register")
                .header("Authorization", "Bearer " + learnerToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 13: Event Materials Accessible ====================

    @Test
    @Order(13)
    void eventMaterialsAccessible() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(get("/api/v1/student/events/" + createdEventId + "/materials")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== Phase 14: Student Cancels Registration ====================

    @Test
    @Order(14)
    void studentCancelsRegistration() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(post("/api/v1/student/events/" + createdEventId + "/cancel-registration")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 15: Learner Cancels Registration ====================

    @Test
    @Order(15)
    void learnerCancelsRegistration() throws Exception {
        String learnerToken = getLearnerToken();
        mockMvc.perform(post("/v1/learner/events/" + createdEventId + "/cancel")
                .header("Authorization", "Bearer " + learnerToken)
                .contentType("application/json")
                .content("{\"reason\":\"Changed mind\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 16: Admin Deletes Event ====================

    @Test
    @Order(16)
    void adminDeletesEvent() throws Exception {
        String adminToken = getAdminToken();
        mockMvc.perform(delete("/v1/events/" + createdEventId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 17: Deleted Event Returns 404 ====================

    @Test
    @Order(17)
    void deletedEventReturns404() throws Exception {
        String adminToken = getAdminToken();
        mockMvc.perform(get("/v1/events/" + createdEventId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    // ==================== Phase 18: Cross-Student Cannot Access Other Student Registration ====================

    @Test
    @Order(18)
    void crossStudentCannotSeeOtherRegistrationDetails() throws Exception {
        String otherStudentToken = getOtherStudentToken();
        mockMvc.perform(get("/v1/events/" + createdEventId + "/registrations")
                .header("Authorization", "Bearer " + otherStudentToken))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 403 || status == 404
                    : "Expected 403 or 404 but got " + status;
            });
    }

    // ==================== Phase 19: Webhook Integration ====================

    @Test
    @Order(19)
    void liveKitWebhookHandlesRoomStarted() throws Exception {
        String body = "{\"event\":\"room_started\",\"room\":{\"name\":\"liveclass-e2e-test\",\"sid\":\"test-sid\"}}";
        postSignedWebhook(body)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @Order(20)
    void liveKitWebhookHandlesRecordingCompleted() throws Exception {
        String body = "{\"event\":\"recording_completed\",\"egress\":{\"id\":\"test-egress-001\",\"roomName\":\"liveclass-e2e-test\",\"status\":\"EGRESS_COMPLETE\"}}";
        postSignedWebhook(body)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @Order(21)
    void liveKitWebhookHandlesParticipantJoined() throws Exception {
        String body = "{\"event\":\"participant_joined\",\"participant\":{\"id\":\"user-participant-001\",\"identity\":\"student@test.com\"}}";
        postSignedWebhook(body)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    // ==================== Phase 20: Replay Access ====================

    @Test
    @Order(22)
    void replayEndpointAccessibleWithAuth() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(get("/v1/replays")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Helpers ====================

    private String getAdminToken() {
        return TestTokens.adminToken();
    }

    private String getStudentToken() {
        return TestTokens.studentToken();
    }

    private String getLearnerToken() {
        return TestTokens.learnerToken();
    }

    private String getOtherStudentToken() {
        return TestTokens.otherStudentToken();
    }

    private ResultActions postSignedWebhook(String body) throws Exception {
        return mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .header("Authorization", TestTokens.webhookAuthHeader(body))
                .content(body));
    }

    private String extractIdFromResponse(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"").matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        throw new AssertionError("No event id in response: " + content);
    }
}
