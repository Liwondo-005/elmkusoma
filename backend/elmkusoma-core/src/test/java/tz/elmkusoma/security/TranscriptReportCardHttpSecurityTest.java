package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.academic.domain.AcademicYear;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.academic.domain.Term;
import tz.elmkusoma.academic.repository.AcademicYearRepository;
import tz.elmkusoma.academic.repository.TermRepository;
import tz.elmkusoma.certificate.domain.Transcript;
import tz.elmkusoma.certificate.repository.TranscriptRepository;
import tz.elmkusoma.grading.domain.GradingScale;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.repository.GradingScaleRepository;
import tz.elmkusoma.grading.repository.ReportCardRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP security matrix for the recently-hardened transcript and report-card
 * reads — through the real filter chain (JwtAuthenticationFilter →
 * JwtRequestAttributeFilter → OrganizationContextResolver → method security →
 * GlobalExceptionHandler).
 *
 * <p>Contract under test:</p>
 * <ul>
 *   <li>transcripts ({@code GET /v1/certificates/transcripts}): student reading
 *       another student's transcripts → 403, own → 200; unlinked parent → 403;
 *       cross-institution teacher → 403; platform ADMIN → 200.</li>
 *   <li>report cards ({@code GET /v1/grading/report-cards/...}): cross-institution
 *       reads → 403 on by-student, by-term, by-ids batch (incl. mixed foreign id)
 *       and by-id single; same-institution → 200 with unchanged payload;
 *       ADMIN → 200; unknown id → 404/empty (no existence oracle).</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TranscriptReportCardHttpSecurityTest {

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
    private TranscriptRepository transcriptRepository;

    @Autowired
    private ReportCardRepository reportCardRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private GradingScaleRepository gradingScaleRepository;

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
    private Term termB;
    private ReportCard cardB;

    @BeforeAll
    void createTwoInstitutionFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        instA = saveInstitution("SecTrans RC-A " + run);
        instB = saveInstitution("SecTrans RC-B " + run);

        teacherA = saveUser("sec-tr-teacher-a-" + run + "@test.com", User.Role.TEACHER, instA);
        teacherB = saveUser("sec-tr-teacher-b-" + run + "@test.com", User.Role.TEACHER, instB);
        studentUserA = saveUser("sec-tr-student-a-" + run + "@test.com", User.Role.STUDENT, instA);
        studentUserB = saveUser("sec-tr-student-b-" + run + "@test.com", User.Role.STUDENT, instB);
        parentUnlinked = saveUser("sec-tr-parent-u-" + run + "@test.com", User.Role.PARENT, instA);
        saveParent(parentUnlinked, instA);
        parentLinked = saveUser("sec-tr-parent-l-" + run + "@test.com", User.Role.PARENT, instA);
        Parent linkedParent = saveParent(parentLinked, instA);
        // platform ADMIN (global bypass role) homed in A
        platformAdmin = saveUser("sec-tr-admin-" + run + "@test.com", User.Role.ADMIN, instA);

        studentA = saveStudent(studentUserA, instA, "SECTR-A-" + run);
        studentB = saveStudent(studentUserB, instB, "SECTR-B-" + run);
        parentStudentLinkRepository.save(ParentStudentLink.builder()
                .parentId(linkedParent.getId())
                .studentId(studentA.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .isPrimary(true)
                .build());

        saveTranscript(studentA, instA, studentUserA, "SECTR-" + run + "-A1");
        saveTranscript(studentB, instB, studentUserB, "SECTR-" + run + "-B1");

        AcademicYear yearB = saveYear(instB, "2026-" + run);
        termB = saveTerm(yearB, instB, "Term 1 " + run);
        GradingScale scaleB = saveScale(instB, "Scale B " + run);
        cardB = reportCardRepository.save(ReportCard.builder()
                .studentId(studentB.getId())
                .academicYearId(yearB.getId())
                .termId(termB.getId())
                .gradingScaleId(scaleB.getId())
                .status(ReportCard.ReportCardStatus.DRAFT)
                .remarks("card-b")
                .institutionId(instB.getId())
                .build());

        AcademicYear yearA = saveYear(instA, "2026-" + run);
        Term termA = saveTerm(yearA, instA, "Term 1 " + run);
        GradingScale scaleA = saveScale(instA, "Scale A " + run);
        reportCardRepository.save(ReportCard.builder()
                .studentId(studentA.getId())
                .academicYearId(yearA.getId())
                .termId(termA.getId())
                .gradingScaleId(scaleA.getId())
                .status(ReportCard.ReportCardStatus.DRAFT)
                .remarks("card-a")
                .institutionId(instA.getId())
                .build());
    }

    // ── fixtures ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("SECTR-" + UUID.randomUUID().toString().substring(0, 8))
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
                .firstName("SecTrans")
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

    private void saveTranscript(Student student, Institution institution, User issuedBy, String serial) {
        Transcript transcript = Transcript.builder()
                .studentId(student.getId())
                .issuedBy(issuedBy.getId())
                .serialNumber(serial)
                .academicYear("2026")
                .term("Term 1")
                .status(Transcript.TranscriptStatus.DRAFT)
                .generatedAt(LocalDateTime.now())
                .build();
        transcript.setInstitutionId(institution.getId());
        transcriptRepository.save(transcript);
    }

    private AcademicYear saveYear(Institution institution, String label) {
        return academicYearRepository.save(AcademicYear.builder()
                .educationLevel(EducationLevel.SECONDARY)
                .yearLabel(label)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusMonths(9))
                .isCurrent(true)
                .isActive(true)
                .institutionId(institution.getId())
                .build());
    }

    private Term saveTerm(AcademicYear year, Institution institution, String name) {
        return termRepository.save(Term.builder()
                .academicYearId(year.getId())
                .name(name)
                .termNumber(1)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusMonths(2))
                .isActive(true)
                .institutionId(institution.getId())
                .build());
    }

    private GradingScale saveScale(Institution institution, String name) {
        return gradingScaleRepository.save(GradingScale.builder()
                .institutionId(institution.getId())
                .name(name)
                .scaleType(GradingScale.ScaleType.PERCENTAGE)
                .isDefault(false)
                .isActive(true)
                .build());
    }

    private String token(User user) {
        return TestTokens.userToken(user.getEmail());
    }

    // ── transcripts ──

    @Test
    void student_readsAnotherStudentsTranscripts_403() throws Exception {
        mockMvc.perform(get("/v1/certificates/transcripts")
                        .param("studentId", studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(studentUserA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void student_readsOwnTranscripts_200() throws Exception {
        mockMvc.perform(get("/v1/certificates/transcripts")
                        .param("studentId", studentA.getId().toString())
                        .header("Authorization", "Bearer " + token(studentUserA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].studentId").value(studentA.getId().toString()));
    }

    @Test
    void parent_unlinkedToStudent_403() throws Exception {
        mockMvc.perform(get("/v1/certificates/transcripts")
                        .param("studentId", studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(parentUnlinked)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void parent_linkedToChild_200() throws Exception {
        mockMvc.perform(get("/v1/certificates/transcripts")
                        .param("studentId", studentA.getId().toString())
                        .header("Authorization", "Bearer " + token(parentLinked)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void teacher_crossInstitution_403() throws Exception {
        mockMvc.perform(get("/v1/certificates/transcripts")
                        .param("studentId", studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(teacherA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void teacher_sameInstitution_200() throws Exception {
        mockMvc.perform(get("/v1/certificates/transcripts")
                        .param("studentId", studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(teacherB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void admin_globalBypass_200() throws Exception {
        mockMvc.perform(get("/v1/certificates/transcripts")
                        .param("studentId", studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(platformAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // ── report cards: by-student ──

    @Test
    void reportCards_byStudent_crossInstitution_403() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/student/" + studentB.getId())
                        .header("Authorization", "Bearer " + token(teacherA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportCards_byStudent_sameInstitution_200_unchangedPayload() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/student/" + studentB.getId())
                        .header("Authorization", "Bearer " + token(teacherB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(cardB.getId().toString()))
                .andExpect(jsonPath("$.data[0].studentId").value(studentB.getId().toString()))
                .andExpect(jsonPath("$.data[0].status").value("DRAFT"));
    }

    @Test
    void reportCards_byStudent_adminBypass_200() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/student/" + studentB.getId())
                        .header("Authorization", "Bearer " + token(platformAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void reportCards_byStudent_unknownId_empty200_noOracle() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/student/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token(teacherB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ── report cards: by-term ──

    @Test
    void reportCards_byTerm_crossInstitution_403() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/term/" + termB.getId())
                        .header("Authorization", "Bearer " + token(teacherA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportCards_byTerm_sameInstitution_200_unchangedPayload() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/term/" + termB.getId())
                        .header("Authorization", "Bearer " + token(teacherB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(cardB.getId().toString()));
    }

    @Test
    void reportCards_byTerm_adminBypass_200() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/term/" + termB.getId())
                        .header("Authorization", "Bearer " + token(platformAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void reportCards_byTerm_unknownId_empty200_noOracle() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/term/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token(teacherB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ── report cards: by-ids batch ──

    @Test
    void reportCards_batch_mixedForeignId_403() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/for-students")
                        .param("ids", studentA.getId().toString(), studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(teacherA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportCards_batch_sameInstitution_200() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/for-students")
                        .param("ids", studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(teacherB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(cardB.getId().toString()));
    }

    @Test
    void reportCards_batch_adminBypass_200() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/for-students")
                        .param("ids", studentA.getId().toString(), studentB.getId().toString())
                        .header("Authorization", "Bearer " + token(platformAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    // ── report cards: by-id single ──

    @Test
    void reportCards_byId_crossInstitution_403() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/" + cardB.getId())
                        .header("Authorization", "Bearer " + token(teacherA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportCards_byId_sameInstitution_200_unchangedPayload() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/" + cardB.getId())
                        .header("Authorization", "Bearer " + token(teacherB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(cardB.getId().toString()))
                .andExpect(jsonPath("$.data.studentId").value(studentB.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.remarks").value("card-b"));
    }

    @Test
    void reportCards_byId_adminBypass_200() throws Exception {
        mockMvc.perform(get("/v1/grading/report-cards/" + cardB.getId())
                        .header("Authorization", "Bearer " + token(platformAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(cardB.getId().toString()));
    }

    @Test
    void reportCards_byId_unknownId_rejectedWithoutOracle() throws Exception {
        // No existence oracle: an unknown id must never succeed. The
        // common-package ResourceNotFoundException is mapped to 404.
        mockMvc.perform(get("/v1/grading/report-cards/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token(teacherB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
