package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
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
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
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
 * HTTP regression for {@code GET /v1/my/children/{studentId}/live-classes} through
 * the real filter chain (JwtAuthenticationFilter → JwtRequestAttributeFilter →
 * OrganizationContextResolver → method security → GlobalExceptionHandler).
 *
 * <p>Contract under test:</p>
 * <ul>
 *   <li>the listing is scoped to the classes the child actually belongs to (union
 *       of active {@code student_class_assignments} and ENROLLED
 *       {@code enrollments}), not every class in the caller's institution;</li>
 *   <li>a linked child with no class membership yields an empty list;</li>
 *   <li>a parent that is not linked to the child gets 404, never data.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParentLiveClassesHttpSecurityTest {

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

    @Autowired
    private LiveClassRepository liveClassRepository;

    private Institution instA;

    private User parentLinked;
    private User parentStranger;

    private Student childWithClass;
    private Student childWithoutClass;

    private UUID childClassGroupId;

    @BeforeAll
    void createFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        instA = saveInstitution("ParLive-A " + run);

        parentLinked = saveUser("parlive-parent-l-" + run + "@test.com", User.Role.PARENT, instA);
        Parent linkedParent = saveParent(parentLinked, instA);
        parentStranger = saveUser("parlive-parent-s-" + run + "@test.com", User.Role.PARENT, instA);
        saveParent(parentStranger, instA);

        childWithClass = saveStudent("parlive-student-c-" + run + "@test.com", instA, "PARLIVE-C-" + run);
        childWithoutClass = saveStudent("parlive-student-n-" + run + "@test.com", instA, "PARLIVE-N-" + run);

        parentStudentLinkRepository.save(ParentStudentLink.builder()
                .parentId(linkedParent.getId())
                .studentId(childWithClass.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .isPrimary(true)
                .build());
        parentStudentLinkRepository.save(ParentStudentLink.builder()
                .parentId(linkedParent.getId())
                .studentId(childWithoutClass.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .isPrimary(false)
                .build());

        childClassGroupId = UUID.randomUUID();
        saveAssignment(childWithClass.getId(), childClassGroupId);

        saveLiveClass("Child class " + run, childClassGroupId, "SCHEDULED");
        saveLiveClass("Child class cancelled " + run, childClassGroupId, "CANCELLED");
        saveLiveClass("Other class " + run, UUID.randomUUID(), "SCHEDULED");
        saveLiveClass("Institution-wide " + run, null, "SCHEDULED");
    }

    // ── fixtures ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("PARLIVE-" + UUID.randomUUID().toString().substring(0, 8))
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
                .firstName("ParLive")
                .lastName("Tester")
                .role(role)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build();
        user.setInstitutionId(institution.getId());
        User saved = userRepository.save(user);
        membershipRepository.save(InstitutionMembership.builder()
                .userId(saved.getId())
                .institutionId(institution.getId())
                .role(role == User.Role.PARENT
                        ? InstitutionMembership.Role.PARENT
                        : InstitutionMembership.Role.STUDENT)
                .isActive(true)
                .build());
        return saved;
    }

    private Parent saveParent(User user, Institution institution) {
        Parent parent = Parent.builder()
                .userId(user.getId())
                .relationshipType(Parent.RelationshipType.GUARDIAN)
                .build();
        parent.setInstitutionId(institution.getId());
        return parentRepository.save(parent);
    }

    private Student saveStudent(String emailPrefix, Institution institution, String admissionPrefix) {
        User user = saveUser(emailPrefix + "@test.com", User.Role.STUDENT, institution);
        return studentRepository.save(Student.builder()
                .userId(user.getId())
                .admissionNumber(admissionPrefix + "-" + UUID.randomUUID().toString().substring(0, 8))
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .institutionId(institution.getId())
                .build());
    }

    private void saveAssignment(UUID studentId, UUID classGroupId) {
        assignmentRepository.save(StudentClassAssignment.builder()
                .institutionId(instA.getId())
                .studentId(studentId)
                .classGroupId(classGroupId)
                .academicYearId(UUID.randomUUID())
                .termId(UUID.randomUUID())
                .assignedDate(LocalDateTime.now())
                .isActive(true)
                .build());
    }

    private LiveClass saveLiveClass(String title, UUID classGroupId, String status) {
        return liveClassRepository.save(LiveClass.builder()
                .teacherId(UUID.randomUUID())
                .title(title)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .durationMinutes(60)
                .status(status)
                .classGroupId(classGroupId)
                .institutionId(instA.getId())
                .build());
    }

    private String token(User user) {
        return TestTokens.userToken(user.getEmail());
    }

    // ── class-scoped listing ──

    @Test
    void linkedParent_childWithClassGroup_seesOnlyThatGroupsClasses() throws Exception {
        // The institution also holds a class of another group and an open
        // (null classGroupId) session — neither may appear here.
        mockMvc.perform(get("/v1/my/children/" + childWithClass.getId() + "/live-classes")
                        .header("Authorization", "Bearer " + token(parentLinked)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data[0].durationMinutes").value(60));
    }

    @Test
    void linkedParent_childWithoutClassGroup_seesEmptyList() throws Exception {
        mockMvc.perform(get("/v1/my/children/" + childWithoutClass.getId() + "/live-classes")
                        .header("Authorization", "Bearer " + token(parentLinked)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ── IDOR ──

    @Test
    void unlinkedParent_readsChildLiveClasses_404() throws Exception {
        mockMvc.perform(get("/v1/my/children/" + childWithClass.getId() + "/live-classes")
                        .header("Authorization", "Bearer " + token(parentStranger)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}