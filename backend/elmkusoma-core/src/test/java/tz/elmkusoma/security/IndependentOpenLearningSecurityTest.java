package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.academic.domain.AcademicYear;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.academic.domain.Grade;
import tz.elmkusoma.academic.domain.Term;
import tz.elmkusoma.academic.repository.AcademicYearRepository;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.repository.GradeRepository;
import tz.elmkusoma.academic.repository.TermRepository;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherStatus;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Independent teachers, cross-institution teaching and open learning — acceptance tests
 * G1–G9, through the real filter chain (JwtAuthenticationFilter → JwtRequestAttributeFilter →
 * OrganizationContextResolver → method security → GlobalExceptionHandler).
 *
 * <p>The rule under test throughout: <b>affiliation is neither the audience nor its
 * restriction</b>. A school-affiliated teacher may host a session every school can attend;
 * an unaffiliated (independent) teacher may create and publish one; a learner's school
 * decides only what that learner's own school offers privately, while a genuine open
 * learning session — PUBLISHED + PUBLIC + no declared class audience — is discoverable and
 * joinable across institutions and by institution-less learners. Everything weaker stays
 * closed: private classes, another school's lessons, grades, submissions, attendance and
 * recordings are never reachable from outside their audience.</p>
 *
 * <ul>
 *   <li>G1 — affiliated teacher hosts an open session for multiple schools and keeps her
 *       own school name.</li>
 *   <li>G2 — independent teacher creates, submits and gets a session published with no
 *       school attached, and shows no fabricated school name.</li>
 *   <li>G3 — independently registered learner discovers and joins an open session without
 *       any enrollment or school affiliation.</li>
 *   <li>G4 — school student keeps their school identity after joining open learning.</li>
 *   <li>G5 — school-specific lessons and private classes stay restricted for other
 *       institutions and independent learners.</li>
 *   <li>G6 — unauthorized teachers cannot publish into another school's scope (foreign
 *       audience target, mismatched tenant header, non-admin governance call).</li>
 *   <li>G7 — independent learners cannot reach restricted records: progress, grades,
 *       submissions, attendance, rosters or private recordings.</li>
 *   <li>G8 — audience restrictions apply consistently across list, detail, join, download
 *       and replay.</li>
 *   <li>G9 — existing institutional workflows continue to work unchanged.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IndependentOpenLearningSecurityTest {

    private static final String RUN = UUID.randomUUID().toString().substring(0, 8);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    private Institution instA;
    private Institution instB;
    private User teacherA;
    private User teacherInd;
    private User studentA;
    private User studentB;
    private User learnerInd;
    private User platformAdmin;
    private Teacher teacherARow;
    private Teacher teacherIndRow;
    private ClassGroup classGroupA;
    private ClassGroup classGroupB;

    @BeforeAll
    void seedFixtures() {
        instA = saveInstitution("OpenLearn-A " + RUN);
        instB = saveInstitution("OpenLearn-B " + RUN);

        // School-affiliated teacher of school A.
        teacherA = saveUser("openlearn-teacher-a-" + RUN + "@test.com", User.Role.TEACHER, instA);
        Teacher teacherRow = Teacher.builder()
                .userId(teacherA.getId())
                .employeeNumber("OPENLEARN-TA-" + RUN)
                .status(TeacherStatus.ACTIVE)
                .build();
        teacherRow.setInstitutionId(instA.getId());
        teacherARow = teacherRepository.save(teacherRow);

        // Independent teacher: no school, no membership — and, before the schema change,
        // not even representable (teachers.institution_id was NOT NULL since V04).
        teacherInd = saveIndependentUser("openlearn-teacher-ind-" + RUN + "@test.com", User.Role.TEACHER);
        Teacher independentRow = Teacher.builder()
                .userId(teacherInd.getId())
                .employeeNumber("OPENLEARN-TI-" + RUN)
                .status(TeacherStatus.ACTIVE)
                .build();
        independentRow.setInstitutionId(null);
        teacherIndRow = teacherRepository.save(independentRow);

        // School students with student profiles (school identity is resolved from these).
        studentA = saveUser("openlearn-student-a-" + RUN + "@test.com", User.Role.STUDENT, instA);
        saveStudentProfile(studentA, "OPENLEARN-SA-" + RUN, instA);
        studentB = saveUser("openlearn-student-b-" + RUN + "@test.com", User.Role.STUDENT, instB);
        saveStudentProfile(studentB, "OPENLEARN-SB-" + RUN, instB);

        // Independent learner: no school, no membership, no student profile anywhere.
        learnerInd = saveIndependentUser("openlearn-learner-ind-" + RUN + "@test.com", User.Role.OTHER_LEARNER);

        platformAdmin = saveUser("openlearn-platform-" + RUN + "@test.com", User.Role.ADMIN, instA);

        // Real class groups (academic hierarchy rows included) — the lesson and audience-target
        // surfaces below deny against these, so they must exist in their own institutions.
        classGroupA = saveClassGroup(instA, "A-" + RUN);
        classGroupB = saveClassGroup(instB, "B-" + RUN);
    }

    // ── G1 ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void g1_affiliatedTeacherHostsOpenSessionForEverySchoolAndKeepsHerOwnSchoolName() throws Exception {
        // The teacher of school A creates an explicitly PUBLIC session through the API.
        MvcResult created = mockMvc.perform(post("/v1/teachers/me/live-classes")
                        .header("Authorization", token(teacherA))
                        .header("X-Institution-Id", instA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"G1 open physics " + RUN + "\","
                                + "\"scheduledAt\":\"" + LocalDateTime.now().plusHours(2) + "\","
                                + "\"durationMinutes\":60,\"accessLevel\":\"PUBLIC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessLevel").value("PUBLIC"))
                .andExpect(jsonPath("$.data.publicationStatus").value("DRAFT"))
                .andReturn();
        String classId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // Governance: submit → platform-admin approve → publish (publication stays the gate).
        mockMvc.perform(post("/v1/teachers/me/live-classes/" + classId + "/submit-for-approval")
                        .header("Authorization", token(teacherA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + classId + "/approve")
                        .header("Authorization", token(platformAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Open learning session\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicationStatus").value("APPROVED"));
        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + classId + "/publish")
                        .header("Authorization", token(platformAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicationStatus").value("PUBLISHED"));

        LiveClass liveClass = liveClassRepository.findById(UUID.fromString(classId)).orElseThrow();
        liveClass.setStatus("IN_PROGRESS");
        liveClassRepository.save(liveClass);

        // Learners from her own school, from the OTHER school, and an independent learner —
        // all join the same open session (G1's "for students of multiple schools").
        mockMvc.perform(post("/v1/live-session/join/" + classId)
                        .header("Authorization", token(studentA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(post("/v1/live-session/join/" + classId)
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(post("/v1/live-session/join/" + classId)
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Hosting an open session changes nothing about her own affiliation: the dashboard
        // still resolves her school from the teacher row / user organization.
        mockMvc.perform(get("/v1/teachers/me/dashboard")
                        .header("Authorization", token(teacherA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"institutionName\":\"")))
                .andExpect(content().string(containsString(instA.getName())));
    }

    // ── G2 ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void g2_independentTeacherCreatesAndPublishesOpenSessionWithoutAnySchool() throws Exception {
        // No X-Institution-Id header exists for this caller at all — and none may be forced.
        MvcResult created = mockMvc.perform(post("/v1/teachers/me/live-classes")
                        .header("Authorization", token(teacherInd))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"G2 independent seminar " + RUN + "\","
                                + "\"scheduledAt\":\"" + LocalDateTime.now().plusHours(3) + "\","
                                + "\"durationMinutes\":45}"))
                .andExpect(status().isCreated())
                // An institution-less teacher declares no school scope, so the honest audience
                // for their session is PUBLIC — asserted here, not assumed.
                .andExpect(jsonPath("$.data.accessLevel").value("PUBLIC"))
                .andExpect(jsonPath("$.data.publicationStatus").value("DRAFT"))
                .andReturn();
        String classId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // Authorized through the exact same editorial workflow as everyone else.
        mockMvc.perform(post("/v1/teachers/me/live-classes/" + classId + "/submit-for-approval")
                        .header("Authorization", token(teacherInd)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + classId + "/approve")
                        .header("Authorization", token(platformAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Independent open session\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + classId + "/publish")
                        .header("Authorization", token(platformAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicationStatus").value("PUBLISHED"));

        // After publication it is publicly discoverable — the "authorized" part of the flow.
        String publicBody = mockMvc.perform(get("/v1/public/live-classes/upcoming"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(publicBody.contains("G2 independent seminar " + RUN) == false,
                "a published open session must be publicly discoverable");

        // Dashboard and list work without a school, and show no fabricated affiliation.
        String dashboard = mockMvc.perform(get("/v1/teachers/me/dashboard")
                        .header("Authorization", token(teacherInd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(dashboard.contains("\"institutionName\":\""),
                "independent teacher must not be shown a school name, body: " + dashboard);
        assertFalse(dashboard.contains(instA.getName()) || dashboard.contains(instB.getName()),
                "independent teacher must not inherit any other institution's name");

        String list = mockMvc.perform(get("/v1/teachers/me/live-classes")
                        .header("Authorization", token(teacherInd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        assertFalse(list.contains("G2 independent seminar " + RUN) == false,
                "the independent teacher's own session list must include their session");

        String profile = mockMvc.perform(get("/v1/teachers/me/profile")
                        .header("Authorization", token(teacherInd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(profile.contains(instA.getName()) || profile.contains(instB.getName()),
                "an independent teacher's profile must not name another school");
    }

    // ── G3 ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void g3_independentLearnerDiscoversAndJoinsOpenSessionWithoutEnrollmentOrSchool() throws Exception {
        // A published open session of the independent teacher (fixture state = the state the
        // workflow in G2 produces). The learner below has no student row, no enrollment and
        // no membership anywhere — the fixture is the proof, nothing is faked to grant access.
        LiveClass open = seedClass("G3 open for everyone " + RUN,
                null, teacherIndRow.getId(),
                "PUBLIC", "PUBLISHED", null, "IN_PROGRESS");

        // Discovery through the learner's own surface, with no institution header at all.
        mockMvc.perform(get("/v1/learner/live-classes")
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("G3 open for everyone " + RUN)));

        mockMvc.perform(get("/v1/learner/live-classes/" + open.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isOk());

        // Joining — no school affiliation, no enrollment, no fake institution header.
        mockMvc.perform(post("/v1/live-session/join/" + open.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── G4 ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void g4_schoolStudentKeepsSchoolNameAfterJoiningOpenLearning() throws Exception {
        LiveClass open = seedClass("G4 cross-school open " + RUN,
                instA.getId(), teacherARow.getId(),
                "PUBLIC", "PUBLISHED", null, "IN_PROGRESS");

        // School B's student joins school A's open session…
        mockMvc.perform(post("/v1/live-session/join/" + open.getId())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // …and the open session shows up in their own learner list alongside school B's own.
        String list = mockMvc.perform(get("/v1/learner/live-classes")
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(list.contains("G4 cross-school open " + RUN) == false,
                "eligible open learning must appear for the school's own learners");

        // Joining open learning never rewrites (or fabricates) the learner's school identity.
        String summary = mockMvc.perform(get("/v1/student/dashboard/summary")
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(summary.contains(instB.getName()) == false,
                "school student must keep their own school name on the dashboard, body: " + summary);
        assertFalse(summary.contains(instA.getName()),
                "joining school A's open session must not swap the student's school to A");
    }

    // ── G5 ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void g5_schoolSpecificLessonsAndPrivateClassesStayRestricted() throws Exception {
        LiveClass privateClass = seedClass("G5 private class " + RUN,
                instA.getId(), teacherARow.getId(),
                "INSTITUTION", "PUBLISHED", classGroupA.getId(), "IN_PROGRESS");

        // Lessons of school A's class group: own school reads, other school is refused,
        // an institution-less learner never even gets a scoping context (400, no data).
        mockMvc.perform(get("/v1/learning/lessons/class/" + classGroupA.getId())
                        .header("Authorization", token(studentA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/learning/lessons/class/" + classGroupA.getId())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/learning/lessons/class/" + classGroupA.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isBadRequest());

        // The private class is invisible and unreachable from outside its audience.
        mockMvc.perform(get("/v1/learner/live-classes/" + privateClass.getId())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/learner/live-classes/" + privateClass.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/v1/live-session/join/" + privateClass.getId())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/live-session/join/" + privateClass.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isForbidden());
    }

    // ── G6 ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void g6_unauthorizedTeachersCannotPublishIntoAnotherSchoolsScope() throws Exception {
        // Foreign audience target: teacher of A aims a session at school B's class group.
        mockMvc.perform(post("/v1/teachers/me/live-classes")
                        .header("Authorization", token(teacherA))
                        .header("X-Institution-Id", instA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"G6 foreign target " + RUN + "\","
                                + "\"scheduledAt\":\"" + LocalDateTime.now().plusDays(1) + "\","
                                + "\"durationMinutes\":30,\"classGroupId\":\"" + classGroupB.getId() + "\"}"))
                .andExpect(status().isForbidden());

        // Tenant mismatch: the same teacher presents school B's header against her school A
        // membership — rejected by context resolution before any business logic runs.
        mockMvc.perform(post("/v1/teachers/me/live-classes")
                        .header("Authorization", token(teacherA))
                        .header("X-Institution-Id", instB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"G6 mismatched tenant " + RUN + "\","
                                + "\"scheduledAt\":\"" + LocalDateTime.now().plusDays(1) + "\","
                                + "\"durationMinutes\":30}"))
                .andExpect(status().isForbidden());

        // An independent teacher may not target a school's private class scope either:
        // affiliation neither authorizes nor widens an audience claim.
        mockMvc.perform(post("/v1/teachers/me/live-classes")
                        .header("Authorization", token(teacherInd))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"G6 independent foreign target " + RUN + "\","
                                + "\"scheduledAt\":\"" + LocalDateTime.now().plusDays(1) + "\","
                                + "\"durationMinutes\":30,\"classGroupId\":\"" + classGroupA.getId() + "\"}"))
                .andExpect(status().isForbidden());

        // Governance stays with platform admins: a teacher cannot approve/publish anything.
        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/"
                        + UUID.randomUUID() + "/publish")
                        .header("Authorization", token(teacherA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ── G7 ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void g7_independentLearnerCannotReachRestrictedRecordsOrPrivateRecordings() throws Exception {
        LiveClass privateRecorded = seedClass("G7 private recorded " + RUN,
                instA.getId(), teacherARow.getId(),
                "INSTITUTION", "PUBLISHED", null, "COMPLETED");
        LiveClass openWithRoster = seedClass("G7 open with roster " + RUN,
                instA.getId(), teacherARow.getId(),
                "PUBLIC", "PUBLISHED", null, "IN_PROGRESS");

        // Private recordings: entitlement is rechecked server side at access time, without
        // any institution context the caller simply cannot prove → denied, not errored-open.
        mockMvc.perform(get("/v1/live-session/classes/" + privateRecorded.getId() + "/recording/download")
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/live-session/classes/" + privateRecorded.getId() + "/recording/playback")
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isForbidden());

        // Grades: staff-only. With a valid school context the role gate itself refuses (403);
        // an independent learner cannot even produce an institution context, so the request
        // dies at context binding (400) before any lookup — no grade data either way.
        mockMvc.perform(get("/v1/academic/grades/" + UUID.randomUUID())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/academic/grades/" + UUID.randomUUID())
                        .header("Authorization", token(learnerInd)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertTrue(s == 400 || s == 403,
                            "independent learner must not reach grades, got " + s);
                });

        // Submissions of a school assignment: same two-layer refusal.
        mockMvc.perform(get("/v1/learning/assignments/" + UUID.randomUUID() + "/submissions")
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/learning/assignments/" + UUID.randomUUID() + "/submissions")
                        .header("Authorization", token(learnerInd)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertTrue(s == 400 || s == 403,
                            "independent learner must not reach submissions, got " + s);
                });

        // Another student's lesson progress — learner roles read only their own record
        // (an independent learner has no record, so someone else's is always denied).
        mockMvc.perform(get("/v1/learning/progress/student/" + studentB.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/learning/progress/student/" + studentA.getId())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());

        // Attendance records behind the oversight surface: staff-only.
        mockMvc.perform(get("/v1/oversight/attendance")
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isForbidden());

        // Student lists: even for an OPEN session, the participant roster of another
        // school's session is never exposed to outsiders.
        mockMvc.perform(get("/v1/learner/live-classes/" + openWithRoster.getId() + "/participants")
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/learner/live-classes/" + openWithRoster.getId() + "/participants")
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isNotFound());
    }

    // ── G8 ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void g8_audienceRestrictionAppliesConsistentlyAcrossListDetailJoinDownloadReplay() throws Exception {
        LiveClass privateClass = seedClass("G8 private " + RUN,
                instA.getId(), teacherARow.getId(),
                "INSTITUTION", "PUBLISHED", null, "IN_PROGRESS");
        privateClass.setRecordingUrl("https://cdn.test/g8-private-" + RUN + ".mp4");
        liveClassRepository.save(privateClass);

        LiveClass openClass = seedClass("G8 open " + RUN,
                instA.getId(), teacherARow.getId(),
                "PUBLIC", "PUBLISHED", null, "IN_PROGRESS");
        openClass.setRecordingUrl("https://cdn.test/g8-open-" + RUN + ".mp4");
        liveClassRepository.save(openClass);

        // Listing: the other school's learner sees the open session and never the private one.
        String list = mockMvc.perform(get("/v1/learner/live-classes")
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(list.contains("G8 private " + RUN), "private sessions must not be listed");
        assertFalse(list.contains("G8 open " + RUN) == false, "open sessions must be listed");

        // Detail.
        mockMvc.perform(get("/v1/learner/live-classes/" + privateClass.getId())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/learner/live-classes/" + openClass.getId())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isOk());

        // Join.
        mockMvc.perform(post("/v1/live-session/join/" + privateClass.getId())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/live-session/join/" + openClass.getId())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isOk());

        // Download: same rule, same verdict.
        mockMvc.perform(get("/v1/live-session/classes/" + privateClass.getId() + "/recording/download")
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/live-session/classes/" + openClass.getId() + "/recording/download")
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isOk());

        // Replay: entitlement is decided the same way. (Playback then streams from storage,
        // which may legitimately be unavailable in tests — the assertion is the entitlement,
        // i.e. that access was not denied as an outsider.)
        mockMvc.perform(get("/v1/live-session/classes/" + openClass.getId() + "/recording/playback")
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertFalse(s == 403,
                            "an entitled learner must not be refused the open session's replay, got " + s);
                });

        // And an independent learner hits exactly the same matrix with no header at all.
        mockMvc.perform(get("/v1/learner/live-classes/" + privateClass.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/v1/live-session/join/" + privateClass.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/live-session/classes/" + privateClass.getId() + "/recording/download")
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/live-session/join/" + openClass.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/live-session/classes/" + openClass.getId() + "/recording/download")
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isOk());
    }

    // ── G9 ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void g9_existingInstitutionalWorkflowsRemainUnchanged() throws Exception {
        // A plain institution-scoped session of school A — unpublished, no open semantics.
        LiveClass institutional = seedClass("G9 institutional " + RUN,
                instA.getId(), teacherARow.getId(),
                "INSTITUTION", "DRAFT", null, "IN_PROGRESS");
        institutional.setRecordingUrl("https://cdn.test/g9-institutional-" + RUN + ".mp4");
        liveClassRepository.save(institutional);

        // Same-school learner joins exactly as before…
        mockMvc.perform(post("/v1/live-session/join/" + institutional.getId())
                        .header("Authorization", token(studentA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // …the tenant rule is still intact for everyone else…
        mockMvc.perform(post("/v1/live-session/join/" + institutional.getId())
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/live-session/classes/" + institutional.getId() + "/recording/download")
                        .header("Authorization", token(studentB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/live-session/join/" + institutional.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isForbidden());

        // …and the teacher's own institutional workflow is untouched when the header is sent.
        mockMvc.perform(get("/v1/teachers/me/live-classes")
                        .header("Authorization", token(teacherA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        String dashboard = mockMvc.perform(get("/v1/teachers/me/dashboard")
                        .header("Authorization", token(teacherA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(dashboard.contains(instA.getName()) == false,
                "affiliated teacher dashboard must still name their school");

        // School students keep their institutional dashboards.
        mockMvc.perform(get("/v1/student/dashboard/summary")
                        .header("Authorization", token(studentA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(instA.getName())));
    }

    // ── fixtures ───────────────────────────────────────────────────────────────────────────

    private LiveClass seedClass(String title, UUID institutionId, UUID teacherRowId,
                                String accessLevel, String publicationStatus,
                                UUID classGroupId, String status) {
        return liveClassRepository.save(LiveClass.builder()
                .teacherId(teacherRowId)
                .title(title)
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .durationMinutes(60)
                .status(status)
                .accessLevel(accessLevel)
                .publicationStatus(publicationStatus)
                .classGroupId(classGroupId)
                .institutionId(institutionId)
                .build());
    }

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("OLRN-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private User saveUser(String email, User.Role role, Institution institution) {
        User user = userRepository.save(newUser(email, role, institution.getId()));
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(institution.getId())
                .role(membershipRole(role))
                .isActive(true)
                .build());
        return user;
    }

    /** An unaffiliated account: no institution on the user and no membership row at all. */
    private User saveIndependentUser(String email, User.Role role) {
        return userRepository.save(newUser(email, role, null));
    }

    private User newUser(String email, User.Role role, UUID institutionId) {
        User user = User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("OpenLearn")
                .lastName("Tester")
                .role(role)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build();
        user.setInstitutionId(institutionId);
        return user;
    }

    private InstitutionMembership.Role membershipRole(User.Role role) {
        return switch (role) {
            case TEACHER -> InstitutionMembership.Role.TEACHER;
            case PARENT -> InstitutionMembership.Role.PARENT;
            case STUDENT, OTHER_LEARNER -> InstitutionMembership.Role.STUDENT;
            case INSTITUTION_ADMIN, ADMIN -> InstitutionMembership.Role.ADMIN;
            default -> InstitutionMembership.Role.STUDENT;
        };
    }

    private void saveStudentProfile(User user, String admissionNumber, Institution institution) {
        studentRepository.save(Student.builder()
                .userId(user.getId())
                .admissionNumber(admissionNumber)
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .institutionId(institution.getId())
                .build());
    }

    /** Seeds the academic chain (year → grade → term) and returns a class group inside it. */
    private ClassGroup saveClassGroup(Institution institution, String label) {
        AcademicYear year = academicYearRepository.save(AcademicYear.builder()
                .institutionId(institution.getId())
                .educationLevel(EducationLevel.SECONDARY)
                .yearLabel("2026")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .isCurrent(true)
                .isDeleted(false)
                .build());
        Grade grade = gradeRepository.save(Grade.builder()
                .institutionId(institution.getId())
                .educationLevel(EducationLevel.SECONDARY)
                .name("Form 1 " + label)
                .sortOrder(1)
                .isDeleted(false)
                .build());
        Term term = termRepository.save(Term.builder()
                .institutionId(institution.getId())
                .academicYearId(year.getId())
                .name("Term 1")
                .termNumber(1)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 4, 30))
                .isDeleted(false)
                .build());
        return classGroupRepository.save(ClassGroup.builder()
                .institutionId(institution.getId())
                .gradeId(grade.getId())
                .academicYearId(year.getId())
                .termId(term.getId())
                .name("Class " + label)
                .isDeleted(false)
                .build());
    }

    /** TestTokens emits the raw JWT; the filter only accepts {@code Authorization: Bearer <jwt>}. */
    private String token(User user) {
        return "Bearer " + TestTokens.userToken(user.getEmail());
    }
}
