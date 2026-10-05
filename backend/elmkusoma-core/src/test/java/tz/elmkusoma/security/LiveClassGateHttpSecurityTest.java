package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.repository.LiveClassIssueRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP security matrix for the recently-hardened live class-gate — through the
 * real filter chain (JwtAuthenticationFilter → JwtRequestAttributeFilter →
 * OrganizationContextResolver → method security → GlobalExceptionHandler).
 *
 * <p>Contract under test:</p>
 * <ul>
 *   <li>class-gated join ({@code POST /v1/live-session/join/{classId}}): a
 *       learner joining a class-scoped session (classGroupId set) of a class
 *       they do not belong to → 403; joining an open session (null group) of
 *       their own institution → 200.</li>
 *   <li>replay download ({@code GET /v1/live-session/classes/{id}/recording/download}):
 *       same class-gate matrix.</li>
 *   <li>reportIssue ({@code POST /v1/live-session/report/{classId}}) and calendar
 *       export ({@code GET /v1/live-session/calendar/{classId}/export}):
 *       cross-institution callers → 403.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LiveClassGateHttpSecurityTest {

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
    private LiveClassIssueRepository issueRepository;

    private Institution instA;
    private Institution instB;
    private User learnerA;
    private User learnerB;
    private LiveClass classScoped;
    private LiveClass classOpen;

    @BeforeAll
    void createTwoInstitutionFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        instA = saveInstitution("SecGate-A " + run);
        instB = saveInstitution("SecGate-B " + run);

        learnerA = saveUser("secgate-learner-a-" + run + "@test.com", User.Role.STUDENT, instA);
        learnerB = saveUser("secgate-learner-b-" + run + "@test.com", User.Role.STUDENT, instB);
        // learnerA is an institution member with a student profile but NO class
        // assignments — exactly "another class" for the class-scoped session
        studentRepository.save(Student.builder()
                .userId(learnerA.getId())
                .admissionNumber("SECGATE-A-" + run)
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .institutionId(instA.getId())
                .build());

        User teacherUserA = saveUser("secgate-teacher-a-" + run + "@test.com", User.Role.TEACHER, instA);
        Teacher teacherRowA = Teacher.builder()
                .userId(teacherUserA.getId())
                .employeeNumber("SECGATE-TA-" + run)
                .status(TeacherStatus.ACTIVE)
                .build();
        teacherRowA.setInstitutionId(instA.getId());
        teacherRowA = teacherRepository.save(teacherRowA);

        // class-scoped session: bound to a class group the learner is not in
        classScoped = liveClassRepository.save(LiveClass.builder()
                .teacherId(teacherRowA.getId())
                .title("SecGate scoped " + run)
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .durationMinutes(60)
                .status("IN_PROGRESS")
                .classGroupId(UUID.randomUUID())
                .recordingUrl("https://cdn.test/secgate-scoped-" + run + ".mp4")
                .institutionId(instA.getId())
                .build());

        // open session: no class group → any institution member may join
        classOpen = liveClassRepository.save(LiveClass.builder()
                .teacherId(teacherRowA.getId())
                .title("SecGate open " + run)
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .durationMinutes(60)
                .status("IN_PROGRESS")
                .classGroupId(null)
                .recordingUrl("https://cdn.test/secgate-open-" + run + ".mp4")
                .institutionId(instA.getId())
                .build());
    }

    // ── fixtures ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("SECGATE-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private User saveUser(String email, User.Role role, Institution institution) {
        User user = User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("SecGate")
                .lastName("Tester")
                .role(role)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build();
        user.setInstitutionId(institution.getId());
        User saved = userRepository.save(user);
        InstitutionMembership.Role membershipRole = switch (role) {
            case TEACHER -> InstitutionMembership.Role.TEACHER;
            case PARENT -> InstitutionMembership.Role.PARENT;
            case STUDENT, OTHER_LEARNER -> InstitutionMembership.Role.STUDENT;
            case INSTITUTION_ADMIN, ADMIN -> InstitutionMembership.Role.ADMIN;
            default -> InstitutionMembership.Role.STUDENT;
        };
        membershipRepository.save(InstitutionMembership.builder()
                .userId(saved.getId())
                .institutionId(institution.getId())
                .role(membershipRole)
                .isActive(true)
                .build());
        return saved;
    }

    private String token(User user) {
        return TestTokens.userToken(user.getEmail());
    }

    // ── join gate ──

    @Test
    void join_classScopedSessionOfAnotherClass_403() throws Exception {
        mockMvc.perform(post("/v1/live-session/join/" + classScoped.getId())
                        .header("Authorization", "Bearer " + token(learnerA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void join_openSessionSameInstitution_200() throws Exception {
        mockMvc.perform(post("/v1/live-session/join/" + classOpen.getId())
                        .header("Authorization", "Bearer " + token(learnerA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roomName").exists());
    }

    @Test
    void join_crossInstitutionSession_403() throws Exception {
        mockMvc.perform(post("/v1/live-session/join/" + classOpen.getId())
                        .header("Authorization", "Bearer " + token(learnerB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ── replay download: same matrix ──

    @Test
    void recordingDownload_classScopedSessionOfAnotherClass_403() throws Exception {
        mockMvc.perform(get("/v1/live-session/classes/" + classScoped.getId() + "/recording/download")
                        .header("Authorization", "Bearer " + token(learnerA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void recordingDownload_openSessionSameInstitution_200() throws Exception {
        mockMvc.perform(get("/v1/live-session/classes/" + classOpen.getId() + "/recording/download")
                        .header("Authorization", "Bearer " + token(learnerA))
                        .header("X-Institution-Id", instA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.downloadUrl").value(classOpen.getRecordingUrl()));
    }

    @Test
    void recordingDownload_crossInstitution_403() throws Exception {
        mockMvc.perform(get("/v1/live-session/classes/" + classOpen.getId() + "/recording/download")
                        .header("Authorization", "Bearer " + token(learnerB))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ── reportIssue ──

    @Test
    void reportIssue_crossInstitution_403_noWrite() throws Exception {
        int before = issueRepository
                .findByLiveClassIdAndIsDeletedFalseOrderByCreatedAtDesc(classOpen.getId()).size();

        mockMvc.perform(post("/v1/live-session/report/" + classOpen.getId())
                        .header("Authorization", "Bearer " + token(learnerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issueType\":\"AUDIO\",\"description\":\"sec cross\","
                                + "\"severity\":\"HIGH\"}"))
                .andExpect(status().isForbidden());

        assertEquals(before, issueRepository
                .findByLiveClassIdAndIsDeletedFalseOrderByCreatedAtDesc(classOpen.getId()).size());
    }

    @Test
    void reportIssue_sameInstitution_200() throws Exception {
        mockMvc.perform(post("/v1/live-session/report/" + classOpen.getId())
                        .header("Authorization", "Bearer " + token(learnerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issueType\":\"AUDIO\",\"description\":\"sec same\","
                                + "\"severity\":\"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── calendar export ──

    @Test
    void calendarExport_crossInstitution_403() throws Exception {
        mockMvc.perform(get("/v1/live-session/calendar/" + classOpen.getId() + "/export")
                        .header("Authorization", "Bearer " + token(learnerB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void calendarExport_sameInstitution_200() throws Exception {
        mockMvc.perform(get("/v1/live-session/calendar/" + classOpen.getId() + "/export")
                        .header("Authorization", "Bearer " + token(learnerA)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("BEGIN:VCALENDAR")));
    }
}
