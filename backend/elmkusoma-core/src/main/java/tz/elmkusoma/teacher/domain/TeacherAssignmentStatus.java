package tz.elmkusoma.teacher.domain;

/**
 * Lifecycle of a {@link TeacherAssignment}.
 *
 * <p>Reassignment ends the old assignment (ENDED) and creates a new one
 * (ACTIVE); history rows are preserved and never deleted.</p>
 */
public enum TeacherAssignmentStatus {
    ACTIVE,
    ENDED,
    CANCELLED
}
