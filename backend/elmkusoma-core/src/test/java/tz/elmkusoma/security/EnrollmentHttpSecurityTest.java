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
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.domain.TransferRecord;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.enrollment.repository.TransferRecordRepository;
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
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP security regression for the hardened enrollment routes
 * ({@code /v1/enrollments/**}) through the real filter chain:
 * JwtAuthenticationFilter → JwtRequestAttributeFilter →
 * OrganizationContextResolver → method security → EnrollmentServiceImpl gates →
 * GlobalExceptionHandler.
 *
 * <p>Two institutions (A/B) with cross-role fixtures; every mutation-denied
 * case re-reads the row to prove no write happened.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnrollmentHttpSecurityTest {

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
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TransferRecordRepository transferRecordRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ParentStudentLinkRepository parentStudentLinkRepository;

    private Institution instA;
    private Institution instB;

    private User teacherA;
    private User adminA;
    private User studentUserA;
    private User studentUserB;
    private User parentUserA;

    private Student profileA;
    private Student profileB;

    private Enrollment enrollA;
    private Enrollment enrollB;
    private Enrollment enrollTransfer;
    private Enrollment enrollHistory;

    @BeforeAll
    void createFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        instA = saveInstitution("Enroll Sec A " + run);
        instB = saveInstitution("Enroll Sec B " + run);

        teacherA = saveUser("enr-teacher-a-" + run + "@test.com", User.Role.TEACHER, instA);
        adminA = saveUser("enr-admin-a-" + run + "@test.com", User.Role.INSTITUTION_ADMIN, instA);
        studentUserA = saveUser("enr-student-a-" + run + "@test.com", User.Role.STUDENT, instA);
        studentUserB = saveUser("enr-student-b-" + run + "@test.com", User.Role.STUDENT, instB);
        parentUserA = saveUser("enr-parent-a-" + run + "@test.com", User.Role.PARENT, instA);

        profileA = saveStudent(studentUserA, instA, "ENR-A-" + run);
        profileB = saveStudent(studentUserB, instB, "ENR-B-" + run);

        // parentA is linked to the student in institution A only
        Parent parentA = parentRepository.save(Parent.builder()
                .userId(parentUserA.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .build());
        parentA.setInstitutionId(instA.getId());
        parentRepository.save(parentA);
        parentStudentLinkRepository.save(ParentStudentLink.builder()
                .parentId(parentA.getId())
                .studentId(profileA.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .isPrimary(true)
                .build());

        enrollA = saveEnrollment(profileA.getId(), instA.getId());
        enrollB = saveEnrollment(profileB.getId(), instB.getId());
        enrollTransfer = saveEnrollment(profileA.getId(), instA.getId());
        enrollHistory = saveEnrollment(profileA.getId(), instA.getId());
    }

    // ── fixtures ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("ENR-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private User saveUser(String email, User.Role role, Institution institution) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("Enroll")
                .lastName("Tester")
                .role(role)
                .institutionId(institution.getId())
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        InstitutionMembership.Role membershipRole = switch (role) {
            case TEACHER -> InstitutionMembership.Role.TEACHER;
            case STUDENT -> InstitutionMembership.Role.STUDENT;
            case PARENT -> InstitutionMembership.Role.PARENT;
            default -> InstitutionMembership.Role.ADMIN;
        };
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
                .enrollmentDate(LocalDate.now())
                .isDeleted(false)
                .build());
    }

    private Enrollment saveEnrollment(UUID studentId, UUID institutionId) {
        return enrollmentRepository.save(Enrollment.builder()
                .institutionId(institutionId)
                .studentId(studentId)
                .classGroupId(UUID.randomUUID())
                .academicYearId(UUID.randomUUID())
                .status(Enrollment.EnrollmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .isDeleted(false)
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + TestTokens.userToken(user.getEmail());
    }

    // ── student self-reads ──

    @Test
    void student_readsAnotherStudentsEnrollments_403() throws Exception {
        mockMvc.perform(get("/v1/enrollments/student/" + profileB.getId())
                        .header("Authorization", bearer(studentUserA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_readsOwnEnrollments_200() throws Exception {
        mockMvc.perform(get("/v1/enrollments/student/" + profileA.getId())
                        .header("Authorization", bearer(studentUserA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].studentId").value(profileA.getId().toString()));
    }

    // ── teacher institution scoping (filter, not deny) ──

    @Test
    void teacher_readsStudentEnrolledOnlyElsewhere_empty200() throws Exception {
        // EnrollmentServiceImpl filters by caller institution for staff roles
        // instead of denying: cross-institution reads return 200 with [].
        mockMvc.perform(get("/v1/enrollments/student/" + profileB.getId())
                        .header("Authorization", bearer(teacherA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void teacher_readsStudentEnrolledLocally_200() throws Exception {
        mockMvc.perform(get("/v1/enrollments/student/" + profileA.getId())
                        .header("Authorization", bearer(teacherA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].studentId").value(profileA.getId().toString()));
    }

    // ── parent link gate on the same endpoint ──

    @Test
    void parent_unlinked_readsEnrollments_403() throws Exception {
        User stranger = saveUser("enr-parent-stranger-" + UUID.randomUUID().toString().substring(0, 8)
                + "@test.com", User.Role.PARENT, instA);
        mockMvc.perform(get("/v1/enrollments/student/" + profileA.getId())
                        .header("Authorization", bearer(stranger)))
                .andExpect(status().isForbidden());
    }

    @Test
    void parent_linked_readsChildEnrollments_200_butNotOtherChild_403() throws Exception {
        mockMvc.perform(get("/v1/enrollments/student/" + profileA.getId())
                        .header("Authorization", bearer(parentUserA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/v1/enrollments/student/" + profileB.getId())
                        .header("Authorization", bearer(parentUserA)))
                .andExpect(status().isForbidden());
    }

    // ── cross-institution mutation ──

    @Test
    void institutionAdmin_updatesForeignEnrollmentStatus_403_andNoWrite() throws Exception {
        mockMvc.perform(put("/v1/enrollments/" + enrollB.getId() + "/status")
                        .param("status", "WITHDRAWN")
                        .header("Authorization", bearer(adminA)))
                .andExpect(status().isForbidden());

        Enrollment reloaded = enrollmentRepository.findById(enrollB.getId()).orElseThrow();
        assertEquals(Enrollment.EnrollmentStatus.ENROLLED, reloaded.getStatus());
    }

    // ── transfer attribution ignores the legacy header ──

    @Test
    void transfer_withForgedXUserId_attributedToJwtIdentity() throws Exception {
        UUID forgedUserId = UUID.randomUUID();
        UUID targetClass = UUID.randomUUID();

        var result = mockMvc.perform(post("/v1/enrollments/" + enrollTransfer.getId() + "/transfer")
                        .header("Authorization", bearer(adminA))
                        .header("X-Institution-Id", instA.getId().toString())
                        .header("X-User-Id", forgedUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toClassGroupId\":\"" + targetClass + "\",\"reason\":\"security probe\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String transferredBy = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.transferredBy");
        assertEquals(adminA.getId().toString(), transferredBy);
        assertNotEquals(forgedUserId.toString(), transferredBy);

        List<TransferRecord> persisted = transferRecordRepository
                .findByEnrollmentIdAndIsDeletedFalse(enrollTransfer.getId());
        assertTrue(persisted.stream().anyMatch(t -> adminA.getId().equals(t.getTransferredBy())));
        assertTrue(persisted.stream().noneMatch(t -> forgedUserId.equals(t.getTransferredBy())));
    }

    // ── transfer history ──

    @Test
    void getTransferHistory_unknownId_404() throws Exception {
        mockMvc.perform(get("/v1/enrollments/" + UUID.randomUUID() + "/transfers")
                        .header("Authorization", bearer(teacherA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransferHistory_afterTransfer_200() throws Exception {
        UUID targetClass = UUID.randomUUID();
        mockMvc.perform(post("/v1/enrollments/" + enrollHistory.getId() + "/transfer")
                        .header("Authorization", bearer(adminA))
                        .header("X-Institution-Id", instA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toClassGroupId\":\"" + targetClass + "\",\"reason\":\"history probe\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/enrollments/" + enrollHistory.getId() + "/transfers")
                        .header("Authorization", bearer(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].toClassGroupId").value(targetClass.toString()));
    }
}
