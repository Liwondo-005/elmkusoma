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

import java.util.UUID;

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
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID STUDENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    private String createdEventId;
    private String createdLiveClassId;

    // ==================== Phase 1: Provider Creates Event ====================

    @Test
    @Order(1)
    void providerCreatesEvent() throws Exception {
        String adminToken = getAdminToken();
        MvcResult result = mockMvc.perform(post("/v1/events")
                .header("Authorization", "Bearer " + adminToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", ADMIN_USER_ID)
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
                .header("Authorization", "Bearer " + adminToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", ADMIN_USER_ID))
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
                .header("Authorization", "Bearer " + adminToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", ADMIN_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("E2E Test Event"));
    }

    // ==================== Phase 4: Admin Updates Event ====================

    @Test
    @Order(4)
    void adminUpdatesEvent() throws Exception {
        String adminToken = getAdminToken();
        mockMvc.perform(put("/v1/events/" + createdEventId)
                .header("Authorization", "Bearer " + adminToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", ADMIN_USER_ID)
                .contentType("application/json")
                .content("{\"title\":\"E2E Test Event Updated\",\"eventType\":\"LECTURE\",\"startsAt\":\"2026-12-01T11:00:00\",\"description\":\"Updated description\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("E2E Test Event Updated"));
    }

    // ==================== Phase 5: Student Discovers Upcoming Events ====================

    @Test
    @Order(5)
    void studentDiscoversUpcomingEvents() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(get("/api/v1/student/events")
                .header("Authorization", "Bearer " + studentToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", STUDENT_USER_ID))
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
                .header("Authorization", "Bearer " + studentToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", STUDENT_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 7: Student Registers for Event ====================

    @Test
    @Order(7)
    void studentRegistersForEvent() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(post("/api/v1/student/events/" + createdEventId + "/register")
                .header("Authorization", "Bearer " + studentToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", STUDENT_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 8: Duplicate Registration is Idempotent ====================

    @Test
    @Order(8)
    void duplicateRegistrationIsIdempotent() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(post("/api/v1/student/events/" + createdEventId + "/register")
                .header("Authorization", "Bearer " + studentToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", STUDENT_USER_ID))
            .andExpect(status().isOk());
    }

    // ==================== Phase 9: Student Views Registered Events ====================

    @Test
    @Order(9)
    void studentViewsRegisteredEvents() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(get("/api/v1/student/events/registered")
                .header("Authorization", "Bearer " + studentToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", STUDENT_USER_ID))
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
                .header("Authorization", "Bearer " + adminToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", ADMIN_USER_ID))
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
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", UUID.fromString("00000000-0000-0000-0000-000000000030"))
                .param("eventType", "LECTURE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 12: Learner Registers for Event ====================

    @Test
    @Order(12)
    void learnerRegistersForEvent() throws Exception {
        String learnerToken = getLearnerToken();
        UUID learnerUserId = UUID.fromString("00000000-0000-0000-0000-000000000030");
        mockMvc.perform(post("/v1/learner/events/" + createdEventId + "/register")
                .header("Authorization", "Bearer " + learnerToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", learnerUserId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 13: Event Materials Accessible ====================

    @Test
    @Order(13)
    void eventMaterialsAccessible() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(get("/api/v1/student/events/" + createdEventId + "/materials")
                .header("Authorization", "Bearer " + studentToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", STUDENT_USER_ID))
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
                .header("Authorization", "Bearer " + studentToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", STUDENT_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 15: Learner Cancels Registration ====================

    @Test
    @Order(15)
    void learnerCancelsRegistration() throws Exception {
        String learnerToken = getLearnerToken();
        UUID learnerUserId = UUID.fromString("00000000-0000-0000-0000-000000000030");
        mockMvc.perform(post("/v1/learner/events/" + createdEventId + "/cancel")
                .header("Authorization", "Bearer " + learnerToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", learnerUserId)
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
                .header("Authorization", "Bearer " + adminToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", ADMIN_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Phase 17: Deleted Event Returns 404 ====================

    @Test
    @Order(17)
    void deletedEventReturns404() throws Exception {
        String adminToken = getAdminToken();
        mockMvc.perform(get("/v1/events/" + createdEventId)
                .header("Authorization", "Bearer " + adminToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", ADMIN_USER_ID))
            .andExpect(status().isNotFound());
    }

    // ==================== Phase 18: Cross-Student Cannot Access Other Student Registration ====================

    @Test
    @Order(18)
    void crossStudentCannotSeeOtherRegistrationDetails() throws Exception {
        String otherStudentToken = getOtherStudentToken();
        UUID otherUserId = UUID.fromString("00000000-0000-0000-0000-000000000040");
        mockMvc.perform(get("/v1/events/" + createdEventId + "/registrations")
                .header("Authorization", "Bearer " + otherStudentToken)
                .requestAttr("institutionId", INSTITUTION_ID)
                .requestAttr("userId", otherUserId))
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
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"room_started\",\"room\":{\"name\":\"liveclass-e2e-test\",\"sid\":\"test-sid\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @Order(20)
    void liveKitWebhookHandlesRecordingCompleted() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"recording_completed\",\"egress\":{\"id\":\"test-egress-001\",\"roomName\":\"liveclass-e2e-test\",\"status\":\"EGRESS_COMPLETE\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @Order(21)
    void liveKitWebhookHandlesParticipantJoined() throws Exception {
        mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .content("{\"event\":\"participant_joined\",\"participant\":{\"id\":\"user-participant-001\",\"identity\":\"student@test.com\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    // ==================== Phase 20: Replay Access ====================

    @Test
    @Order(20)
    void replayEndpointAccessibleWithAuth() throws Exception {
        String studentToken = getStudentToken();
        mockMvc.perform(get("/v1/replays")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== Helpers ====================

    private String getAdminToken() {
        return "test-admin-token";
    }

    private String getStudentToken() {
        return "test-student-token";
    }

    private String getLearnerToken() {
        return "test-learner-token";
    }

    private String getOtherStudentToken() {
        return "test-other-student-token";
    }

    private String extractIdFromResponse(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        int dataIndex = content.indexOf("\"id\":\"");
        if (dataIndex == -1) {
            dataIndex = content.indexOf("\"id\": \"");
        }
        if (dataIndex != -1) {
            int start = content.indexOf("\"", dataIndex + 5) + 1;
            int end = content.indexOf("\"", start);
            return content.substring(start, end);
        }
        return "fallback-id";
    }
}
