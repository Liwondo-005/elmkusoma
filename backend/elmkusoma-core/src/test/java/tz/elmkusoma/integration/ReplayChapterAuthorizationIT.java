package tz.elmkusoma.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Replay chapter markers: who may write them, and who may read them.
 *
 * <p>Chapters describe a recording's contents, so writing them is restricted to the people
 * who can be held responsible for it - the class's own teacher or institution staff - and
 * reading them follows the same replay entitlement that already guards the video itself.
 * These tests pin both halves, including the tenant boundary, because a chapter API that
 * any authenticated teacher could write would let staff annotate someone else's recording.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class ReplayChapterAuthorizationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID institutionId;
    private UUID otherInstitutionId;

    private UUID classAId;
    private UUID replayAId;
    private String teacherAEmail;

    private String teacherBEmail;

    private UUID learnerUserId;
    private String learnerEmail;

    @BeforeEach
    void setUp() {
        String run = UUID.randomUUID().toString().substring(0, 8);
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();

        insertInstitution(institutionId, "Chapters " + run, "CH-" + run);
        insertInstitution(otherInstitutionId, "ChaptersX " + run, "CX-" + run);

        teacherAEmail = "chapter-teacher-a-" + run + "@test.com";
        UUID teacherAUserId = insertUser(teacherAEmail, institutionId, "TEACHER");
        UUID teacherATeacherId = insertTeacher(teacherAUserId, institutionId);
        classAId = insertLiveClass(teacherATeacherId, institutionId, "Chapter class A " + run);
        replayAId = insertReplay(classAId, institutionId, 600);

        teacherBEmail = "chapter-teacher-b-" + run + "@test.com";
        UUID teacherBUserId = insertUser(teacherBEmail, institutionId, "TEACHER");
        UUID teacherBTeacherId = insertTeacher(teacherBUserId, institutionId);
        UUID classBId = insertLiveClass(teacherBTeacherId, institutionId, "Chapter class B " + run);
        insertReplay(classBId, institutionId, 600);

        learnerEmail = "chapter-learner-" + run + "@test.com";
        learnerUserId = insertUser(learnerEmail, institutionId, "STUDENT");
    }

    @AfterEach
    void tearDown() {
        // Children first; the schema has no ON DELETE CASCADE on these.
        jdbcTemplate.update("DELETE FROM replay_chapters WHERE institution_id = ? OR institution_id = ?",
                institutionId, otherInstitutionId);
        jdbcTemplate.update("DELETE FROM replays WHERE institution_id = ? OR institution_id = ?",
                institutionId, otherInstitutionId);
        jdbcTemplate.update("DELETE FROM live_class_participants WHERE live_class_id IN "
                + "(SELECT id FROM live_classes WHERE institution_id = ? OR institution_id = ?)",
                institutionId, otherInstitutionId);
        jdbcTemplate.update("DELETE FROM live_classes WHERE institution_id = ? OR institution_id = ?",
                institutionId, otherInstitutionId);
        jdbcTemplate.update("DELETE FROM teachers WHERE institution_id = ? OR institution_id = ?",
                institutionId, otherInstitutionId);
        jdbcTemplate.update("DELETE FROM institution_memberships WHERE institution_id = ? OR institution_id = ?",
                institutionId, otherInstitutionId);
        jdbcTemplate.update("DELETE FROM users WHERE institution_id = ? OR institution_id = ?",
                institutionId, otherInstitutionId);
        jdbcTemplate.update("DELETE FROM institutions WHERE id = ? OR id = ?",
                institutionId, otherInstitutionId);
    }

    @Test
    void owningTeacher_canAddAChapterToTheirOwnRecording() throws Exception {
        mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", "Bearer " + TestTokens.userToken(teacherAEmail))
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"Worked example\",\"positionSeconds\":120}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Worked example"))
                .andExpect(jsonPath("$.data.positionSeconds").value(120));
    }

    /** Another teacher in the same institution owns a different class - not this recording. */
    @Test
    void anotherTeacher_cannotChapterSomeoneElsesRecording() throws Exception {
        mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", "Bearer " + TestTokens.userToken(teacherBEmail))
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"Not mine\",\"positionSeconds\":30}"))
                .andExpect(status().isNotFound());

        assertEquals(0, chapterCount(), "a rejected write must not persist anything");
    }

    @Test
    void foreignInstitutionTeacher_cannotReachTheRecordingAtAll() throws Exception {
        mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", "Bearer " + TestTokens.userToken(teacherBEmail))
                        .header("X-Institution-Id", otherInstitutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"Cross tenant\",\"positionSeconds\":10}"))
                .andExpect(status().isForbidden());

        assertEquals(0, chapterCount());
    }

    @Test
    void learner_cannotWriteChapters() throws Exception {
        mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", "Bearer " + TestTokens.userToken(learnerEmail))
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"Learner edit\",\"positionSeconds\":10}"))
                .andExpect(status().isForbidden());

        assertEquals(0, chapterCount());
    }

    @Test
    void chapter_needsATitleAndAPositionInsideTheRecording() throws Exception {
        String teacher = "Bearer " + TestTokens.userToken(teacherAEmail);

        mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", teacher)
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType("application/json")
                        .content("{\"positionSeconds\":10}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", teacher)
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"No position\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", teacher)
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"Past the end\",\"positionSeconds\":99999}"))
                .andExpect(status().isBadRequest());

        assertEquals(0, chapterCount(), "none of the invalid markers may be stored");
    }

    @Test
    void twoChaptersCannotShareATimestamp() throws Exception {
        String teacher = "Bearer " + TestTokens.userToken(teacherAEmail);
        String header = "X-Institution-Id";

        mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", teacher)
                        .header(header, institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"First\",\"positionSeconds\":60}"))
                .andExpect(status().isCreated());

        // Indistinguishable when clicked, so V151 makes (replay, position) unique.
        mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", teacher)
                        .header(header, institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"Second\",\"positionSeconds\":60}"))
                .andExpect(status().isBadRequest());

        assertEquals(1, chapterCount());
    }

    @Test
    void deletingAFreesTheTimestampForALegitimateReplacement() throws Exception {
        String teacher = "Bearer " + TestTokens.userToken(teacherAEmail);
        String header = "X-Institution-Id";

        String created = mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", teacher)
                        .header(header, institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"Draft\",\"positionSeconds\":90}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID chapterId = UUID.fromString(created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

        mockMvc.perform(delete("/v1/replays/" + replayAId + "/chapters/" + chapterId)
                        .header("Authorization", teacher)
                        .header(header, institutionId.toString()))
                .andExpect(status().isOk());
        assertEquals(0, chapterCount(), "a deleted marker must not stay listed");

        mockMvc.perform(post("/v1/replays/" + replayAId + "/chapters")
                        .header("Authorization", teacher)
                        .header(header, institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"Replacement\",\"positionSeconds\":90}"))
                .andExpect(status().isCreated());
        assertEquals(1, chapterCount(), "soft delete must free the timestamp again");
    }

    /** Chapters ride along with the replay the learner is already entitled to watch. */
    @Test
    void participant_seesChaptersOnTheReplayTheyMayWatch() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO live_class_participants (id, institution_id, created_at, is_deleted, "
                        + "live_class_id, user_id, role, joined_at) VALUES (?, ?, ?, false, ?, ?, ?, ?)",
                UUID.randomUUID(), institutionId, LocalDateTime.now(),
                classAId, learnerUserId, "LEARNER", LocalDateTime.now());

        jdbcTemplate.update(
                "INSERT INTO replay_chapters (id, institution_id, created_at, is_deleted, replay_id, "
                        + "title, position_seconds) VALUES (?, ?, ?, false, ?, ?, ?)",
                UUID.randomUUID(), institutionId, LocalDateTime.now(),
                replayAId, "Introduction", 0);

        mockMvc.perform(get("/v1/learner/replays/" + replayAId)
                        .header("Authorization", "Bearer " + TestTokens.userToken(learnerEmail))
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chapters[0].title").value("Introduction"))
                .andExpect(jsonPath("$.data.chapters[0].positionSeconds").value(0));
    }

    /** Entitlement is the same rule as the video: no participation, no chapters either. */
    @Test
    void nonParticipant_doesNotSeeChapters() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO replay_chapters (id, institution_id, created_at, is_deleted, replay_id, "
                        + "title, position_seconds) VALUES (?, ?, ?, false, ?, ?, ?)",
                UUID.randomUUID(), institutionId, LocalDateTime.now(),
                replayAId, "Introduction", 0);

        mockMvc.perform(get("/v1/learner/replays/" + replayAId)
                        .header("Authorization", "Bearer " + TestTokens.userToken(learnerEmail))
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());
    }

    private long chapterCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM replay_chapters WHERE replay_id = ? AND is_deleted = false",
                Long.class, replayAId);
        return count == null ? 0 : count;
    }

    private void insertInstitution(UUID id, String name, String code) {
        jdbcTemplate.update(
                "INSERT INTO institutions (id, created_at, is_deleted, name, code, type, country, is_active) "
                        + "VALUES (?, ?, false, ?, ?, 'SECONDARY', 'Tanzania', true)",
                id, LocalDateTime.now(), name, code);
    }

    private UUID insertUser(String email, UUID institutionId, String role) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, institution_id, created_at, is_deleted, email, password_hash, "
                        + "first_name, last_name, role, is_active, is_email_verified) "
                        + "VALUES (?, ?, ?, false, ?, 'test-hash', 'Chapter', 'Tester', ?, true, true)",
                id, institutionId, LocalDateTime.now(), email, role);
        // institution_memberships.id is a bigint sequence, unlike every other id here (uuid),
        // so it takes the column default rather than a generated UUID.
        jdbcTemplate.update(
                "INSERT INTO institution_memberships (created_at, is_deleted, user_id, "
                        + "institution_id, role, is_active) VALUES (?, false, ?, ?, ?, true)",
                LocalDateTime.now(), id, institutionId,
                "STUDENT".equals(role) ? "STUDENT" : "TEACHER");
        return id;
    }

    /** teachers.id is distinct from users.id; the controller resolves one to the other. */
    private UUID insertTeacher(UUID userId, UUID institutionId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO teachers (id, institution_id, created_at, is_deleted, user_id, status) "
                        + "VALUES (?, ?, ?, false, ?, 'ACTIVE')",
                id, institutionId, LocalDateTime.now(), userId);
        return id;
    }

    private UUID insertLiveClass(UUID teacherId, UUID institutionId, String title) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO live_classes (id, institution_id, created_at, is_deleted, teacher_id, title, "
                        + "scheduled_at, duration_minutes, status, max_participants, recording_enabled, "
                        + "session_type, timezone, is_recurring, lobby_enabled) "
                        + "VALUES (?, ?, ?, false, ?, ?, ?, 60, 'COMPLETED', 30, true, 'LECTURE', "
                        + "'Africa/Dar_es_Salaam', false, false)",
                id, institutionId, LocalDateTime.now(), teacherId, title,
                LocalDateTime.now().minusHours(2));
        return id;
    }

    private UUID insertReplay(UUID liveSessionId, UUID institutionId, int durationSeconds) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO replays (id, institution_id, created_at, is_deleted, live_session_id, title, "
                        + "recording_url, status, view_count, last_position_seconds, duration_seconds) "
                        + "VALUES (?, ?, ?, false, ?, 'Recorded class', 'https://storage.example/r.mp4', "
                        + "'AVAILABLE', 0, 0, ?)",
                id, institutionId, LocalDateTime.now(), liveSessionId, durationSeconds);
        return id;
    }
}
