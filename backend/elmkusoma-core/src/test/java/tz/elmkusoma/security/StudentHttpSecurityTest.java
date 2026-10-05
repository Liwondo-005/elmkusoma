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
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.ParentStudentLink;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP security regression for the student routes ({@code /v1/students/**})
 * through the real filter chain.
 *
 * <p>Note on the PARENT cases below: the method-level {@code @PreAuthorize} on
 * {@code StudentController.getStudent} admits ADMIN/INSTITUTION_ADMIN/TEACHER/
 * STUDENT but not PARENT, so even a correctly linked parent is denied at the
 * HTTP layer (the controller's PARENT branch is unreachable). The linked-parent
 * test therefore documents the current over-denial as 403; the working
 * linked-parent → 200 contract is pinned on the enrollment by-student endpoint
 * in {@link EnrollmentHttpSecurityTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudentHttpSecurityTest {

    private static final String FIXTURE_CITY = "FixtureCity";

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
    private StudentClassAssignmentRepository assignmentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ParentStudentLinkRepository parentStudentLinkRepository;

    private Institution instA;
    private Institution instB;

    private User teacherA;
    private User adminA;
    private User globalAdmin;
    private User studentUserA;
    private User studentUserB;
    private User parentUserA;
    private User parentStranger;

    private Student studentA;
    private Student studentB;

    @BeforeAll
    void createFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        instA = saveInstitution("Student Sec A " + run);
        instB = saveInstitution("Student Sec B " + run);

        teacherA = saveUser("stu-teacher-a-" + run + "@test.com", User.Role.TEACHER,
                instA, InstitutionMembership.Role.TEACHER);
        adminA = saveUser("stu-admin-a-" + run + "@test.com", User.Role.INSTITUTION_ADMIN,
                instA, InstitutionMembership.Role.ADMIN);
        globalAdmin = saveUser("stu-global-" + run + "@test.com", User.Role.ADMIN,
                instA, InstitutionMembership.Role.ADMIN);
        studentUserA = saveUser("stu-student-a-" + run + "@test.com", User.Role.STUDENT,
                instA, InstitutionMembership.Role.STUDENT);
        studentUserB = saveUser("stu-student-b-" + run + "@test.com", User.Role.STUDENT,
                instB, InstitutionMembership.Role.STUDENT);
        parentUserA = saveUser("stu-parent-a-" + run + "@test.com", User.Role.PARENT,
                instA, InstitutionMembership.Role.PARENT);
        parentStranger = saveUser("stu-parent-stranger-" + run + "@test.com", User.Role.PARENT,
                instA, InstitutionMembership.Role.PARENT);

        studentA = saveStudent(studentUserA, instA, "STU-A-" + run);
        studentB = saveStudent(studentUserB, instB, "STU-B-" + run);

        // parentUserA is linked to the student in institution A only;
        // parentStranger has a Parent profile but no links.
        Parent parentA = parentRepository.save(Parent.builder()
                .userId(parentUserA.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .build());
        parentA.setInstitutionId(instA.getId());
        parentRepository.save(parentA);
        parentStudentLinkRepository.save(ParentStudentLink.builder()
                .parentId(parentA.getId())
                .studentId(studentA.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .isPrimary(true)
                .build());

        Parent stranger = parentRepository.save(Parent.builder()
                .userId(parentStranger.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .build());
        stranger.setInstitutionId(instA.getId());
        parentRepository.save(stranger);
    }

    // ── fixtures ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("STU-" + UUID.randomUUID().toString().substring(0, 8))
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
                .firstName("Student")
                .lastName("Tester")
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

    private Student saveStudent(User user, Institution institution, String admissionPrefix) {
        return studentRepository.save(Student.builder()
                .institutionId(institution.getId())
                .userId(user.getId())
                .admissionNumber(admissionPrefix + "-" + UUID.randomUUID().toString().substring(0, 8))
                .status(StudentStatus.ACTIVE)
                .city(FIXTURE_CITY)
                .enrollmentDate(LocalDate.now())
                .isDeleted(false)
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + TestTokens.userToken(user.getEmail());
    }

    private String updateBody() {
        return "{\"institutionId\":\"" + instA.getId() + "\","
                + "\"userId\":\"" + studentUserB.getId() + "\","
                + "\"firstName\":\"Tampered\",\"lastName\":\"Attempt\","
                + "\"email\":\"tampered-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com\","
                + "\"city\":\"HackedCity\"}";
    }

    private String assignBody() {
        return "{\"classGroupId\":\"" + UUID.randomUUID() + "\","
                + "\"academicYearId\":\"" + UUID.randomUUID() + "\","
                + "\"termId\":\"" + UUID.randomUUID() + "\"}";
    }

    // ── teacher cross-institution ──

    @Test
    void teacher_getsForeignStudent_403() throws Exception {
        mockMvc.perform(get("/v1/students/" + studentB.getId())
                        .header("Authorization", bearer(teacherA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacher_getsLocalStudent_200() throws Exception {
        mockMvc.perform(get("/v1/students/" + studentA.getId())
                        .header("Authorization", bearer(teacherA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.admissionNumber").value(studentA.getAdmissionNumber()));
    }

    @Test
    void teacher_updatesForeignStudent_403_roleGate_andNoWrite() throws Exception {
        // PUT /v1/students/{id} admits ADMIN/INSTITUTION_ADMIN only, so a
        // teacher is rejected by method security before any write can happen.
        mockMvc.perform(put("/v1/students/" + studentB.getId())
                        .header("Authorization", bearer(teacherA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isForbidden());

        assertEquals(FIXTURE_CITY, studentRepository.findById(studentB.getId()).orElseThrow().getCity());
    }

    @Test
    void teacher_assignsForeignStudent_403_roleGate() throws Exception {
        mockMvc.perform(post("/v1/students/" + studentB.getId() + "/assign-class")
                        .header("Authorization", bearer(teacherA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody()))
                .andExpect(status().isForbidden());
    }

    // ── institution-admin cross-institution (service scope gate + no write) ──

    @Test
    void institutionAdmin_updatesForeignStudent_403_andNoWrite() throws Exception {
        mockMvc.perform(put("/v1/students/" + studentB.getId())
                        .header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isForbidden());

        assertEquals(FIXTURE_CITY, studentRepository.findById(studentB.getId()).orElseThrow().getCity());
    }

    @Test
    void institutionAdmin_assignsForeignStudent_403_andNothingPersisted() throws Exception {
        mockMvc.perform(post("/v1/students/" + studentB.getId() + "/assign-class")
                        .header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody()))
                .andExpect(status().isForbidden());

        assertTrue(assignmentRepository.findByStudentIdAndIsDeletedFalse(studentB.getId()).isEmpty());
    }

    // ── parent reads on the student detail endpoint ──

    @Test
    void parent_unlinked_readsStudent_403() throws Exception {
        mockMvc.perform(get("/v1/students/" + studentA.getId())
                        .header("Authorization", bearer(parentStranger)))
                .andExpect(status().isForbidden());
    }

    @Test
    void parent_linked_readsChild_200() throws Exception {
        // PARENT role is admitted to GET /v1/students/{id}; a linked parent
        // reads the own child's profile (resolved via Parent profile + link).
        mockMvc.perform(get("/v1/students/" + studentA.getId())
                        .header("Authorization", bearer(parentUserA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── admission-number lookup ──

    @Test
    void admissionLookup_crossInstitution_teacher_403() throws Exception {
        mockMvc.perform(get("/v1/students/admission/" + studentB.getAdmissionNumber())
                        .header("Authorization", bearer(teacherA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admissionLookup_sameInstitution_teacher_200() throws Exception {
        mockMvc.perform(get("/v1/students/admission/" + studentA.getAdmissionNumber())
                        .header("Authorization", bearer(teacherA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.admissionNumber").value(studentA.getAdmissionNumber()));
    }

    @Test
    void admissionLookup_crossInstitution_globalAdmin_200() throws Exception {
        mockMvc.perform(get("/v1/students/admission/" + studentB.getAdmissionNumber())
                        .header("Authorization", bearer(globalAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.admissionNumber").value(studentB.getAdmissionNumber()));
    }
}
