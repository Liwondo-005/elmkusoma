package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.highereducation.domain.Programme;
import tz.elmkusoma.highereducation.domain.StudentCourseEnrollment;
import tz.elmkusoma.highereducation.repository.ProgrammeRepository;
import tz.elmkusoma.highereducation.repository.StudentCourseEnrollmentRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.util.Objects;
import java.util.UUID;

/**
 * Server-authoritative identity for the College/TVET higher-education surface.
 * Learner callers always act on their own student row: the {studentId} path
 * parameter may carry the auth user id (legacy frontend behaviour) or the
 * students.id — anything else is refused, so a foreign id never widens access.
 */
@Service
@RequiredArgsConstructor
public class HighEdIdentity {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StudentCourseEnrollmentRepository enrollmentRepository;
    private final ProgrammeRepository programmeRepository;

    public static boolean isLearner(String userRole) {
        return "STUDENT".equals(userRole) || "OTHER_LEARNER".equals(userRole);
    }

    /** Learners: always their own students.id. Staff/parents: the requested id. */
    public UUID resolveStudentId(UUID callerUserId, String userRole, UUID requestedStudentId) {
        if (!isLearner(userRole)) {
            return requestedStudentId;
        }
        UUID ownStudentId = studentRepository.findByUserIdAndIsDeletedFalse(callerUserId)
                .map(Student::getId)
                .orElse(null);
        if (ownStudentId == null) {
            // Profile-less learner (no students row yet): only their own auth
            // user id is acceptable — anything else is another learner's id.
            if (requestedStudentId != null && requestedStudentId.equals(callerUserId)) {
                return requestedStudentId;
            }
            throw new ForbiddenException("Student profile", "access");
        }
        if (requestedStudentId != null
                && !requestedStudentId.equals(ownStudentId)
                && !requestedStudentId.equals(callerUserId)) {
            throw new ForbiddenException("Student profile", "access");
        }
        return ownStudentId;
    }

    /** Fails when the student row exists but belongs to another institution. */
    public void assertStudentInInstitution(UUID studentId, UUID serverInstitutionId) {
        if (serverInstitutionId == null || studentId == null) {
            return;
        }
        studentRepository.findById(studentId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .ifPresent(s -> {
                    if (s.getInstitutionId() == null || !s.getInstitutionId().equals(serverInstitutionId)) {
                        throw new ForbiddenException("Student profile", "access");
                    }
                });
    }

    /**
     * College/TVET context for the shared higher-education dashboard — derived
     * from the academic profile, never trusted from the client. Order:
     * users.learningLevel, then the education level of the learner's programme
     * enrolments, then the caller's request fallback, then COLLEGE.
     */
    public String resolveLearningLevel(UUID callerUserId, UUID studentId, String fallback) {
        if (callerUserId != null) {
            User user = userRepository.findByIdAndIsDeletedFalse(callerUserId).orElse(null);
            if (user != null && user.getLearningLevel() != null) {
                return user.getLearningLevel().name();
            }
        }
        if (studentId != null) {
            UUID programmeId = enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                    .map(StudentCourseEnrollment::getProgrammeId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (programmeId != null) {
                Programme programme = programmeRepository.findById(programmeId).orElse(null);
                if (programme != null && programme.getEducationLevel() != null) {
                    return programme.getEducationLevel().name();
                }
            }
        }
        return fallback != null && !fallback.isBlank() ? fallback.toUpperCase() : "COLLEGE";
    }
}
