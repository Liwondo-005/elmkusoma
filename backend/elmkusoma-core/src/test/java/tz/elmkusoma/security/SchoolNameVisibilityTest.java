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
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.testutil.TestTokens;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * School-name visibility on the student and teacher dashboards through the
 * real filter chain.
 *
 * <p>Covers the full workflow the product promises: a student is assigned to a
 * school through the existing authorized admin workflow ({@code POST /v1/students}),
 * the teacher is onboarded the same way ({@code POST /v1/teachers}), and both
 * then authenticate as themselves and read their own dashboard/profile APIs,
 * which must carry that school's id and exact persisted name.
 *
 * <p>Isolation is pinned at both ends: a second school's student sees only their
 * own school, a foreign {@code X-Institution-Id} is refused before any data
 * returns, and a profile row without a school reports {@code null} rather than
 * fabricating a name from membership or account columns.
 *
 * <p>Fixtures are isolated (unique emails/codes per run) and created here — no
 * seeded dev/real student or teacher record is modified.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchoolNameVisibilityTest {

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

    private String run;
    private Institution schoolA;
    private Institution schoolB;

    private User adminA;
    private User adminB;
    private User studentAUser;
    private User studentBUser;
    private User studentNoSchoolUser;
    private User teacherAUser;

    @BeforeAll
    void createFixtures() {
        run = UUID.randomUUID().toString().substring(0, 8);

        schoolA = saveInstitution("School A " + run);
        schoolB = saveInstitution("School B " + run);

        adminA = saveUser("sch-admin-a-" + run + "@test.com", User.Role.INSTITUTION_ADMIN,
                schoolA, InstitutionMembership.Role.ADMIN);
        adminB = saveUser("sch-admin-b-" + run + "@test.com", User.Role.INSTITUTION_ADMIN,
                schoolB, InstitutionMembership.Role.ADMIN);
        studentAUser = saveUser("sch-student-a-" + run + "@test.com", User.Role.STUDENT,
                schoolA, InstitutionMembership.Role.STUDENT);
        studentBUser = saveUser("sch-student-b-" + run + "@test.com", User.Role.STUDENT,
                schoolB, InstitutionMembership.Role.STUDENT);
        studentNoSchoolUser = saveUser("sch-student-ns-" + run + "@test.com", User.Role.STUDENT,
                schoolA, InstitutionMembership.Role.STUDENT);
        teacherAUser = saveUser("sch-teacher-a-" + run + "@test.com", User.Role.TEACHER,
                schoolA, InstitutionMembership.Role.TEACHER);

        // Profile without a school: membership and users.institution_id both say
        // School A, but the students row - the actual student-to-school assignment -
        // carries none. The dashboard must report null instead of fabricating a name
        // from the account columns or the request context.
        studentRepository.save(Student.builder()
                .institutionId(null)
                .userId(studentNoSchoolUser.getId())
                .admissionNumber("SCH-NS-" + run + "-" + UUID.randomUUID().toString().substring(0, 8))
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .isDeleted(false)
                .build());
    }

    // ── fixtures (isolated; mirrors StudentHttpSecurityTest's helpers) ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("SCH-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private User saveUser(String email, User.Role role, Institution institution,
                          InstitutionMembership.Role membershipRole) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("School")
                .lastName("Visibility")
                .role(role)
                .institutionId(institution.getId())
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(institution.getId())
                .role(membershipRole)
                .isActive(true)
                .build());
        return user;
    }

    private String bearer(User user) {
        return "Bearer " + TestTokens.userToken(user.getEmail());
    }

    private void assignStudentViaExistingWorkflow(User admin, Institution school, User student)
            throws Exception {
        // Existing authorized workflow: institution admin creates the student profile;
        // the controller re-stamps the tenant from the caller's validated membership.
        String body = "{\"institutionId\":\"" + school.getId() + "\","
                + "\"userId\":\"" + student.getId() + "\","
                + "\"firstName\":\"School\",\"lastName\":\"Student\","
                + "\"email\":\"" + student.getEmail() + "\","
                + "\"city\":\"WardX\"}";
        mockMvc.perform(post("/v1/students")
                        .header("Authorization", bearer(admin))
                        .header("X-Institution-Id", school.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    // ── student workflow: assign -> persist -> authenticate -> API contains school name ──

    @Test
    void studentAssignedViaAdminWorkflow_dashboardReturnsThatSchoolsName() throws Exception {
        assignStudentViaExistingWorkflow(adminA, schoolA, studentAUser);

        mockMvc.perform(get("/v1/student/dashboard/summary")
                        .header("Authorization", bearer(studentAUser))
                        .header("X-Institution-Id", schoolA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.institutionId").value(schoolA.getId().toString()))
                .andExpect(jsonPath("$.data.institutionName").value(schoolA.getName()));
    }

    @Test
    void studentSummary_reflectsOwnPersistedSchool_notAnotherSchool() throws Exception {
        assignStudentViaExistingWorkflow(adminB, schoolB, studentBUser);

        mockMvc.perform(get("/v1/student/dashboard/summary")
                        .header("Authorization", bearer(studentBUser))
                        .header("X-Institution-Id", schoolB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.institutionId").value(schoolB.getId().toString()))
                .andExpect(jsonPath("$.data.institutionName").value(schoolB.getName()));
    }

    @Test
    void student_crossInstitutionHeader_isRefusedBeforeAnyDataReturns() throws Exception {
        mockMvc.perform(get("/v1/student/dashboard/summary")
                        .header("Authorization", bearer(studentAUser))
                        .header("X-Institution-Id", schoolB.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentProfileWithoutSchool_reportsNoSchool_insteadOfFabricatingAName() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/student/dashboard/summary")
                        .header("Authorization", bearer(studentNoSchoolUser))
                        .header("X-Institution-Id", schoolA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").exists())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        // The profile row carries no school, so the payload must carry no school id
        // or name at all — membership and account columns both point at School A, and
        // neither may leak in as a fabricated affiliation. (The API omits null entries;
        // either encoding - omitted or explicit null - satisfies these checks.)
        assertFalse(body.contains("\"institutionId\""),
                "summary must not report a school id when the profile has none: " + body);
        assertFalse(body.contains("\"institutionName\""),
                "summary must not report a school name when the profile has none: " + body);
        assertFalse(body.contains(schoolA.getName()),
                "summary must not fabricate the account's school name: " + body);
    }

    // ── teacher workflow: onboard -> persist -> authenticate -> API contains school name ──

    @Test
    void teacherOnboardedViaAdminWorkflow_dashboardAndProfileReturnThatSchoolsName() throws Exception {
        String body = "{\"userId\":\"" + teacherAUser.getId() + "\","
                + "\"employeeNumber\":\"EMP-SCH-" + run + "\","
                + "\"specialization\":\"Mathematics\"}";
        mockMvc.perform(post("/v1/teachers")
                        .header("Authorization", bearer(adminA))
                        .header("X-Institution-Id", schoolA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.institutionId").value(schoolA.getId().toString()))
                .andExpect(jsonPath("$.data.institutionName").value(schoolA.getName()));

        mockMvc.perform(get("/v1/teachers/me/dashboard")
                        .header("Authorization", bearer(teacherAUser))
                        .header("X-Institution-Id", schoolA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.institutionId").value(schoolA.getId().toString()))
                .andExpect(jsonPath("$.data.institutionName").value(schoolA.getName()));

        mockMvc.perform(get("/v1/teachers/me/profile")
                        .header("Authorization", bearer(teacherAUser))
                        .header("X-Institution-Id", schoolA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.institutionId").value(schoolA.getId().toString()))
                .andExpect(jsonPath("$.data.institutionName").value(schoolA.getName()));
    }

    @Test
    void teacher_crossInstitutionHeader_isRefusedBeforeAnyDataReturns() throws Exception {
        mockMvc.perform(get("/v1/teachers/me/dashboard")
                        .header("Authorization", bearer(teacherAUser))
                        .header("X-Institution-Id", schoolB.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ── session payload carries the same server-resolved name ──

    @Test
    void authMe_exposesInstitutionNameFromPersistedRelationship() throws Exception {
        mockMvc.perform(get("/v1/auth/me")
                        .header("Authorization", bearer(studentAUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.institutionId").value(schoolA.getId().toString()))
                .andExpect(jsonPath("$.data.institutionName").value(schoolA.getName()));
    }
}
