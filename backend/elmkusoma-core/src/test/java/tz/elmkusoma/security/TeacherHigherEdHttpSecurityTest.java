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
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.highereducation.domain.Competency;
import tz.elmkusoma.highereducation.domain.CompetencyRecord;
import tz.elmkusoma.highereducation.domain.CompetencyStatus;
import tz.elmkusoma.highereducation.domain.CompetencyType;
import tz.elmkusoma.highereducation.domain.Department;
import tz.elmkusoma.highereducation.domain.Portfolio;
import tz.elmkusoma.highereducation.domain.Programme;
import tz.elmkusoma.highereducation.domain.ProgrammeType;
import tz.elmkusoma.highereducation.domain.Visibility;
import tz.elmkusoma.highereducation.repository.CompetencyRecordRepository;
import tz.elmkusoma.highereducation.repository.CompetencyRepository;
import tz.elmkusoma.highereducation.repository.DepartmentRepository;
import tz.elmkusoma.highereducation.repository.PortfolioItemRepository;
import tz.elmkusoma.highereducation.repository.PortfolioRepository;
import tz.elmkusoma.highereducation.repository.ProgrammeRepository;
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
import tz.elmkusoma.teacher.repository.TeacherQualificationRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP security matrix for the recently-hardened teacher and higher-education
 * routes — through the real filter chain (JwtAuthenticationFilter →
 * JwtRequestAttributeFilter → OrganizationContextResolver → method security →
 * GlobalExceptionHandler).
 *
 * <p>Contract under test (cross-institution/foreign-owner → 403 with no write on
 * denied mutations; same-institution/owner → 2xx; ADMIN bypass → 2xx):</p>
 * <ul>
 *   <li>teacher qualifications ({@code POST/GET /v1/teachers/{id}/qualifications}),</li>
 *   <li>live-class participants/stats
 *       ({@code GET /v1/teachers/me/live-classes/{id}/participants|/stats}),</li>
 *   <li>higher-ed programme/department get/put/delete,</li>
 *   <li>competency record PUT,</li>
 *   <li>higher-ed dashboard/{studentId},</li>
 *   <li>portfolio POST items.</li>
 * </ul>
 *
 * <p>Known scope limits (verified by reading the controllers, reported
 * separately): {@code TeacherLiveClassController} is {@code hasRole('TEACHER')}
 * and the higher-ed dashboard is {@code hasAnyRole('STUDENT','OTHER_LEARNER')},
 * so platform ADMIN cannot reach those two surfaces (403 by role gate) — no
 * ADMIN test is written for them.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeacherHigherEdHttpSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherQualificationRepository qualificationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private ProgrammeRepository programmeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CompetencyRepository competencyRepository;

    @Autowired
    private CompetencyRecordRepository competencyRecordRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    private Institution instA;
    private Institution instB;
    private User teacherUserA;
    private User teacherUserB;
    private User instAdminA;
    private User instAdminB;
    private User platformAdmin;
    private User studentUserA;
    private Teacher teacherRowA;
    private Teacher teacherRowB;
    private Student studentA;
    private Student studentB;
    private LiveClass classOwnedByA;
    private Programme progGetB;
    private Programme progPutB;
    private Programme progDelDenyB;
    private Programme progDelAllowB;
    private Programme progAdminDelB;
    private Department deptGetB;
    private Department deptPutB;
    private Department deptDelDenyB;
    private Department deptDelAllowB;
    private Department deptAdminDelB;
    private Competency competencyA;
    private Portfolio portfolioB;

    @BeforeAll
    void createTwoInstitutionFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        instA = saveInstitution("SecHed-A " + run);
        instB = saveInstitution("SecHed-B " + run);

        teacherUserA = saveUser("seched-teacher-a-" + run + "@test.com", User.Role.TEACHER, instA);
        teacherUserB = saveUser("seched-teacher-b-" + run + "@test.com", User.Role.TEACHER, instB);
        instAdminA = saveUser("seched-admin-a-" + run + "@test.com", User.Role.INSTITUTION_ADMIN, instA);
        instAdminB = saveUser("seched-admin-b-" + run + "@test.com", User.Role.INSTITUTION_ADMIN, instB);
        platformAdmin = saveUser("seched-admin-" + run + "@test.com", User.Role.ADMIN, instA);
        studentUserA = saveUser("seched-student-a-" + run + "@test.com", User.Role.STUDENT, instA);
        User studentUserB = saveUser("seched-student-b-" + run + "@test.com", User.Role.STUDENT, instB);

        teacherRowA = saveTeacher(teacherUserA, instA, "SECHED-TA-" + run);
        teacherRowB = saveTeacher(teacherUserB, instB, "SECHED-TB-" + run);
        studentA = saveStudent(studentUserA, instA, "SECHED-A-" + run);
        studentB = saveStudent(studentUserB, instB, "SECHED-B-" + run);

        classOwnedByA = liveClassRepository.save(LiveClass.builder()
                .teacherId(teacherRowA.getId())
                .title("SecHed owned class " + run)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .durationMinutes(60)
                .status("SCHEDULED")
                .institutionId(instA.getId())
                .build());

        progGetB = saveProgramme(instB, "SecHed ProgGet " + run);
        progPutB = saveProgramme(instB, "SecHed ProgPut " + run);
        progDelDenyB = saveProgramme(instB, "SecHed ProgDelDeny " + run);
        progDelAllowB = saveProgramme(instB, "SecHed ProgDelAllow " + run);
        progAdminDelB = saveProgramme(instB, "SecHed ProgAdminDel " + run);

        deptGetB = saveDepartment(instB, "SecHed DeptGet " + run);
        deptPutB = saveDepartment(instB, "SecHed DeptPut " + run);
        deptDelDenyB = saveDepartment(instB, "SecHed DeptDelDeny " + run);
        deptDelAllowB = saveDepartment(instB, "SecHed DeptDelAllow " + run);
        deptAdminDelB = saveDepartment(instB, "SecHed DeptAdminDel " + run);

        competencyA = competencyRepository.save(Competency.builder()
                .name("SecHed Competency " + run)
                .code("SEC-COMP-" + run)
                .competencyType(CompetencyType.SKILL)
                .isActive(true)
                .institutionId(instA.getId())
                .build());

        portfolioB = portfolioRepository.save(Portfolio.builder()
                .studentId(studentB.getId())
                .title("SecHed Portfolio " + run)
                .visibility(Visibility.PRIVATE)
                .isActive(true)
                .institutionId(instB.getId())
                .build());
    }

    // ── fixtures ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("SECHED-" + UUID.randomUUID().toString().substring(0, 8))
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
                .firstName("SecHed")
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

    private Teacher saveTeacher(User user, Institution institution, String employeeNumber) {
        Teacher teacher = Teacher.builder()
                .userId(user.getId())
                .employeeNumber(employeeNumber)
                .status(TeacherStatus.ACTIVE)
                .build();
        teacher.setInstitutionId(institution.getId());
        return teacherRepository.save(teacher);
    }

    private Student saveStudent(User user, Institution institution, String admissionNumber) {
        return studentRepository.save(Student.builder()
                .userId(user.getId())
                .admissionNumber(admissionNumber)
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .institutionId(institution.getId())
                .build());
    }

    private Programme saveProgramme(Institution institution, String name) {
        return programmeRepository.save(Programme.builder()
                .name(name)
                .code("SEC-P-" + UUID.randomUUID().toString().substring(0, 8))
                .programmeType(ProgrammeType.DEGREE)
                .educationLevel(EducationLevel.COLLEGE)
                .durationMonths(36)
                .isActive(true)
                .institutionId(institution.getId())
                .build());
    }

    private Department saveDepartment(Institution institution, String name) {
        return departmentRepository.save(Department.builder()
                .name(name)
                .code("SEC-D-" + UUID.randomUUID().toString().substring(0, 8))
                .isActive(true)
                .institutionId(institution.getId())
                .build());
    }

    private String token(User user) {
        return TestTokens.userToken(user.getEmail());
    }

    private String header(Institution institution) {
        return institution.getId().toString();
    }

    private String qualificationBody(String name) {
        return "{\"qualificationName\":\"" + name + "\","
                + "\"institutionName\":\"SecHed University\","
                + "\"fieldOfStudy\":\"Mathematics\","
                + "\"yearObtained\":2020,"
                + "\"certificateUrl\":\"https://files.test/sec-cert.pdf\"}";
    }

    private String programmeBody(String name) {
        return "{\"name\":\"" + name + "\","
                + "\"code\":\"SEC-PX\","
                + "\"description\":\"sec\","
                + "\"programmeType\":\"DEGREE\","
                + "\"educationLevel\":\"COLLEGE\","
                + "\"durationMonths\":36,"
                + "\"isActive\":true}";
    }

    private String departmentBody(String name) {
        return "{\"name\":\"" + name + "\","
                + "\"code\":\"SEC-DX\","
                + "\"description\":\"sec\","
                + "\"isActive\":true}";
    }

    // ── teacher qualifications ──

    @Test
    void qualification_post_crossTeacher_403_noWrite() throws Exception {
        int before = qualificationRepository.findAllByTeacherId(teacherRowB.getId()).size();

        mockMvc.perform(post("/v1/teachers/" + teacherRowB.getId() + "/qualifications")
                        .header("Authorization", "Bearer " + token(teacherUserA))
                        .header("X-Institution-Id", header(instA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qualificationBody("SecQual-X")))
                .andExpect(status().isForbidden());

        assertEquals(before, qualificationRepository.findAllByTeacherId(teacherRowB.getId()).size());
    }

    @Test
    void qualification_post_ownTeacher_201() throws Exception {
        mockMvc.perform(post("/v1/teachers/" + teacherRowB.getId() + "/qualifications")
                        .header("Authorization", "Bearer " + token(teacherUserB))
                        .header("X-Institution-Id", header(instB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qualificationBody("SecQual-B")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teacherId").value(teacherRowB.getId().toString()))
                .andExpect(jsonPath("$.data.qualificationName").value("SecQual-B"));
    }

    @Test
    void qualification_get_crossTeacher_403() throws Exception {
        mockMvc.perform(get("/v1/teachers/" + teacherRowB.getId() + "/qualifications")
                        .header("Authorization", "Bearer " + token(teacherUserA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void qualification_get_ownTeacher_200() throws Exception {
        mockMvc.perform(get("/v1/teachers/" + teacherRowB.getId() + "/qualifications")
                        .header("Authorization", "Bearer " + token(teacherUserB))
                        .header("X-Institution-Id", header(instB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void qualification_admin_bypass_200_and_201() throws Exception {
        mockMvc.perform(get("/v1/teachers/" + teacherRowB.getId() + "/qualifications")
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/teachers/" + teacherRowB.getId() + "/qualifications")
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .header("X-Institution-Id", header(instA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qualificationBody("SecQual-Admin")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.teacherId").value(teacherRowB.getId().toString()));
    }

    // ── live-class participants / stats (teacher-owned surface) ──

    @Test
    void liveClass_participants_crossTeacher_403() throws Exception {
        mockMvc.perform(get("/v1/teachers/me/live-classes/" + classOwnedByA.getId() + "/participants")
                        .header("Authorization", "Bearer " + token(teacherUserB))
                        .header("X-Institution-Id", header(instB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void liveClass_participants_owner_200() throws Exception {
        mockMvc.perform(get("/v1/teachers/me/live-classes/" + classOwnedByA.getId() + "/participants")
                        .header("Authorization", "Bearer " + token(teacherUserA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void liveClass_stats_crossTeacher_403() throws Exception {
        mockMvc.perform(get("/v1/teachers/me/live-classes/" + classOwnedByA.getId() + "/stats")
                        .header("Authorization", "Bearer " + token(teacherUserB))
                        .header("X-Institution-Id", header(instB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void liveClass_stats_owner_200() throws Exception {
        mockMvc.perform(get("/v1/teachers/me/live-classes/" + classOwnedByA.getId() + "/stats")
                        .header("Authorization", "Bearer " + token(teacherUserA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalJoined").value(0));
    }

    // ── programme get/put/delete ──

    @Test
    void programme_get_crossInstitution_403() throws Exception {
        mockMvc.perform(get("/api/v1/education/programmes/" + progGetB.getId())
                        .header("Authorization", "Bearer " + token(instAdminA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void programme_get_sameInstitution_200() throws Exception {
        mockMvc.perform(get("/api/v1/education/programmes/" + progGetB.getId())
                        .header("Authorization", "Bearer " + token(instAdminB))
                        .header("X-Institution-Id", header(instB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(progGetB.getId().toString()));
    }

    @Test
    void programme_get_adminBypass_200() throws Exception {
        mockMvc.perform(get("/api/v1/education/programmes/" + progGetB.getId())
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(progGetB.getId().toString()));
    }

    @Test
    void programme_put_crossInstitution_403_noWrite() throws Exception {
        String before = programmeRepository.findById(progPutB.getId()).orElseThrow().getName();

        mockMvc.perform(put("/api/v1/education/programmes/" + progPutB.getId())
                        .header("Authorization", "Bearer " + token(instAdminA))
                        .header("X-Institution-Id", header(instA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(programmeBody("SecHed Hijacked")))
                .andExpect(status().isForbidden());

        assertEquals(before, programmeRepository.findById(progPutB.getId()).orElseThrow().getName());
    }

    @Test
    void programme_put_sameInstitution_200() throws Exception {
        mockMvc.perform(put("/api/v1/education/programmes/" + progPutB.getId())
                        .header("Authorization", "Bearer " + token(instAdminB))
                        .header("X-Institution-Id", header(instB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(programmeBody("SecHed ProgPut Renamed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("SecHed ProgPut Renamed"));

        assertEquals("SecHed ProgPut Renamed",
                programmeRepository.findById(progPutB.getId()).orElseThrow().getName());
    }

    @Test
    void programme_put_adminBypass_200() throws Exception {
        mockMvc.perform(put("/api/v1/education/programmes/" + progGetB.getId())
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .header("X-Institution-Id", header(instA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(programmeBody("SecHed ProgGet Admin Renamed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("SecHed ProgGet Admin Renamed"));
    }

    @Test
    void programme_delete_crossInstitution_403_noWrite() throws Exception {
        mockMvc.perform(delete("/api/v1/education/programmes/" + progDelDenyB.getId())
                        .header("Authorization", "Bearer " + token(instAdminA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isForbidden());

        Programme stillThere = programmeRepository.findById(progDelDenyB.getId()).orElseThrow();
        assertFalse(stillThere.getIsDeleted());
    }

    @Test
    void programme_delete_sameInstitution_200() throws Exception {
        mockMvc.perform(delete("/api/v1/education/programmes/" + progDelAllowB.getId())
                        .header("Authorization", "Bearer " + token(instAdminB))
                        .header("X-Institution-Id", header(instB)))
                .andExpect(status().isOk());

        assertTrue(programmeRepository.findById(progDelAllowB.getId()).orElseThrow().getIsDeleted());
    }

    @Test
    void programme_delete_adminBypass_200() throws Exception {
        mockMvc.perform(delete("/api/v1/education/programmes/" + progAdminDelB.getId())
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isOk());

        assertTrue(programmeRepository.findById(progAdminDelB.getId()).orElseThrow().getIsDeleted());
    }

    // ── department get/put/delete ──

    @Test
    void department_get_crossInstitution_403() throws Exception {
        mockMvc.perform(get("/api/v1/education/departments/" + deptGetB.getId())
                        .header("Authorization", "Bearer " + token(instAdminA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void department_get_sameInstitution_200() throws Exception {
        mockMvc.perform(get("/api/v1/education/departments/" + deptGetB.getId())
                        .header("Authorization", "Bearer " + token(instAdminB))
                        .header("X-Institution-Id", header(instB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(deptGetB.getId().toString()));
    }

    @Test
    void department_get_adminBypass_200() throws Exception {
        mockMvc.perform(get("/api/v1/education/departments/" + deptGetB.getId())
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isOk());
    }

    @Test
    void department_put_crossInstitution_403_noWrite() throws Exception {
        String before = departmentRepository.findById(deptPutB.getId()).orElseThrow().getName();

        mockMvc.perform(put("/api/v1/education/departments/" + deptPutB.getId())
                        .header("Authorization", "Bearer " + token(instAdminA))
                        .header("X-Institution-Id", header(instA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("SecHed Hijacked")))
                .andExpect(status().isForbidden());

        assertEquals(before, departmentRepository.findById(deptPutB.getId()).orElseThrow().getName());
    }

    @Test
    void department_put_sameInstitution_200() throws Exception {
        mockMvc.perform(put("/api/v1/education/departments/" + deptPutB.getId())
                        .header("Authorization", "Bearer " + token(instAdminB))
                        .header("X-Institution-Id", header(instB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("SecHed DeptPut Renamed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("SecHed DeptPut Renamed"));

        assertEquals("SecHed DeptPut Renamed",
                departmentRepository.findById(deptPutB.getId()).orElseThrow().getName());
    }

    @Test
    void department_put_adminBypass_200() throws Exception {
        mockMvc.perform(put("/api/v1/education/departments/" + deptGetB.getId())
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .header("X-Institution-Id", header(instA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("SecHed DeptGet Admin Renamed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("SecHed DeptGet Admin Renamed"));
    }

    @Test
    void department_delete_crossInstitution_403_noWrite() throws Exception {
        mockMvc.perform(delete("/api/v1/education/departments/" + deptDelDenyB.getId())
                        .header("Authorization", "Bearer " + token(instAdminA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isForbidden());

        assertFalse(departmentRepository.findById(deptDelDenyB.getId()).orElseThrow().getIsDeleted());
    }

    @Test
    void department_delete_sameInstitution_200() throws Exception {
        mockMvc.perform(delete("/api/v1/education/departments/" + deptDelAllowB.getId())
                        .header("Authorization", "Bearer " + token(instAdminB))
                        .header("X-Institution-Id", header(instB)))
                .andExpect(status().isOk());

        assertTrue(departmentRepository.findById(deptDelAllowB.getId()).orElseThrow().getIsDeleted());
    }

    @Test
    void department_delete_adminBypass_200() throws Exception {
        mockMvc.perform(delete("/api/v1/education/departments/" + deptAdminDelB.getId())
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isOk());

        assertTrue(departmentRepository.findById(deptAdminDelB.getId()).orElseThrow().getIsDeleted());
    }

    // ── competency record PUT ──

    @Test
    void competencyRecord_put_crossInstitution_403_noWrite() throws Exception {
        Optional<CompetencyRecord> before = competencyRecordRepository
                .findByStudentIdAndCompetencyIdAndIsDeletedFalse(studentA.getId(), competencyA.getId());

        mockMvc.perform(put("/api/v1/education/competencies/student/" + studentA.getId()
                                + "/competency/" + competencyA.getId())
                        .param("status", "COMPETENT")
                        .param("evidence", "sec-hijack")
                        .header("Authorization", "Bearer " + token(instAdminB))
                        .header("X-Institution-Id", header(instB)))
                .andExpect(status().isForbidden());

        Optional<CompetencyRecord> after = competencyRecordRepository
                .findByStudentIdAndCompetencyIdAndIsDeletedFalse(studentA.getId(), competencyA.getId());
        assertEquals(before.map(r -> r.getStatus().name()).orElse(null),
                after.map(r -> r.getStatus().name()).orElse(null));
        assertEquals(before.map(CompetencyRecord::getEvidence).orElse(null),
                after.map(CompetencyRecord::getEvidence).orElse(null));
    }

    @Test
    void competencyRecord_put_ownerStudent_practicing_200() throws Exception {
        clearCompetencyRecord();

        mockMvc.perform(put("/api/v1/education/competencies/student/" + studentA.getId()
                                + "/competency/" + competencyA.getId())
                        .param("status", "PRACTICING")
                        .param("evidence", "sec-owner-evidence")
                        .header("Authorization", "Bearer " + token(studentUserA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        CompetencyRecord record = competencyRecordRepository
                .findByStudentIdAndCompetencyIdAndIsDeletedFalse(studentA.getId(), competencyA.getId())
                .orElseThrow();
        assertEquals(CompetencyStatus.PRACTICING, record.getStatus());
        assertEquals("sec-owner-evidence", record.getEvidence());
    }

    @Test
    void competencyRecord_put_ownerStudent_competent_403_noWrite() throws Exception {
        clearCompetencyRecord();

        mockMvc.perform(put("/api/v1/education/competencies/student/" + studentA.getId()
                                + "/competency/" + competencyA.getId())
                        .param("status", "COMPETENT")
                        .param("evidence", "sec-self-awarded")
                        .header("Authorization", "Bearer " + token(studentUserA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isForbidden());

        assertTrue(competencyRecordRepository
                .findByStudentIdAndCompetencyIdAndIsDeletedFalse(studentA.getId(), competencyA.getId())
                .isEmpty());
    }

    private void clearCompetencyRecord() {
        competencyRecordRepository
                .findByStudentIdAndCompetencyIdAndIsDeletedFalse(studentA.getId(), competencyA.getId())
                .ifPresent(competencyRecordRepository::delete);
    }

    @Test
    void competencyRecord_put_adminBypass_200() throws Exception {
        mockMvc.perform(put("/api/v1/education/competencies/student/" + studentA.getId()
                                + "/competency/" + competencyA.getId())
                        .param("status", "ASSESSED")
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(CompetencyStatus.ASSESSED, competencyRecordRepository
                .findByStudentIdAndCompetencyIdAndIsDeletedFalse(studentA.getId(), competencyA.getId())
                .orElseThrow().getStatus());
    }

    // ── portfolio POST items ──

    @Test
    void portfolioItem_post_crossInstitution_403_noWrite() throws Exception {
        long before = portfolioItemRepository.countByPortfolioIdAndIsDeletedFalse(portfolioB.getId());

        mockMvc.perform(post("/api/v1/education/portfolios/" + portfolioB.getId() + "/items")
                        .header("Authorization", "Bearer " + token(instAdminA))
                        .header("X-Institution-Id", header(instA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"SecHijack\",\"description\":\"x\","
                                + "\"itemType\":\"DOCUMENT\",\"fileUrl\":\"https://files.test/h.pdf\"}"))
                .andExpect(status().isForbidden());

        assertEquals(before, portfolioItemRepository.countByPortfolioIdAndIsDeletedFalse(portfolioB.getId()));
    }

    @Test
    void portfolioItem_post_sameInstitution_201() throws Exception {
        mockMvc.perform(post("/api/v1/education/portfolios/" + portfolioB.getId() + "/items")
                        .header("Authorization", "Bearer " + token(instAdminB))
                        .header("X-Institution-Id", header(instB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"SecItem-B\",\"description\":\"sec\","
                                + "\"itemType\":\"DOCUMENT\",\"fileUrl\":\"https://files.test/b.pdf\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("SecItem-B"));

        assertTrue(portfolioItemRepository.findByPortfolioIdAndIsDeletedFalse(portfolioB.getId())
                .stream().anyMatch(i -> "SecItem-B".equals(i.getTitle())));
    }

    @Test
    void portfolioItem_post_adminBypass_201() throws Exception {
        mockMvc.perform(post("/api/v1/education/portfolios/" + portfolioB.getId() + "/items")
                        .header("Authorization", "Bearer " + token(platformAdmin))
                        .header("X-Institution-Id", header(instA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"SecItem-Admin\",\"description\":\"sec\","
                                + "\"itemType\":\"DOCUMENT\",\"fileUrl\":\"https://files.test/a.pdf\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("SecItem-Admin"));
    }

    // ── higher-ed dashboard ──

    @Test
    void dashboard_ownerStudent_200() throws Exception {
        mockMvc.perform(get("/api/v1/education/higher-education/dashboard/" + studentA.getId())
                        .header("Authorization", "Bearer " + token(studentUserA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.academicContext").value("COLLEGE"));
    }

    @Test
    void dashboard_foreignStudent_403() throws Exception {
        mockMvc.perform(get("/api/v1/education/higher-education/dashboard/" + studentB.getId())
                        .header("Authorization", "Bearer " + token(studentUserA))
                        .header("X-Institution-Id", header(instA)))
                .andExpect(status().isForbidden());
    }
}
