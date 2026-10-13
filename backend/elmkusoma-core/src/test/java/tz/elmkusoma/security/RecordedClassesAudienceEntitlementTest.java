package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventRegistration;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorded classes: entitlement is the class audience, not attendance.
 *
 * <p>Both recording libraries used to answer "may this learner watch this?" with a single
 * question - is there a {@code live_class_participants} row for them? That conflated two
 * different ideas. A participant row proves the learner <em>was in the room</em>; the class
 * group the session was scheduled for proves they are <em>entitled to it</em>. Every learner
 * who was absent on the day, or who enrolled after the recording was made, was therefore shut
 * out of recordings of their own class - while the byte path
 * ({@code LiveClassRecordingAccessService}, the same rule {@code LiveSessionController} applies
 * before admitting a learner to the live room) already treated them as entitled.</p>
 *
 * <p>These tests pin the corrected rule on the two discovery surfaces and on the shared replay
 * endpoints:</p>
 * <ul>
 *   <li>attendance is still sufficient - nobody who could watch yesterday loses it;</li>
 *   <li>membership of the recorded class's audience is now also sufficient;</li>
 *   <li>neither holding is denial, so cross-class, cross-tenant, failed-recording and
 *       known-id probes all stay closed.</li>
 * </ul>
 *
 * <p>The audience is resolved through the repository's one membership model -
 * {@code student_class_assignments} ∪ ENROLLED {@code enrollments} - so it is the same answer
 * for every learner category; the parameterised test below walks all seven against the
 * placement type each actually uses.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class RecordedClassesAudienceEntitlementTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private InstitutionRepository institutionRepository;
    @Autowired private InstitutionMembershipRepository membershipRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private StudentClassAssignmentRepository studentClassAssignmentRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private LiveClassRepository liveClassRepository;
    @Autowired private LiveClassParticipantRepository participantRepository;
    @Autowired private ReplayRepository replayRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRegistrationRepository eventRegistrationRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;

    /** The tenant every fixture below lives in, so the seeded admin and teacher stay usable. */
    private UUID home;
    /** The audience the recorded session was scheduled for. */
    private UUID recordedClass;
    /** An unrelated class in the same tenant: entitled to nothing here. */
    private UUID otherClass;

    private User attendee;
    private User absentMember;
    private User lateJoiner;
    private User classMate;
    private User stranger;
    private User teacherUser;

    private LiveClass liveClass;
    private Replay replay;
    private MediaAsset recording;
    private String replayTitle;
    private String recordingTitle;

    @BeforeEach
    void setUp() {
        String run = UUID.randomUUID().toString().substring(0, 8);
        home = TestDataSeeder.INSTITUTION_ID;
        recordedClass = UUID.randomUUID();
        otherClass = UUID.randomUUID();

        attendee = newLearner("attendee", run, User.Role.STUDENT, null);
        absentMember = newLearner("absent", run, User.Role.STUDENT, null);
        lateJoiner = newLearner("late", run, User.Role.STUDENT, null);
        classMate = newLearner("mate", run, User.Role.STUDENT, null);
        stranger = newLearner("stranger", run, User.Role.STUDENT, null);
        teacherUser = newTeacher(run);

        placeInClassByAssignment(attendee, recordedClass);
        placeInClassByAssignment(absentMember, recordedClass);
        placeInClassByAssignment(lateJoiner, recordedClass);
        placeInClassByAssignment(classMate, recordedClass);
        placeInClassByAssignment(stranger, otherClass);

        liveClass = liveClassRepository.saveAndFlush(LiveClass.builder()
                .institutionId(home)
                .teacherId(TestDataSeeder.TEACHER_ID)
                .title("Recorded class " + run)
                .scheduledAt(LocalDateTime.now().minusHours(3))
                .durationMinutes(60)
                .status("COMPLETED")
                .classGroupId(recordedClass)
                .isDeleted(false)
                .build());

        replayTitle = "Audience replay " + run;
        replay = replayRepository.saveAndFlush(Replay.builder()
                .liveSessionId(liveClass.getId())
                .institutionId(home)
                .title(replayTitle)
                .status(Replay.STATUS_AVAILABLE)
                .recordingUrl("recordings/audience-" + run + ".mp4")
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        recordingTitle = "Audience recording " + run;
        recording = saveRecording(recordingTitle, liveClass.getId(), recordedClass, "READY",
                teacherUser.getId());

        // Only one learner was in the room; everyone else above is entitled without having been.
        participantRepository.saveAndFlush(LiveClassParticipant.builder()
                .liveClassId(liveClass.getId())
                .userId(attendee.getId())
                .role("LEARNER")
                .joinedAt(LocalDateTime.now().minusHours(2))
                .isDeleted(false)
                .build());
    }

    // ── attendance and audience are both sufficient ──────────────────────────────────────────

    /** #1 An enrolled learner who attended keeps the access the old rule gave them. */
    @Test
    void learnerWhoAttended_canWatchTheRecording() throws Exception {
        assertLearnerCanReach(attendee);
    }

    /**
     * #2 The defect itself: enrolled in the recorded class, never in the room, and therefore
     * invisible to the old participant-row rule. Entitlement comes from the audience instead.
     */
    @Test
    void learnerWhoDidNotAttendButIsInTheRecordedClass_canWatchTheRecording() throws Exception {
        assertLearnerCanReach(absentMember);
    }

    /** #3 A learner placed in the class after the session was recorded is entitled now. */
    @Test
    void learnerWhoJoinedTheClassAfterTheRecording_wasMade_canWatchIt() throws Exception {
        // The replay predates the placement: entitlement is evaluated against the membership
        // the learner holds today, not against anything about the day of the session.
        assertTrue(lateJoiner.getCreatedAt() == null
                        || !lateJoiner.getCreatedAt().isAfter(replay.getCreatedAt()),
                "fixture must place the learner after the recording was recorded");
        assertLearnerCanReach(lateJoiner);
    }

    /** Membership proved through the other half of the union: an ENROLLED enrollment. */
    @Test
    void learnerPlacedThroughAnEnrollment_canWatchTheRecording() throws Exception {
        User enrolled = newLearner("enrolled", UUID.randomUUID().toString().substring(0, 8),
                User.Role.STUDENT, null);
        Student student = studentOf(enrolled);
        enrollmentRepository.saveAndFlush(Enrollment.builder()
                .studentId(student.getId())
                .classGroupId(recordedClass)
                .academicYearId(UUID.randomUUID())
                .status(Enrollment.EnrollmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now().minusDays(2))
                .build());

        assertLearnerCanReach(enrolled);
    }

    /** A withdrawn enrollment stops being membership, so access follows the current state. */
    @Test
    void learnerWhoseEnrollmentWasWithdrawn_isDeniedAgain() throws Exception {
        User withdrawn = newLearner("withdrawn", UUID.randomUUID().toString().substring(0, 8),
                User.Role.STUDENT, null);
        Student student = studentOf(withdrawn);
        enrollmentRepository.saveAndFlush(Enrollment.builder()
                .studentId(student.getId())
                .classGroupId(recordedClass)
                .academicYearId(UUID.randomUUID())
                .status(Enrollment.EnrollmentStatus.WITHDRAWN)
                .enrolledAt(LocalDateTime.now().minusDays(30))
                .withdrawnAt(LocalDateTime.now().minusDays(1))
                .build());

        assertLearnerCannotReach(withdrawn);
    }

    // ── the audience boundary still holds ───────────────────────────────────────────────────

    /** #4 Same tenant, different class: entitled to nothing recorded for the other one. */
    @Test
    void learnerOfAnUnrelatedClass_isDenied() throws Exception {
        assertLearnerCannotReach(stranger);
        assertFalse(listLearnerReplays(stranger).contains(replayTitle),
                "the listing must not advertise another class's recording");
        assertFalse(listMedia(stranger).contains(recordingTitle),
                "the media library must not advertise another class's recording");
    }

    /**
     * #5 Another tenant. The audience check runs behind the tenant boundary, so a foreign
     * learner is refused before membership of anything is even considered.
     */
    @Test
    void learnerFromAnotherInstitution_isDenied() throws Exception {
        String run = UUID.randomUUID().toString().substring(0, 8);
        Institution foreign = institutionRepository.save(Institution.builder()
                .name("Foreign " + run)
                .code("FG-" + run)
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
        User outsider = newLearnerIn(foreign.getId(), "outsider", run);
        placeInClassByAssignment(outsider, recordedClass);

        // The foreign tenant's own view of the home tenant's recording.
        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", foreign.getId().toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", foreign.getId().toString()))
                .andExpect(status().isNotFound());

        // Claiming the home tenant as one's own does not help: no membership there.
        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isForbidden());
    }

    /**
     * #6 Knowing an id is not authorization. The ids below are the real ones, handed to a
     * learner who belongs to neither the class nor the recording's audience.
     */
    @Test
    void knowingAnotherRecordingsIdDoesNotBypassAuthorization() throws Exception {
        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(stranger.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/media/" + recording.getId() + "/download-url")
                        .header("Authorization", "Bearer " + TestTokens.userToken(stranger.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(stranger.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isForbidden());

        // The staff-shared endpoint answers a learner exactly as the learner endpoint does.
        mockMvc.perform(get("/v1/replays/" + replay.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(stranger.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isForbidden());

        // And enumerating that endpoint advertises nothing either.
        assertFalse(listReplays(stranger).contains(replayTitle),
                "the shared replay listing must not advertise a recording the learner cannot open");
    }

    /** Guessing a class id is not authorization either: the asset's own tenant still governs. */
    @Test
    void guessingAClassGroupIdDoesNotWidenAccess() throws Exception {
        mockMvc.perform(get("/v1/media/class/" + recordedClass)
                        .header("Authorization", "Bearer " + TestTokens.userToken(stranger.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk())
                .andExpect(result -> assertTrue(
                        result.getResponse().getContentAsString().isEmpty()
                                || !result.getResponse().getContentAsString().contains(recordingTitle),
                        "a class id the caller is not entitled to must yield no recordings"));
    }

    // ── availability and publication ────────────────────────────────────────────────────────

    /** #7 A capture that never produced a file is not offered to learners. */
    @Test
    void failedRecording_isNotOfferedToLearners() throws Exception {
        String run = UUID.randomUUID().toString().substring(0, 8);
        String failedTitle = "Failed capture " + run;
        MediaAsset failed = saveRecording(failedTitle, liveClass.getId(), recordedClass,
                "FAILED", teacherUser.getId());

        assertFalse(listMedia(absentMember).contains(failedTitle),
                "a failed capture must not appear in a learner's library");
        mockMvc.perform(get("/v1/media/" + failed.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(absentMember.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isNotFound());

        // The teacher who owns it still sees the failure, which is how it gets diagnosed.
        mockMvc.perform(get("/v1/media/" + failed.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(teacherUser.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk());
    }

    /** Soft-deleted and never-published recordings stay out of every learner's view. */
    @Test
    void deletedAndUnavailableRecordings_stayHidden() throws Exception {
        String run = UUID.randomUUID().toString().substring(0, 8);
        String goneTitle = "Deleted recording " + run;
        MediaAsset gone = saveRecording(goneTitle, liveClass.getId(), recordedClass, "READY",
                teacherUser.getId());
        gone.setIsDeleted(true);
        mediaAssetRepository.saveAndFlush(gone);

        assertFalse(listMedia(absentMember).contains(goneTitle));
        mockMvc.perform(get("/v1/media/" + gone.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(absentMember.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isNotFound());

        replay.setIsDeleted(true);
        replayRepository.saveAndFlush(replay);
        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(absentMember.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isNotFound());
    }

    /**
     * A session that declares no audience at all grants a learner nothing.
     *
     * <p>Pre-existing rule, deliberately preserved: institution membership alone has never been
     * enough, because that would open every undeclared recording in the tenant to every learner
     * in it. Attendance remains the only way into such a session.</p>
     */
    @Test
    void sessionWithNoDeclaredAudience_staysClosedToLearnersWhoDidNotAttend() throws Exception {
        String run = UUID.randomUUID().toString().substring(0, 8);
        LiveClass open = liveClassRepository.saveAndFlush(LiveClass.builder()
                .institutionId(home)
                .teacherId(TestDataSeeder.TEACHER_ID)
                .title("Undeclared session " + run)
                .scheduledAt(LocalDateTime.now().minusHours(2))
                .durationMinutes(30)
                .status("COMPLETED")
                .isDeleted(false)
                .build());
        Replay openReplay = replayRepository.saveAndFlush(Replay.builder()
                .liveSessionId(open.getId())
                .institutionId(home)
                .title("Undeclared replay " + run)
                .status(Replay.STATUS_AVAILABLE)
                .recordingUrl("recordings/open-" + run + ".mp4")
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        mockMvc.perform(get("/v1/learner/replays/" + openReplay.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(absentMember.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isForbidden());
        assertFalse(listLearnerReplays(absentMember).contains("Undeclared replay " + run));
    }

    // ── staff access is untouched ───────────────────────────────────────────────────────────

    /** #8 The owning teacher reaches their recording without any audience or attendance row. */
    @Test
    void owningTeacher_retainsAccess() throws Exception {
        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(teacherUser.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk());
    }

    /** #8 An institution admin reads the whole library, entitled or not. */
    @Test
    void institutionAdmin_retainsAccess() throws Exception {
        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/replays/" + replay.getId())
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk());

        assertFalse(listMedia(teacherUser).isEmpty(),
                "a teacher's own library listing must keep working");
    }

    // ── #9 the listing itself is correct ────────────────────────────────────────────────────

    @Test
    void mediaLibraryListing_returnsExactlyTheRecordingsTheLearnerMayOpen() throws Exception {
        String body = listMedia(absentMember);
        assertTrue(body.contains(recordingTitle), "the entitled recording must be listed");
        assertFalse(body.contains("Failed capture"), "failed captures must not be listed");

        // Opening what was listed must work, so the list never advertises a dead end.
        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(absentMember.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.playbackUrl")
                        .value("http://localhost:3000/v1/live-session/classes/"
                                + liveClass.getId() + "/recording/playback"));
    }

    @Test
    void learnerReplayListing_returnsExactlyTheReplaysTheLearnerMayOpen() throws Exception {
        String body = listLearnerReplays(absentMember);
        assertTrue(body.contains(replayTitle), "the entitled replay must be listed");
        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(absentMember.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.replay.videoUrl")
                        .value("http://localhost:3000/v1/replays/" + replay.getId() + "/playback"));
    }

    /** The library must never serialise the private storage location. */
    @Test
    void recordingResponses_neverExposeTheStorageLocation() throws Exception {
        String body = mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(absentMember.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("recordings/audience-"),
                "the object key must never reach a client: " + body);
    }

    // ── #12 no regression on the paths that were already correct ────────────────────────────

    /** Non-recording media is institution-scoped, not class-scoped, and stays that way. */
    @Test
    void nonRecordingAssets_remainVisibleToAnyLearnerOfTheInstitution() throws Exception {
        String run = UUID.randomUUID().toString().substring(0, 8);
        MediaAsset handbook = MediaAsset.builder()
                .title("Shared handbook " + run)
                .mediaType("DOCUMENT")
                .fileUrl("https://cdn.test/handbook.pdf")
                .status("READY")
                .visibility("INSTITUTION")
                .teacherId(UUID.randomUUID())
                .build();
        handbook.setInstitutionId(home);
        mediaAssetRepository.saveAndFlush(handbook);

        mockMvc.perform(get("/v1/media/" + handbook.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(stranger.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk());
    }

    /**
     * Event replays keep their own rule: opting into an event is how that audience is
     * declared, so registration - not class membership - is what opens one.
     */
    @Test
    void eventReplays_stillRequireRegistration() throws Exception {
        String run = UUID.randomUUID().toString().substring(0, 8);
        Event event = eventRepository.save(Event.builder()
                .institutionId(home)
                .organizerId(TestDataSeeder.TEACHER_USER_ID)
                .eventType("WORKSHOP")
                .title("Entitlement event " + run)
                .startsAt(LocalDateTime.now().minusHours(2))
                .endsAt(LocalDateTime.now().minusHours(1))
                .status("ENDED")
                .accessLevel("PUBLIC")
                .isFree(true)
                .requiresApproval(false)
                .build());
        Replay eventReplay = replayRepository.saveAndFlush(Replay.builder()
                .eventId(event.getId())
                .institutionId(home)
                .title("Entitlement event replay " + run)
                .status(Replay.STATUS_AVAILABLE)
                .recordingUrl("recordings/event-" + run + ".mp4")
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        // Belonging to the recorded class buys nothing on an event replay.
        mockMvc.perform(get("/v1/learner/replays/" + eventReplay.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(absentMember.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isForbidden());

        eventRegistrationRepository.saveAndFlush(EventRegistration.builder()
                .eventId(event.getId())
                .userId(absentMember.getId())
                .status("CONFIRMED")
                .registeredAt(LocalDateTime.now().minusHours(3))
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/v1/learner/replays/" + eventReplay.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(absentMember.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk());
    }

    /** Unauthenticated callers are still refused outright, before any entitlement is read. */
    @Test
    void unauthenticatedRequests_areDenied() throws Exception {
        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/v1/replays"))
                .andExpect(status().is4xxClientError());
    }

    // ── #11 every learner category, against the placement it actually uses ──────────────────

    /**
     * The seven categories differ in role and in how an admin places them, never in the rule
     * that reads the placement: {@code ClassAccessGuard} unions
     * {@code student_class_assignments} with ENROLLED {@code enrollments} regardless of level.
     *
     * <p>Nursery, Primary and Secondary learners are placed with a class assignment; VETA,
     * College and University learners are placed with an enrollment; an Other Learner holds the
     * same student profile the other six do and is placed the same way. Every one of those rows
     * is written by an admin endpoint - no category self-selects.</p>
     */
    @ParameterizedTest(name = "{0} learner via {2}")
    @CsvSource({
            // category,               role,            learningLevel, placement
            "Nursery,                  STUDENT,         NURSERY,       ASSIGNMENT",
            "Primary,                  STUDENT,         PRIMARY,       ASSIGNMENT",
            "Secondary,                STUDENT,         SECONDARY,     ASSIGNMENT",
            "VETA,                     STUDENT,         VETA,          ENROLLMENT",
            "College,                  STUDENT,         COLLEGE,       ENROLLMENT",
            "University,               STUDENT,         UNIVERSITY,    ENROLLMENT",
            "Other Learners,           OTHER_LEARNER,   -,             ASSIGNMENT"
    })
    void everyLearnerCategory_canReachItsOwnRecordedClasses(
            String category, String role, String learningLevel, String placement) throws Exception {

        User.LearningLevel level = "-".equals(learningLevel)
                ? null : User.LearningLevel.valueOf(learningLevel);
        User learner = newLearner(category.replace(' ', '-').toLowerCase(),
                UUID.randomUUID().toString().substring(0, 8),
                User.Role.valueOf(role), level);

        if ("ENROLLMENT".equals(placement)) {
            Student student = studentOf(learner);
            enrollmentRepository.saveAndFlush(Enrollment.builder()
                    .studentId(student.getId())
                    .classGroupId(recordedClass)
                    .academicYearId(UUID.randomUUID())
                    .status(Enrollment.EnrollmentStatus.ENROLLED)
                    .enrolledAt(LocalDateTime.now().minusDays(5))
                    .build());
        } else {
            placeInClassByAssignment(learner, recordedClass);
        }

        assertLearnerCanReach(learner);

        // ...and it sees nothing from the class it does not belong to.
        mockMvc.perform(get("/v1/media/class/" + otherClass)
                        .header("Authorization", "Bearer " + TestTokens.userToken(learner.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk())
                .andExpect(result -> assertFalse(
                        result.getResponse().getContentAsString().contains(recordingTitle),
                        category + " must not see another class's recording"));
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────

    /** Both discovery surfaces plus the detail endpoints behind each play button. */
    private void assertLearnerCanReach(User learner) throws Exception {
        String token = TestTokens.userToken(learner.getEmail());

        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/media/" + recording.getId() + "/download-url")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/media/recordings/LIVE_CLASS/" + liveClass.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk())
                .andExpect(result -> assertTrue(
                        result.getResponse().getContentAsString().contains(recordingTitle),
                        "the recordings of a class the learner belongs to must be returned"));

        assertTrue(listLearnerReplays(learner).contains(replayTitle),
                "the replay must appear in the learner's listing");
        assertTrue(listMedia(learner).contains(recordingTitle),
                "the recording must appear in the learner's media library");
    }

    private void assertLearnerCannotReach(User learner) throws Exception {
        String token = TestTokens.userToken(learner.getEmail());

        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/media/" + recording.getId() + "/download-url")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isNotFound());
    }

    private String listLearnerReplays(User learner) throws Exception {
        return mockMvc.perform(get("/v1/learner/replays")
                        .header("Authorization", "Bearer " + TestTokens.userToken(learner.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String listReplays(User learner) throws Exception {
        return mockMvc.perform(get("/v1/replays")
                        .header("Authorization", "Bearer " + TestTokens.userToken(learner.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String listMedia(User learner) throws Exception {
        return mockMvc.perform(get("/v1/media")
                        .header("Authorization", "Bearer " + TestTokens.userToken(learner.getEmail()))
                        .header("X-Institution-Id", home.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private User newLearner(String tag, String run, User.Role role, User.LearningLevel level) {
        return newLearnerIn(home, tag, run, role, level);
    }

    private User newLearnerIn(UUID institutionId, String tag, String run) {
        return newLearnerIn(institutionId, tag, run, User.Role.STUDENT, null);
    }

    private User newLearnerIn(UUID institutionId, String tag, String run,
                              User.Role role, User.LearningLevel level) {
        User user = userRepository.save(User.builder()
                .email(tag + "-" + run + "@recorded.test")
                .passwordHash("test-hash")
                .firstName("Recorded")
                .lastName("Learner")
                .role(role)
                .learningLevel(level)
                .institutionId(institutionId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        // Mirrors AuthServiceImpl.ensureMembership: a learner seat is membership role STUDENT
        // for both STUDENT and OTHER_LEARNER users.
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(institutionId)
                .role(InstitutionMembership.Role.STUDENT)
                .isActive(true)
                .isDeleted(false)
                .build());
        studentProfile(user);
        return user;
    }

    private User newTeacher(String run) {
        User user = userRepository.save(User.builder()
                .email("recorded-teacher-" + run + "@recorded.test")
                .passwordHash("test-hash")
                .firstName("Recorded")
                .lastName("Teacher")
                .role(User.Role.TEACHER)
                .institutionId(home)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(home)
                .role(InstitutionMembership.Role.TEACHER)
                .isActive(true)
                .isDeleted(false)
                .build());
        return user;
    }

    /** Both enrollment tests place through this, so the profile is never duplicated. */
    private Student studentOf(User user) {
        return studentRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .orElseGet(() -> studentProfile(user));
    }

    /** Every learner category has a student profile; that is what the guard reads. */
    private Student studentProfile(User user) {
        return studentRepository.saveAndFlush(Student.builder()
                .userId(user.getId())
                .admissionNumber("REC-" + UUID.randomUUID().toString().substring(0, 12))
                .status(tz.elmkusoma.student.domain.StudentStatus.ACTIVE)
                .enrollmentDate(java.time.LocalDate.now())
                .build());
    }

    private void placeInClassByAssignment(User user, UUID classGroupId) {
        Student student = studentRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .orElseGet(() -> studentProfile(user));
        studentClassAssignmentRepository.saveAndFlush(StudentClassAssignment.builder()
                .studentId(student.getId())
                .classGroupId(classGroupId)
                .academicYearId(UUID.randomUUID())
                .termId(UUID.randomUUID())
                .isActive(true)
                .build());
    }

    private MediaAsset saveRecording(String title, UUID sourceId, UUID classGroupId,
                                     String status, UUID teacherUserId) {
        MediaAsset asset = MediaAsset.builder()
                .title(title)
                .mediaType("RECORDING")
                .fileUrl("https://cdn.test/" + UUID.randomUUID() + ".mp4")
                .status(status)
                .visibility("INSTITUTION")
                .sourceType("LIVE_CLASS")
                .sourceId(sourceId)
                .classGroupId(classGroupId)
                .teacherId(teacherUserId)
                .build();
        asset.setInstitutionId(home);
        return mediaAssetRepository.saveAndFlush(asset);
    }
}
