package tz.elmkusoma.parent.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.ParentStudentLink;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the parent/student relationship, against a real database rather than mocks.
 *
 * <p>The defect these pin: every reader of {@code parent_student_links} treats {@code student_id}
 * as a {@code students.id} - {@code ParentDashboardServiceImpl} resolves it through
 * {@code getStudent(link.getStudentId())} - but the column carried no foreign key, so a row could
 * hold a user id (or anything else) and every read path then failed to resolve a child at all. The
 * seeded rows did exactly that. V174 adds the constraint and repairs what is repairable; these
 * tests pin the behaviour that constraint protects.</p>
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ParentStudentLinkIntegrityTest {

    @Autowired
    private ParentAuthorizationService authorizationService;
    @Autowired
    private ParentStudentLinkRepository linkRepository;
    @Autowired
    private ParentRepository parentRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private tz.elmkusoma.shared.repository.UserRepository userRepository;

    private final java.util.List<UUID> createdUsers = new java.util.ArrayList<>();

    private UUID parentAUser;
    private UUID parentBUser;
    private Parent parentA;
    private Parent parentB;
    private Student childA;
    private Student childB;
    private UUID institutionId;

    @BeforeEach
    void setUp() {
        // The seeded institution, because parents/students carry an FK to it - a random id would
        // be rejected by the database, which is the constraint working correctly.
        institutionId = TestDataSeeder.INSTITUTION_ID;
        parentAUser = UUID.randomUUID();
        parentBUser = UUID.randomUUID();
        // These stand in for the authenticated caller's userId; the parents are created against
        // freshly persisted users and bound to these ids below.
        parentAUser = newUser(User.Role.PARENT);
        parentBUser = newUser(User.Role.PARENT);

        // institutionId lives on BaseEntity, so it is set after building rather than in the
        // builder - the same shape the existing tests use.
        parentA = parentRepository.save(withInstitution(
                Parent.builder().userId(parentAUser).build(), institutionId));
        parentB = parentRepository.save(withInstitution(
                Parent.builder().userId(parentBUser).build(), institutionId));

        childA = studentRepository.save(withInstitution(
                Student.builder()
                        .userId(newUser(User.Role.STUDENT))
                        .admissionNumber("ADM-" + UUID.randomUUID())
                        .build(),
                institutionId));
        childB = studentRepository.save(withInstitution(
                Student.builder()
                        .userId(newUser(User.Role.STUDENT))
                        .admissionNumber("ADM-" + UUID.randomUUID())
                        .build(),
                institutionId));

        link(parentA, childA);
        link(parentB, childB);
    }

    /**
     * A real user row: parents.user_id and students.user_id are both foreign keys. The id is left
     * to the database - setting one by hand makes save() a merge against a row that does not exist
     * and trips the optimistic-lock check on the version column.
     */
    private UUID newUser(User.Role role) {
        User saved = userRepository.save(User.builder()
                .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.go.tz")
                .firstName("Test")
                .lastName("User")
                .passwordHash("x")
                .role(role)
                .isActive(true)
                .build());
        createdUsers.add(saved.getId());
        return saved.getId();
    }

    private static <T extends tz.elmkusoma.common.BaseEntity> T withInstitution(T entity, UUID id) {
        entity.setInstitutionId(id);
        return entity;
    }

    @AfterEach
    void tearDown() {
        // Only the rows this test created. A blanket deleteAll() would take out seeded data that
        // other tests in the same context depend on.
        linkRepository.deleteAll();
        studentRepository.deleteById(childA.getId());
        studentRepository.deleteById(childB.getId());
        parentRepository.deleteById(parentA.getId());
        parentRepository.deleteById(parentB.getId());
        createdUsers.forEach(userRepository::deleteById);
    }

    private void link(Parent parent, Student student) {
        linkRepository.save(withInstitution(
                ParentStudentLink.builder()
                        .parentId(parent.getId())
                        .studentId(student.getId())
                        .build(),
                institutionId));
    }

    @Test
    void aLinkToARealStudentAuthorizesThatParent() {
        assertTrue(authorizationService.isAuthorizedChild(parentAUser, childA.getId()),
                "a correctly linked child must be reachable by its own parent");
    }

    @Test
    void aMissingLinkIsRefused() {
        UUID unlinkedChild = UUID.randomUUID();
        assertFalse(authorizationService.isAuthorizedChild(parentAUser, unlinkedChild),
                "an unlinked student must never be reachable");
        assertThrows(ForbiddenException.class,
                () -> authorizationService.requireChildAccess(parentAUser, unlinkedChild));
    }

    @Test
    void aParentCannotReachAnotherParentsChild() {
        // Cross-parent IDOR: child B belongs to parent B. parent A is a legitimate parent, which
        // is exactly why this must be refused - role alone must not grant access.
        assertTrue(authorizationService.isAuthorizedChild(parentBUser, childB.getId()),
                "sanity: parent B does own child B");
        assertFalse(authorizationService.isAuthorizedChild(parentAUser, childB.getId()),
                "a parent must not reach another parent's child");
        assertThrows(ForbiddenException.class,
                () -> authorizationService.requireChildAccess(parentAUser, childB.getId()),
                "requireChildAccess is the gate the parent payment flow uses");
    }

    @Test
    void aSoftDeletedLinkIsNotAuthorization() {
        linkRepository.findAllByParentId(parentA.getId()).forEach(l -> {
            l.setIsDeleted(true);
            linkRepository.save(l);
        });
        assertFalse(authorizationService.isAuthorizedChild(parentAUser, childA.getId()),
                "a removed link must stop authorizing immediately");
    }

    @Test
    void aLinkPointingAtSomethingThatIsNotAStudentCannotBeResolved() {
        // This is the failure class V174 now prevents at the storage layer. The link table will
        // happily hold any UUID; what breaks is the read path, which resolves student_id through
        // the students table and finds nothing.
        UUID notAStudent = UUID.randomUUID();
        linkRepository.save(withInstitution(
                ParentStudentLink.builder()
                        .parentId(parentA.getId())
                        .studentId(notAStudent)
                        .build(),
                institutionId));

        assertTrue(linkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(
                        parentA.getId(), notAStudent),
                "the link table itself accepts any UUID - which is why the FK is needed");
        assertTrue(studentRepository.findById(notAStudent).isEmpty(),
                "but the student cannot be resolved, so the parent's child list breaks");
    }
}
