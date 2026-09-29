package tz.elmkusoma.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Shared class-membership rules: union of active student_class_assignments and
 * ENROLLED enrollments, users.id → students.id normalization, class audience
 * resolution. One implementation used by the learning AND assessment domains.
 */
@ExtendWith(MockitoExtension.class)
class ClassAccessGuardTest {

    @Mock private UserRepository userRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentClassAssignmentRepository studentClassAssignmentRepository;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private ClassAccessGuard guard;

    private UUID studentId;
    private UUID userId;
    private UUID classA;
    private UUID classB;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        classA = UUID.randomUUID();
        classB = UUID.randomUUID();
    }

    private Student student() {
        return Student.builder()
                .id(studentId)
                .userId(userId)
                .status(StudentStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    private StudentClassAssignment assignment(UUID classGroupId, boolean active) {
        return StudentClassAssignment.builder()
                .studentId(studentId)
                .classGroupId(classGroupId)
                .isActive(active)
                .isDeleted(false)
                .build();
    }

    private Enrollment enrollment(UUID classGroupId, Enrollment.EnrollmentStatus status) {
        return Enrollment.builder()
                .studentId(studentId)
                .classGroupId(classGroupId)
                .academicYearId(UUID.randomUUID())
                .status(status)
                .enrolledAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
    }

    // ── Membership enforcement ────────────────────────────────────────────────

    @Test
    void assertLearnerCanAccessClass_allowsOwnClass() {
        when(userRepository.findByEmailAndIsDeletedFalse("learner@test.com"))
                .thenReturn(Optional.of(User.builder().id(userId).build()));
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(student()));
        when(studentClassAssignmentRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of(assignment(classA, true)));

        assertDoesNotThrow(() -> guard.assertLearnerCanAccessClass("learner@test.com", classA));
    }

    @Test
    void assertLearnerCanAccessClass_blocksForeignClass() {
        when(userRepository.findByEmailAndIsDeletedFalse("learner@test.com"))
                .thenReturn(Optional.of(User.builder().id(userId).build()));
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(student()));
        when(studentClassAssignmentRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of(assignment(classA, true)));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> guard.assertLearnerCanAccessClass("learner@test.com", classB));
        assertTrue(ex.getMessage().contains("not a member"));
    }

    @Test
    void assertLearnerCanAccessClass_countsActiveEnrollmentsAsMembership() {
        when(userRepository.findByEmailAndIsDeletedFalse("learner@test.com"))
                .thenReturn(Optional.of(User.builder().id(userId).build()));
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(student()));
        // No term-based class assignments at all — membership comes from enrollments.
        when(studentClassAssignmentRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of());
        when(enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of(enrollment(classA, Enrollment.EnrollmentStatus.ENROLLED)));

        assertDoesNotThrow(() -> guard.assertLearnerCanAccessClass("learner@test.com", classA));
    }

    @Test
    void assertLearnerCanAccessClass_withdrawnEnrollmentGrantsNoMembership() {
        when(userRepository.findByEmailAndIsDeletedFalse("learner@test.com"))
                .thenReturn(Optional.of(User.builder().id(userId).build()));
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(student()));
        when(studentClassAssignmentRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of());
        when(enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of(enrollment(classA, Enrollment.EnrollmentStatus.WITHDRAWN)));

        // Withdrawn enrollment is not a membership → no modeled membership at all →
        // documented fallback allows the read (tenant without active membership data).
        assertDoesNotThrow(() -> guard.assertLearnerCanAccessClass("learner@test.com", classA));
    }

    @Test
    void assertLearnerCanAccessClass_skipsWhenUserHasNoStudentProfile() {
        when(userRepository.findByEmailAndIsDeletedFalse("teacher@test.com"))
                .thenReturn(Optional.of(User.builder().id(userId).build()));
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> guard.assertLearnerCanAccessClass("teacher@test.com", classA));
    }

    @Test
    void assertLearnerCanAccessClass_noOpWithoutEmail() {
        assertDoesNotThrow(() -> guard.assertLearnerCanAccessClass(null, classA));
        verifyNoInteractions(studentClassAssignmentRepository);
    }

    // ── users.id → students.id normalization ──────────────────────────────────

    @Test
    void resolveStudentId_acceptsStudentsIdDirectly() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));

        assertEquals(studentId, guard.resolveStudentId(studentId));
    }

    @Test
    void resolveStudentId_resolvesUsersIdThroughProfile() {
        when(studentRepository.findById(userId)).thenReturn(Optional.empty());
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(student()));

        assertEquals(studentId, guard.resolveStudentId(userId));
    }

    @Test
    void resolveStudentId_returnsNullWithoutProfile() {
        when(studentRepository.findById(any())).thenReturn(Optional.empty());
        when(studentRepository.findByUserIdAndIsDeletedFalse(any()))
                .thenReturn(Optional.empty());

        assertNull(guard.resolveStudentId(UUID.randomUUID()));
        assertNull(guard.resolveStudentId(null));
    }

    // ── Class audience / assignment ───────────────────────────────────────────

    @Test
    void resolveClassStudentIds_unionsAssignmentsAndEnrollments() {
        UUID otherStudentId = UUID.randomUUID();
        Student other = Student.builder()
                .id(otherStudentId).userId(UUID.randomUUID())
                .status(StudentStatus.ACTIVE).isDeleted(false).build();
        Enrollment otherEnrollment = Enrollment.builder()
                .studentId(otherStudentId)
                .classGroupId(classA)
                .academicYearId(UUID.randomUUID())
                .status(Enrollment.EnrollmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        when(studentClassAssignmentRepository.findByClassGroupIdAndIsDeletedFalse(classA))
                .thenReturn(List.of(assignment(classA, true)));
        when(enrollmentRepository.findByClassGroupIdAndIsDeletedFalse(classA))
                .thenReturn(List.of(otherEnrollment));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
        when(studentRepository.findById(otherStudentId)).thenReturn(Optional.of(other));

        List<UUID> studentIds = guard.resolveClassStudentIds(classA);

        // One student from class assignments + one from enrollments.
        assertEquals(2, studentIds.size());
        assertTrue(studentIds.contains(studentId));
        assertTrue(studentIds.contains(otherStudentId));
    }

    @Test
    void resolveClassGroupIdForStudent_prefersActiveClassAssignment() {
        when(studentClassAssignmentRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of(assignment(classA, true)));

        assertEquals(classA, guard.resolveClassGroupIdForStudent(studentId));
    }

    @Test
    void resolveClassGroupIdForStudent_fallsBackToEnrollment() {
        when(studentClassAssignmentRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of());
        when(enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of(enrollment(classB, Enrollment.EnrollmentStatus.ENROLLED)));

        assertEquals(classB, guard.resolveClassGroupIdForStudent(studentId));
    }

    @Test
    void resolveClassStudentUserIds_mapsProfilesToUserIds() {
        when(studentClassAssignmentRepository.findByClassGroupIdAndIsDeletedFalse(classA))
                .thenReturn(List.of(assignment(classA, true)));
        when(enrollmentRepository.findByClassGroupIdAndIsDeletedFalse(classA))
                .thenReturn(List.of());
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));

        List<UUID> userIds = guard.resolveClassStudentUserIds(classA);

        assertEquals(List.of(userId), userIds);
    }
}
