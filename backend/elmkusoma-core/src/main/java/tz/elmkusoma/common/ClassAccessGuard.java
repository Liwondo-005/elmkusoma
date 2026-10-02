package tz.elmkusoma.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared class-group access rules for the teaching &amp; learning domain.
 *
 * <p>Used by both {@code LearningServiceImpl} and {@code AssessmentServiceImpl} so the
 * rules stay identical everywhere:</p>
 * <ul>
 *   <li>learner class-membership enforcement for class-scoped reads (from both
 *       {@code student_class_assignments} and active {@code enrollments}),</li>
 *   <li>{@code users.id} to {@code students.id} normalization (the two ids are distinct),</li>
 *   <li>class audience/student resolution for notifications, listings and gradebooks.</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ClassAccessGuard {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentClassAssignmentRepository studentClassAssignmentRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Class-membership enforcement for learner reads.
     *
     * <p>Membership is the union of active {@code student_class_assignments} rows and
     * {@code enrollments} with status ENROLLED. Enforced only when the caller has a
     * student profile AND membership rows exist; when no membership data is modeled at
     * all the read is allowed (keeps existing flows working while blocking cross-class
     * reads in tenants that do model membership).</p>
     */
    public void assertLearnerCanAccessClass(String userEmail, UUID classGroupId) {
        if (userEmail == null || classGroupId == null) {
            return;
        }
        Student student = findStudentByUserEmail(userEmail);
        if (student == null) {
            return;
        }
        Set<UUID> memberClassIds = memberClassGroupIds(student.getId());
        if (memberClassIds.isEmpty()) {
            return;
        }
        if (!memberClassIds.contains(classGroupId)) {
            log.warn("Class membership denied: student {} is not a member of class {}",
                    student.getId(), classGroupId);
            throw new SecurityException("You are not a member of this class");
        }
    }

    /**
     * Strict membership check for content that is explicitly scoped to one class
     * (e.g. assignment-targeted resources).
     *
     * <p>Unlike {@link #assertLearnerCanAccessClass}, missing learner data is
     * never treated as implicit allow: without a student profile, or without
     * membership rows proving the learner belongs to this class, the answer is
     * {@code false}. The membership union is the same existing model used
     * everywhere else ({@code student_class_assignments} ∪ ENROLLED
     * {@code enrollments}) — no parallel system.</p>
     */
    public boolean isLearnerInClass(String userEmail, UUID classGroupId) {
        if (userEmail == null || classGroupId == null) {
            return false;
        }
        Student student = findStudentByUserEmail(userEmail);
        if (student == null) {
            return false;
        }
        return memberClassGroupIds(student.getId()).contains(classGroupId);
    }

    /**
     * Normalizes a caller-supplied identifier to a {@code students.id}.
     * Accepts either a students.id (returned as-is) or a users.id (resolved through the
     * student profile). Returns null when no student profile exists for the id.
     */
    public UUID resolveStudentId(UUID maybeStudentOrUserId) {
        if (maybeStudentOrUserId == null) {
            return null;
        }
        Student byPk = studentRepository.findById(maybeStudentOrUserId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElse(null);
        if (byPk != null) {
            return byPk.getId();
        }
        return studentRepository.findByUserIdAndIsDeletedFalse(maybeStudentOrUserId)
                .map(Student::getId)
                .orElse(null);
    }

    /** Student profile of the given user id; null when the user has no student profile. */
    public Student findStudentByUserId(UUID userId) {
        if (userId == null) {
            return null;
        }
        return studentRepository.findByUserIdAndIsDeletedFalse(userId).orElse(null);
    }

    /** Student profile of the given user email; null when the user has no student profile. */
    public Student findStudentByUserEmail(String email) {
        if (email == null) {
            return null;
        }
        User user = userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);
        if (user == null) {
            return null;
        }
        return findStudentByUserId(user.getId());
    }

    /**
     * The student's class group id: active {@code student_class_assignments} first,
     * falling back to an ENROLLED {@code enrollments} row. Null when unassigned.
     */
    public UUID resolveClassGroupIdForStudent(UUID studentId) {
        if (studentId == null) {
            return null;
        }
        return studentClassAssignmentRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .map(StudentClassAssignment::getClassGroupId)
                .findFirst()
                .orElseGet(() -> activeEnrollments(studentId)
                        .stream()
                        .map(Enrollment::getClassGroupId)
                        .findFirst()
                        .orElse(null));
    }

    /**
     * Ids of active students belonging to the given class group (union of active
     * class assignments and ENROLLED enrollments, deduplicated).
     */
    public List<UUID> resolveClassStudentIds(UUID classGroupId) {
        if (classGroupId == null) {
            return List.of();
        }
        Set<UUID> studentIds = new LinkedHashSet<>();
        for (StudentClassAssignment assignment
                : studentClassAssignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)) {
            if (Boolean.FALSE.equals(assignment.getIsActive())) {
                continue;
            }
            Student student = studentRepository.findById(assignment.getStudentId()).orElse(null);
            if (student != null && !Boolean.TRUE.equals(student.getIsDeleted())
                    && student.getStatus() == StudentStatus.ACTIVE) {
                studentIds.add(student.getId());
            }
        }
        for (Enrollment enrollment : enrollmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)) {
            if (enrollment.getStatus() != Enrollment.EnrollmentStatus.ENROLLED) {
                continue;
            }
            Student student = studentRepository.findById(enrollment.getStudentId()).orElse(null);
            if (student != null && !Boolean.TRUE.equals(student.getIsDeleted())
                    && student.getStatus() == StudentStatus.ACTIVE) {
                studentIds.add(student.getId());
            }
        }
        return new ArrayList<>(studentIds);
    }

    /**
     * User ids of active students assigned to the given class group (deduplicated).
     * Empty when the class group has no active student members.
     */
    public List<UUID> resolveClassStudentUserIds(UUID classGroupId) {
        Set<UUID> userIds = new LinkedHashSet<>();
        for (UUID studentId : resolveClassStudentIds(classGroupId)) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student != null && student.getUserId() != null) {
                userIds.add(student.getUserId());
            }
        }
        return new ArrayList<>(userIds);
    }

    /** Union of active class-assignment class groups and ENROLLED enrollment class groups. */
    private Set<UUID> memberClassGroupIds(UUID studentId) {
        Set<UUID> classGroupIds = new LinkedHashSet<>();
        for (StudentClassAssignment assignment
                : studentClassAssignmentRepository.findByStudentIdAndIsDeletedFalse(studentId)) {
            if (!Boolean.FALSE.equals(assignment.getIsActive()) && assignment.getClassGroupId() != null) {
                classGroupIds.add(assignment.getClassGroupId());
            }
        }
        for (Enrollment enrollment : activeEnrollments(studentId)) {
            if (enrollment.getClassGroupId() != null) {
                classGroupIds.add(enrollment.getClassGroupId());
            }
        }
        return classGroupIds;
    }

    private List<Enrollment> activeEnrollments(UUID studentId) {
        return enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream()
                .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ENROLLED)
                .toList();
    }
}
