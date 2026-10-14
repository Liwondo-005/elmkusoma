package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.ParentStudentLink;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP security matrix for the hardened certificate listing
 * ({@code GET /v1/certificates}) — through the real filter chain
 * (JwtAuthenticationFilter → JwtRequestAttributeFilter →
 * OrganizationContextResolver → method security → GlobalExceptionHandler).
 *
 * <p>Contract under test:</p>
 * <ul>
 *   <li>Unlinked parent → 403; parent linked to the child → 200 with the
 *       child's certificates.</li>
 *   <li>Cross-institution teacher → 403; own-institution teacher → 200 with
 *       unchanged payload; platform ADMIN → 200 (implicit bypass, same idiom
 *       as the transcripts sibling).</li>
 *   <li>Student reading another student's certificates → 403; own → 200
 *       (the users.id → students.id normalization fixes the old false-deny).</li>
 *   <li>The {@code studentId == null} institution branch keeps returning only
 *       the caller's institution.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CertificateListHttpSecurityTest {

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
    private ParentRepository parentRepository;

    @Autowired
    private ParentStudentLinkRepository parentStudentLinkRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    private Institution instA;
    private Institution instB;
    private User teacherA;
    private User teacherB;
    private User studentUserA;
    private User studentUserB;
    private User parentUnlinked;
    private User parentLinked;
    private User platformAdmin;
    private Student studentA;
    private Student studentB;

    @BeforeAll
    void createTwoInstitutionFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        instA = saveInstitution("CertList-A " + run);
        instB = saveInstitution("CertList-B " + run);

        teacherA = saveUser("cert-list-teacher-a-" + run + "@test.com", User.Role.TEACHER, instA);
        teacherB = saveUser("cert-list-teacher-b-" + run + "@test.com", User.Role.TEACHER, instB);
        studentUserA = saveUser("cert-list-student-a-" + run + "@test.com", User.Role.STUDENT, instA);
        studentUserB = saveUser("cert-list-student-b-" + run + "@test.com", User.Role.STUDENT, instB);
        parentUnlinked = saveUser("cert-list-parent-u-" + run + "@test.com", User.Role.PARENT, instA);
        saveParent(parentUnlinked, instA);
        parentLinked = saveUser("cert-list-parent-l-" + run + "@test.com", User.Role.PARENT, instA);
        Parent linkedParent = saveParent(parentLinked, instA);
        // platform ADMIN (global bypass role) homed in A
        platformAdmin = saveUser("cert-list-admin-" + run + "@test.com", User.Role.ADMIN, instA);

        studentA = saveStudent(studentUserA, instA, "CLIST-A-" + run);
        studentB = saveStudent(studentUserB, instB, "CLIST-B-" + run);
        parentStudentLinkRepository.save(ParentStudentLink.builder()
                .parentId(linkedParent.getId())
                .studentId(studentA.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .isPrimary(true)
                .build());

        saveCertificate(studentA, instA, "CLIST-" + run + "-A1");
        saveCertificate(studentB, instB, "CLIST-" + run + "-B1");
    }

    // ── fixtures ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("CLIST-" + UUID.randomUUID().toString().substring(0, 8))
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
                .firstName("CertList")
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

    private Student saveStudent(User user, Institution institution, String admissionNumber) {
        Student student = Student.builder()
                .userId(user.getId())
                .admissionNumber(admissionNumber)
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .institutionId(institution.getId())
                .build();
        return studentRepository.save(student);
    }

    private Parent saveParent(User user, Institution institution) {
        Parent parent = Parent.builder()
                .userId(user.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .build();
        parent.setInstitutionId(institution.getId());
        return parentRepository.save(parent);
    }

    private void saveCertificate(Student student, Institution institution, String serial) {
        Certificate certificate = Certificate.builder()
                .templateId(UUID.randomUUID())
                .studentId(student.getId())
                .issuedBy(UUID.randomUUID())
                .serialNumber(serial)
                .certificateType(Certificate.CertificateType.COMPLETION)
                .title("Cert list " + serial)
                .studentName("CertList Tester")
                .completionDate(LocalDate.now())
                .issueDate(LocalDateTime.now())
                .verificationCode("clv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .status(Certificate.CertificateStatus.ISSUED)
                .build();
        certificate.setInstitutionId(institution.getId());
        certificateRepository.save(certificate);
    }

    private String token(User user) {
        return TestTokens.userToken(user.getEmail());
    }

    // ── certificates list ──

    @Test
    void student_readsAnotherStudentsCertificates_403() throws Exception {
        mockMvc.perform(get("/v1/certificates")
                        .param("studentId", studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(studentUserA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error")
                        .value("Access denied: cannot view other students' certificates"));
    }

    @Test
    void student_readsOwnCertificates_200() throws Exception {
        mockMvc.perform(get("/v1/certificates")
                        .param("studentId", studentA.getId().toString())
                        .header("Authorization", "Bearer " + token(studentUserA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].studentId")
                        .value(studentA.getId().toString()));
    }

    @Test
    void parent_unlinkedToStudent_403() throws Exception {
        mockMvc.perform(get("/v1/certificates")
                        .param("studentId", studentA.getId().toString())
                        .header("Authorization", "Bearer " + token(parentUnlinked)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error")
                        .value("Access denied: cannot view certificates of students not linked to you"));
    }

    @Test
    void parent_linkedToChild_200() throws Exception {
        mockMvc.perform(get("/v1/certificates")
                        .param("studentId", studentA.getId().toString())
                        .header("Authorization", "Bearer " + token(parentLinked)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].studentId")
                        .value(studentA.getId().toString()));
    }

    @Test
    void teacher_crossInstitution_403() throws Exception {
        mockMvc.perform(get("/v1/certificates")
                        .param("studentId", studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(teacherA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error")
                        .value("Access denied: student belongs to another institution"));
    }

    @Test
    void teacher_sameInstitution_200() throws Exception {
        mockMvc.perform(get("/v1/certificates")
                        .param("studentId", studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(teacherB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].studentId")
                        .value(studentB.getId().toString()));
    }

    @Test
    void admin_globalBypass_200() throws Exception {
        mockMvc.perform(get("/v1/certificates")
                        .param("studentId", studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(platformAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @Test
    void institutionListBranch_returnsOnlyCallersInstitution_200() throws Exception {
        mockMvc.perform(get("/v1/certificates")
                        .header("Authorization", "Bearer " + token(teacherA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].studentId")
                        .value(studentA.getId().toString()));
    }
}
