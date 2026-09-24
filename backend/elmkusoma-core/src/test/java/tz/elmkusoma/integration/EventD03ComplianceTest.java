package tz.elmkusoma.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D03 rubric compliance matrix (§41/§60/§98 object-level access, §53 discovery,
 * §59 meeting URL, §69 capacity, §70 exposure, §81/§82 pagination,
 * §18/§19 lifecycle notifications, §94/§95 audit + dependency handling,
 * §97 provider scope, webhook idempotency, migration presence via FlywayMigrationValidationTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class EventD03ComplianceTest {

    private static final UUID INST_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID EVENT_B = UUID.fromString("00000000-0000-0000-0000-000000000060");
    private static final UUID REPLAY_B = UUID.fromString("00000000-0000-0000-0000-000000000061");
    private static final UUID CLASS_B = UUID.fromString("00000000-0000-0000-0000-000000000062");
    private static final UUID PROVIDER_B_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000070");
    private static final String PROVIDER_B_EMAIL = "providerb@elmkusoma.tz";

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LearnerNotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ReplayRepository replayRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void seedCrossInstitutionFixtures() {
        if (count("SELECT COUNT(*) FROM institutions WHERE id = ?", INST_B) == 0) {
            jdbcTemplate.update(
                    "INSERT INTO institutions (id, name, code, type, country, is_active, status, is_deleted, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    INST_B, "Institution B", "TEST002", "SECONDARY", "Tanzania",
                    true, "ACTIVE", false, LocalDateTime.now());
        }
        if (count("SELECT COUNT(*) FROM events WHERE id = ?", EVENT_B) == 0) {
            jdbcTemplate.update(
                    "INSERT INTO events (id, institution_id, created_at, is_deleted, organizer_id, title, "
                            + "event_type, starts_at, status, event_status, is_free, requires_approval, timezone) "
                            + "VALUES (?, ?, ?, false, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    EVENT_B, INST_B, LocalDateTime.now(),
                    TestDataSeeder.ADMIN_USER_ID, "Cross Institution Event", "LECTURE",
                    LocalDateTime.now().plusDays(3), "PUBLISHED", "PUBLISHED",
                    true, false, "Africa/Dar_es_Salaam");
        }
        if (count("SELECT COUNT(*) FROM replays WHERE id = ?", REPLAY_B) == 0) {
            jdbcTemplate.update(
                    "INSERT INTO replays (id, institution_id, created_at, is_deleted, event_id, title, "
                            + "status, view_count, last_position_seconds) "
                            + "VALUES (?, ?, ?, false, ?, ?, ?, 0, 0)",
                    REPLAY_B, INST_B, LocalDateTime.now(), EVENT_B, "Cross Institution Replay", "AVAILABLE");
        }
        if (count("SELECT COUNT(*) FROM live_classes WHERE id = ?", CLASS_B) == 0) {
            jdbcTemplate.update(
                    "INSERT INTO live_classes (id, institution_id, created_at, is_deleted, teacher_id, title, "
                            + "scheduled_at, duration_minutes, status, max_participants, recording_enabled, "
                            + "session_type, timezone, is_recurring, lobby_enabled, recording_url) "
                            + "VALUES (?, ?, ?, false, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    CLASS_B, INST_B, LocalDateTime.now(), TestDataSeeder.TEACHER_USER_ID,
                    "Cross Institution Class", LocalDateTime.now().plusHours(2), 60,
                    "IN_PROGRESS", 50, true, "LECTURE", "Africa/Dar_es_Salaam", false, false,
                    "https://cdn.example.com/rec-b.mp4");
        }
        jdbcTemplate.update(
                "UPDATE live_classes SET recording_url = ? WHERE id = ?",
                "https://cdn.example.com/rec-a.mp4", TestDataSeeder.CLASS_ID);
        if (count("SELECT COUNT(*) FROM users WHERE id = ?", PROVIDER_B_USER_ID) == 0) {
            jdbcTemplate.update(
                    "INSERT INTO users (id, institution_id, created_at, is_deleted, email, password_hash, "
                            + "first_name, last_name, role, is_active, is_email_verified) "
                            + "VALUES (?, ?, ?, false, ?, ?, ?, ?, ?, ?, ?)",
                    PROVIDER_B_USER_ID, TestDataSeeder.INSTITUTION_ID, LocalDateTime.now(),
                    PROVIDER_B_EMAIL, "unused-hash", "Prov", "B", "PROVIDER_STAFF", true, true);
        }
    }

    // ==================== §41/§60/§98 Recording download access ====================

    @Test
    void recordingDownload_SameInstitution_WithRecording_Returns200() throws Exception {
        mockMvc.perform(get("/v1/live-session/classes/" + TestDataSeeder.CLASS_ID + "/recording/download")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.downloadUrl").value("https://cdn.example.com/rec-a.mp4"));
    }

    @Test
    void recordingDownload_WrongInstitution_Returns403() throws Exception {
        mockMvc.perform(get("/v1/live-session/classes/" + CLASS_B + "/recording/download")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden());
    }

    // ==================== §41/§60/§98 Cross-institution object-level denial ====================

    @Test
    void getEvent_CrossInstitution_Admin_Returns404() throws Exception {
        mockMvc.perform(get("/v1/events/" + EVENT_B)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEvent_CrossInstitution_Student_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/student/events/" + EVENT_B)
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEvent_CrossInstitution_Returns403() throws Exception {
        mockMvc.perform(put("/v1/events/" + EVENT_B)
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content("{\"title\":\"Hijack\",\"eventType\":\"LECTURE\",\"startsAt\":\"2026-12-01T10:00:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteEvent_CrossInstitution_Returns403() throws Exception {
        mockMvc.perform(delete("/v1/events/" + EVENT_B)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReplay_CrossInstitution_Returns404() throws Exception {
        mockMvc.perform(get("/v1/replays/" + REPLAY_B)
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void progressUpdate_CrossInstitution_Replay_Returns404() throws Exception {
        mockMvc.perform(put("/v1/replays/" + REPLAY_B + "/progress")
                        .header("Authorization", "Bearer " + TestTokens.studentToken())
                        .contentType("application/json")
                        .content("{\"positionSeconds\":42}"))
                .andExpect(status().isNotFound());
    }

    // ==================== §69 Capacity / blocked registration ====================

    @Test
    void registerWhenFull_Returns409() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Full Capacity Event\",\"eventType\":\"LECTURE\","
                        + "\"startsAt\":\"" + future() + "\",\"status\":\"REGISTRATION_OPEN\",\"maxParticipants\":1}");

        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.otherStudentToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("full")));
    }

    @Test
    void registerOnCancelledEvent_Returns409() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Soon Cancelled\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future() + "\"}");
        mockMvc.perform(post("/v1/events/" + eventId + "/cancel")
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content("{\"reason\":\"no longer needed\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("cancelled")));
    }

    // ==================== §96 Reschedule transitions ====================

    @Test
    void reschedule_InvalidTransition_Returns409() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Invalid Transition\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future() + "\"}");
        mockMvc.perform(post("/v1/events/" + eventId + "/cancel")
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content("{\"title\":\"Invalid Transition\",\"eventType\":\"LECTURE\","
                                + "\"startsAt\":\"" + future() + "\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Invalid event status transition")));
    }

    @Test
    void reschedule_ValidStartChange_WorksAndNotifies() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Reschedule Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\"}");
        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk());

        String newStart = futureLater();
        mockMvc.perform(put("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content("{\"title\":\"Reschedule Event\",\"eventType\":\"LECTURE\","
                                + "\"startsAt\":\"" + newStart + "\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startsAt").value(newStart));

        assertTrue(notificationRepository.existsByUserIdAndTargetIdAndNotificationTypeAndIsDeletedFalse(
                TestDataSeeder.STUDENT_USER_ID, UUID.fromString(eventId), "EVENT_RESCHEDULED"));
    }

    // ==================== §18/§19 Webhooks: duplicate + out-of-order ====================

    @Test
    void webhook_RecordingCompleted_DuplicateDelivery_IsIdempotent() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Webhook Idempotent\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\"}");
        String egressId = "egress-" + UUID.randomUUID();
        String body = recordingCompletedBody(eventId, egressId);

        postSignedWebhook(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
        postSignedWebhook(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        long available = replayRepository.findByEventIdAndIsDeletedFalse(UUID.fromString(eventId))
                .stream()
                .filter(r -> Replay.STATUS_AVAILABLE.equals(r.getStatus()))
                .count();
        assertTrue(available <= 1, "duplicate recording_completed must not create multiple AVAILABLE replays");
    }

    @Test
    void webhook_RecordingCompletedBeforeStarted_StillOk() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Webhook Out Of Order\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\"}");
        String egressId = "egress-" + UUID.randomUUID();

        postSignedWebhook(recordingCompletedBody(eventId, egressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
        postSignedWebhook(recordingStartedBody(eventId, egressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhook_RecordingAvailable_NotifiesRegistrant() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Recording Notify\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\"}");
        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk());

        postSignedWebhook(recordingCompletedBody(eventId, "egress-" + UUID.randomUUID()))
                .andExpect(status().isOk());

        boolean notified = notificationRepository
                .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(TestDataSeeder.STUDENT_USER_ID)
                .stream()
                .anyMatch(n -> "EVENT_RECORDING".equals(n.getNotificationType()));
        assertTrue(notified, "registrant should receive EVENT_RECORDING when replay becomes available");
    }

    // ==================== §18/§19 Registration confirmation notification ====================

    @Test
    void registration_ConfirmationNotificationCreated() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Notify On Register\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\"}");
        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk());

        assertTrue(notificationRepository.existsByUserIdAndTargetIdAndNotificationTypeAndIsDeletedFalse(
                TestDataSeeder.STUDENT_USER_ID, UUID.fromString(eventId), "EVENT_REGISTRATION"));
    }

    // ==================== §46 related content + §59 meeting URL + §70 exposure ====================

    @Test
    void relatedCourseId_RoundTrip() throws Exception {
        UUID courseId = UUID.randomUUID();
        String eventId = createEvent(
                "{\"title\":\"Linked Course\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"relatedCourseId\":\"" + courseId + "\"}");

        mockMvc.perform(get("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relatedCourseId").value(courseId.toString()));
    }

    @Test
    void meetingUrl_HiddenFromUnregisteredLearner() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Meet Link\",\"eventType\":\"WORKSHOP\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\",\"meetingUrl\":\"https://meet.example.com/room-1\"}");

        mockMvc.perform(get("/api/v1/student/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meetingUrl").doesNotExist());

        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meetingUrl").value("https://meet.example.com/room-1"));
    }

    @Test
    void eventResponse_ExposesProviderAccessCancellation() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Exposure Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"providerId\":\"prov-d03-1\",\"accessLevel\":\"INSTITUTION\"}");
        mockMvc.perform(post("/v1/events/" + eventId + "/cancel")
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content("{\"reason\":\"weather\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerId").value("prov-d03-1"))
                .andExpect(jsonPath("$.data.accessLevel").value("INSTITUTION"))
                .andExpect(jsonPath("$.data.cancellationReason").value("weather"))
                .andExpect(jsonPath("$.data.cancelledAt").exists());
    }

    // ==================== §81/§82 Pagination + §97 provider filter ====================

    @Test
    void eventsList_WithPagination_SetsTotalCountHeader() throws Exception {
        mockMvc.perform(get("/v1/events?page=0&size=1")
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void eventsList_WithoutPagination_KeepsLegacyFullList() throws Exception {
        mockMvc.perform(get("/v1/events")
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void providerFilter_ReturnsOnlyProviderEvents() throws Exception {
        createEvent("{\"title\":\"Prov A\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                + "\",\"providerId\":\"prov-d03-a\"}");
        createEvent("{\"title\":\"Prov B\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                + "\",\"providerId\":\"prov-d03-b\"}");

        mockMvc.perform(get("/v1/events?providerId=prov-d03-a")
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].providerId", everyItem(is("prov-d03-a"))));
    }

    // ==================== §53 Personal relevance ====================

    @Test
    void personalizedDiscovery_Student_ReturnsRankedEvents() throws Exception {
        mockMvc.perform(get("/api/v1/student/events?personalized=true")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void personalizedDiscovery_Learner_ReturnsRankedEvents() throws Exception {
        mockMvc.perform(get("/v1/learner/events?personalized=true")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== §94 Audit trail ====================

    @Test
    void createEvent_WritesAuditLog() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Audited Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future() + "\"}");

        var logs = auditLogRepository.findByEntityTypeAndEntityId(
                TestDataSeeder.INSTITUTION_ID, "EVENT", UUID.fromString(eventId));
        assertFalse(logs.isEmpty(), "creating an event must write an audit_logs row (§94)");
        assertTrue(logs.stream().anyMatch(l -> l.getAction() == AuditLog.AuditAction.CREATE));
    }

    // ==================== §95 Dependency-aware delete ====================

    @Test
    void deleteLiveEvent_WithoutForce_Returns409_WithForce_CascadesReplay() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Live Delete\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"LIVE\"}");
        UUID eventUuid = UUID.fromString(eventId);

        replayRepository.save(Replay.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .eventId(eventUuid)
                .title("Replay of Live Delete")
                .status(Replay.STATUS_AVAILABLE)
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        mockMvc.perform(delete("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/v1/events/" + eventId + "?force=true")
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk());

        assertTrue(eventRepository.findById(eventUuid)
                        .map(e -> Boolean.TRUE.equals(e.getIsDeleted()))
                        .orElse(false),
                "force delete must soft-delete the event");
        assertTrue(replayRepository.findByEventIdAndIsDeletedFalse(eventUuid).isEmpty(),
                "force delete must cascade soft-delete to dependent replays (§95)");
    }

    // ==================== §90/§99/§104 Event join (token) + §89/§91 failure chains ====================

    @Test
    void joinEvent_WithoutToken_Returns401Or403() throws Exception {
        mockMvc.perform(post("/v1/learner/events/" + UUID.randomUUID() + "/join"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403,
                            "Expected 401 or 403 but got " + status);
                });
    }

    @Test
    void joinEvent_CrossInstitution_Returns403Or404() throws Exception {
        mockMvc.perform(post("/v1/learner/events/" + EVENT_B + "/join")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 403 || status == 404,
                            "Expected 403 or 404 but got " + status);
                });
    }

    @Test
    void joinEvent_UnknownEvent_Returns404() throws Exception {
        mockMvc.perform(post("/v1/learner/events/" + UUID.randomUUID() + "/join")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinEvent_UnregisteredNonPublic_Returns403() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Join Needs Registration\",\"eventType\":\"LECTURE\","
                        + "\"startsAt\":\"" + future() + "\",\"status\":\"PUBLISHED\","
                        + "\"accessLevel\":\"INSTITUTION\"}");
        startLive(eventId);

        mockMvc.perform(post("/v1/learner/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("Registration required")));
    }

    @Test
    void joinEvent_RegisteredLive_Returns200WithToken() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Join Live Event\",\"eventType\":\"LECTURE\","
                        + "\"startsAt\":\"" + future() + "\",\"status\":\"PUBLISHED\","
                        + "\"accessLevel\":\"INSTITUTION\"}");
        mockMvc.perform(post("/v1/learner/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isCreated());
        startLive(eventId);

        mockMvc.perform(post("/v1/learner/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventStatus").value("LIVE"))
                .andExpect(jsonPath("$.data.roomName").value("event-" + eventId))
                .andExpect(jsonPath("$.data.liveKitAvailable").value(true))
                .andExpect(jsonPath("$.data.waitingRoom").value(false))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void joinEvent_RegisteredStudentLive_Returns200WithToken() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Student Join Live\",\"eventType\":\"LECTURE\","
                        + "\"startsAt\":\"" + future() + "\",\"status\":\"PUBLISHED\"}");
        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk());
        startLive(eventId);

        mockMvc.perform(post("/v1/learner/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventStatus").value("LIVE"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void joinEvent_Preparing_ReturnsWaitingRoomToken() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Preparing Join\",\"eventType\":\"LECTURE\","
                        + "\"startsAt\":\"" + future() + "\",\"status\":\"REGISTRATION_OPEN\"}");
        mockMvc.perform(post("/v1/learner/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isCreated());
        // REGISTRATION_OPEN -> PREPARING is a legal transition
        mockMvc.perform(put("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content("{\"title\":\"Preparing Join\",\"eventType\":\"LECTURE\","
                                + "\"startsAt\":\"" + future() + "\",\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/learner/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventStatus").value("PREPARING"))
                .andExpect(jsonPath("$.data.waitingRoom").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void joinEvent_NotStarted_Returns409() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Not Started Join\",\"eventType\":\"LECTURE\","
                        + "\"startsAt\":\"" + future() + "\",\"status\":\"PUBLISHED\"}");
        mockMvc.perform(post("/v1/learner/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/learner/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void joinEvent_Cancelled_Returns409_BlocksJoin() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Cancel Then Join\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\"}");
        mockMvc.perform(post("/v1/learner/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/events/" + eventId + "/cancel")
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content("{\"reason\":\"cancelled for join test\"}"))
                .andExpect(status().isOk());

        // §89/§91: cancelled event -> notification already sent; join/register now denied
        assertTrue(notificationRepository.existsByUserIdAndTargetIdAndNotificationTypeAndIsDeletedFalse(
                TestDataSeeder.LEARNER_USER_ID, UUID.fromString(eventId), "EVENT_CANCELLED"));

        mockMvc.perform(post("/v1/learner/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("cancelled")));

        mockMvc.perform(post("/v1/learner/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void joinEvent_TokenExpiresWithin15Minutes_ShortLived() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Short Lived Token\",\"eventType\":\"LECTURE\","
                        + "\"startsAt\":\"" + future() + "\",\"status\":\"PUBLISHED\"}");
        startLive(eventId);

        // organizer/admin staff path: admin is staff so no registration required
        mockMvc.perform(post("/v1/learner/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        // Decode exp claim from the LiveKit JWT (HS256, payload is second segment)
        MvcResult result = mockMvc.perform(post("/v1/learner/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Matcher tokenMatcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        assertTrue(tokenMatcher.find(), "token missing from join response");
        String[] parts = tokenMatcher.group(1).split("\\.");
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]),
                java.nio.charset.StandardCharsets.UTF_8);
        Matcher expMatcher = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)").matcher(payloadJson);
        assertTrue(expMatcher.find(), "exp claim missing");
        long expSeconds = Long.parseLong(expMatcher.group(1));
        long ttlSeconds = expSeconds - System.currentTimeMillis() / 1000;
        assertTrue(ttlSeconds > 0 && ttlSeconds <= 15 * 60,
                "LiveKit token must be short-lived (<=15min), got " + ttlSeconds + "s");
    }

    @Test
    void webhook_RecordingCompleted_DelayedRetry_IsIdempotent() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Delayed Webhook\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\"}");
        String egressId = "egress-" + UUID.randomUUID();
        String body = recordingCompletedBody(eventId, egressId);

        postSignedWebhook(body).andExpect(status().isOk());
        // simulate delayed redelivery of the same event
        Thread.sleep(50);
        postSignedWebhook(body).andExpect(status().isOk());

        long available = replayRepository.findByEventIdAndIsDeletedFalse(UUID.fromString(eventId))
                .stream()
                .filter(r -> Replay.STATUS_AVAILABLE.equals(r.getStatus()))
                .count();
        assertTrue(available <= 1, "delayed redelivery must not create multiple AVAILABLE replays");
    }

    @Test
    void webhook_RecordingFailed_ProducesNoFalseAvailableReplay() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Failed Recording\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\"}");
        String egressId = "egress-" + UUID.randomUUID();
        String body = "{\"event\":\"recording_failed\","
                + "\"room\":{\"name\":\"event-" + eventId + "\",\"sid\":\"sid-" + egressId + "\"},"
                + "\"egress\":{\"id\":\"" + egressId + "\",\"roomName\":\"event-" + eventId + "\","
                + "\"error\":\"storage unavailable\"}}";

        postSignedWebhook(body)
                .andExpect(status().isOk());

        long available = replayRepository.findByEventIdAndIsDeletedFalse(UUID.fromString(eventId))
                .stream()
                .filter(r -> Replay.STATUS_AVAILABLE.equals(r.getStatus()))
                .count();
        assertTrue(available == 0, "failed recording must not create an AVAILABLE replay");

        // §66: learner-visible EventResponse carries recordingStatus=FAILED for FE display
        mockMvc.perform(get("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingStatus").value("FAILED"));

        // §66: FAILED replay must remain excluded from the learner AVAILABLE-only list
        mockMvc.perform(get("/v1/learner/replays")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(result -> assertFalse(
                        result.getResponse().getContentAsString().contains("Failed Recording"),
                        "FAILED recording must not appear in the learner replay list"));
    }

    // ==================== §12/§70 Learner exposure of trust fields ====================

    @Test
    void learnerEventAndList_KeepAccessLevelProviderCancellationFields() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Learner Trust Fields\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\",\"providerId\":\"prov-d03-trust\","
                        + "\"accessLevel\":\"INSTITUTION\",\"meetingUrl\":\"https://meet.example.com/trust\"}");

        mockMvc.perform(get("/v1/learner/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessLevel").value("INSTITUTION"))
                .andExpect(jsonPath("$.data.providerId").value("prov-d03-trust"))
                .andExpect(jsonPath("$.data.meetingUrl").doesNotExist());

        // list sanitization must strip only meetingUrl — trust fields survive
        mockMvc.perform(get("/v1/learner/events")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.title=='Learner Trust Fields')].providerId", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.title=='Learner Trust Fields')].accessLevel", hasSize(1)));

        mockMvc.perform(post("/v1/learner/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/events/" + eventId + "/cancel")
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content("{\"reason\":\"trust check\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/learner/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessLevel").value("INSTITUTION"))
                .andExpect(jsonPath("$.data.providerId").value("prov-d03-trust"))
                .andExpect(jsonPath("$.data.cancellationReason").value("trust check"))
                .andExpect(jsonPath("$.data.cancelledAt").exists());
    }

    // ==================== §69 almostFull capacity hint ====================

    @Test
    void almostFull_HintWithin10Percent_ClearedWhenMarkedFull() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Almost Full Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"REGISTRATION_OPEN\",\"maxParticipants\":10}");
        for (int i = 0; i < 9; i++) {
            jdbcTemplate.update(
                    "INSERT INTO event_registrations (id, institution_id, created_at, is_deleted, "
                            + "event_id, user_id, status, registered_at, attended) "
                            + "VALUES (?, ?, ?, false, ?, ?, 'REGISTERED', ?, false)",
                    UUID.randomUUID(), TestDataSeeder.INSTITUTION_ID, LocalDateTime.now(),
                    UUID.fromString(eventId), UUID.randomUUID(), LocalDateTime.now());
        }

        mockMvc.perform(get("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registeredCount").value(9))
                .andExpect(jsonPath("$.data.availableSpots").value(1))
                .andExpect(jsonPath("$.data.almostFull").value(true));

        // 10th seat triggers the mark-FULL path: almostFull must flip off
        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventStatus").value("FULL"))
                .andExpect(jsonPath("$.data.almostFull").value(false));
    }

    // ==================== §74 related lesson/module deep link data ====================

    @Test
    void relatedModuleAndLessonIds_RoundTrip() throws Exception {
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        String eventId = createEvent(
                "{\"title\":\"Deep Linked Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"relatedModuleId\":\"" + moduleId
                        + "\",\"relatedLessonId\":\"" + lessonId + "\"}");

        mockMvc.perform(get("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relatedModuleId").value(moduleId.toString()))
                .andExpect(jsonPath("$.data.relatedLessonId").value(lessonId.toString()));
    }

    // ==================== §49 materials stable ids for D04 linking ====================

    @Test
    void learnerMaterials_ExposeStableIdAndFileUrl_ForD04Linking() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Materials Link Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future() + "\"}");
        MvcResult created = mockMvc.perform(post("/v1/events/" + eventId + "/materials")
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content("{\"eventId\":\"" + eventId + "\",\"title\":\"Slide Deck\","
                                + "\"materialType\":\"PRESENTATION\","
                                + "\"fileUrl\":\"https://cdn.example.com/deck.pdf\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String materialId = extractId(created);

        mockMvc.perform(get("/v1/learner/events/" + eventId + "/materials")
                        .header("Authorization", "Bearer " + TestTokens.learnerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + materialId + "')].fileUrl", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.id=='" + materialId + "')].fileUrl",
                        hasItems("https://cdn.example.com/deck.pdf")));
    }

    // ==================== §96 timezone exposure ====================

    @Test
    void timezone_DefaultedOnCreateAndReturnedOnResponse() throws Exception {
        String defaultTzId = createEvent(
                "{\"title\":\"Default TZ Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future() + "\"}");
        mockMvc.perform(get("/v1/events/" + defaultTzId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("Africa/Dar_es_Salaam"))
                .andExpect(jsonPath("$.data.startsAt").isString());

        String customTzId = createEvent(
                "{\"title\":\"Custom TZ Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"timezone\":\"Africa/Nairobi\"}");
        mockMvc.perform(get("/v1/events/" + customTzId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("Africa/Nairobi"));
    }

    // ==================== §98 wrong-provider update + cross-user registration denial ====================

    @Test
    void updateEvent_WrongProvider_Returns403() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Provider Owned Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"providerId\":\"prov-d03-owner\"}");

        mockMvc.perform(put("/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.userToken(PROVIDER_B_EMAIL))
                        .contentType("application/json")
                        .content("{\"title\":\"Hijack By Other Provider\",\"eventType\":\"LECTURE\","
                                + "\"startsAt\":\"" + future() + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("provider")));
    }

    @Test
    void registrations_CrossUser_ManipulatedUserId_Returns403() throws Exception {
        mockMvc.perform(get("/v1/events/registrations/user/" + TestDataSeeder.LEARNER_USER_ID)
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("own registrations")));
    }

    // ==================== §89 Integration contract shapes ====================

    @Test
    void contract_InstitutionEventsList_HasTitleAndStartsAt() throws Exception {
        createEvent("{\"title\":\"D03 Contract Shape\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                + "\",\"status\":\"PUBLISHED\"}");

        mockMvc.perform(get("/v1/events/institution/" + TestDataSeeder.INSTITUTION_ID)
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.title=='D03 Contract Shape')].startsAt", hasSize(1)));
    }

    @Test
    void contract_ReplaysByEvent_ReturnsList() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Replay Contract Event\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future() + "\"}");
        replayRepository.save(Replay.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .eventId(UUID.fromString(eventId))
                .title("Replay Contract Item")
                .status(Replay.STATUS_AVAILABLE)
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        mockMvc.perform(get("/v1/replays/event/" + eventId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.title=='Replay Contract Item')]", hasSize(1)));
    }

    @Test
    void contract_UserRegistrations_OwnRegistrationsListed() throws Exception {
        String eventId = createEvent(
                "{\"title\":\"Own Registrations Contract\",\"eventType\":\"LECTURE\",\"startsAt\":\"" + future()
                        + "\",\"status\":\"PUBLISHED\"}");
        mockMvc.perform(post("/api/v1/student/events/" + eventId + "/register")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/events/registrations/user/" + TestDataSeeder.STUDENT_USER_ID)
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.eventId=='" + eventId + "')]", hasSize(1)));
    }

    // ==================== Helpers ====================

    private void startLive(String eventId) throws Exception {
        mockMvc.perform(post("/v1/events/" + eventId + "/start-live")
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk());
    }

    private int count(String sql, Object arg) {
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class, arg);
        return n == null ? 0 : n;
    }

    private String future() {
        return LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0)
                .toString();
    }

    private String futureLater() {
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .format(LocalDateTime.now().plusDays(14).withHour(14).withMinute(30)
                        .withSecond(0).withNano(0));
    }

    private String createEvent(String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/events")
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private org.springframework.test.web.servlet.ResultActions postSignedWebhook(String body) throws Exception {
        return mockMvc.perform(post("/v1/webhooks/livekit")
                .contentType("application/json")
                .header("Authorization", TestTokens.webhookAuthHeader(body))
                .content(body));
    }

    private String recordingCompletedBody(String eventId, String egressId) {
        return "{\"event\":\"recording_completed\","
                + "\"room\":{\"name\":\"event-" + eventId + "\",\"sid\":\"sid-" + egressId + "\"},"
                + "\"egress\":{\"id\":\"" + egressId + "\",\"roomName\":\"event-" + eventId + "\","
                + "\"file_results\":[{\"location\":\"https://cdn.example.com/" + egressId
                + ".mp4\",\"duration\":120}]}}";
    }

    private String recordingStartedBody(String eventId, String egressId) {
        return "{\"event\":\"recording_started\","
                + "\"room\":{\"name\":\"event-" + eventId + "\",\"sid\":\"sid-" + egressId + "\"},"
                + "\"egress\":{\"id\":\"" + egressId + "\",\"roomName\":\"event-" + eventId + "\"}}";
    }

    private String extractId(MvcResult result) throws Exception {
        Matcher m = ID_PATTERN.matcher(result.getResponse().getContentAsString());
        if (m.find()) {
            return m.group(1);
        }
        throw new AssertionError("No id in response: " + result.getResponse().getContentAsString());
    }
}
