package tz.elmkusoma.liveclass.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.UUID;

/**
 * The role/ownership half of the live-class recording authorisation rule.
 *
 * <p>This is the exact decision that {@code LiveSessionController} has always made before
 * handing out a recording URL - an admin may read any recording in scope, a teacher only a
 * class they own, a learner only a session they may actually attend. It was a private method
 * on that controller; it now lives here so the byte-serving playback endpoint and the
 * download-URL endpoint share one implementation and cannot drift apart.</p>
 *
 * <p>Institution scoping and authentication deliberately stay with the caller: the existing
 * endpoint answers those in a specific order (404 for a missing class, 403 for a foreign
 * institution, 401 for an anonymous caller, then this check), and reordering them would
 * change responses that the security regression suite pins.</p>
 */
@Service
@RequiredArgsConstructor
public class LiveClassRecordingAccessService {

    private final LiveClassRepository liveClassRepository;
    private final TeacherRepository teacherRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final ClassAccessGuard classAccessGuard;

    /**
     * @param liveClass            the already-loaded, already-institution-scoped class
     * @param userId               the authenticated caller, or null when anonymous
     * @param callerInstitutionId  the institution the caller was resolved into
     * @param userRole             the caller's role
     * @return true only when this caller may read this class's recording
     */
    public boolean isAllowed(LiveClass liveClass, UUID userId, UUID callerInstitutionId, String userRole) {
        if (liveClass == null) {
            return false;
        }
        if ("ADMIN".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole)
                || "NATIONAL_ADMIN".equals(userRole)) {
            return true;
        }
        if ("TEACHER".equals(userRole)) {
            if (userId == null || callerInstitutionId == null) {
                return false;
            }
            // teacher owns the class (teacherId may store either Teacher.id or User.id)
            return teacherRepository.findByUserIdAndInstitutionId(userId, callerInstitutionId)
                    .map(t -> liveClass.getTeacherId() != null
                            && (liveClass.getTeacherId().equals(t.getId())
                            || liveClass.getTeacherId().equals(userId)))
                    .orElse(liveClass.getTeacherId() != null
                            && liveClass.getTeacherId().equals(userId));
        }
        // learner: must be an active member of the class's institution
        if (userId == null || callerInstitutionId == null) {
            return false;
        }
        if (!membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, callerInstitutionId)) {
            return false;
        }
        if (liveClass.getClassGroupId() == null) {
            // open session: any institution member may watch the recording
            return true;
        }
        // class-scoped session: prove membership of the target class
        // (student_class_assignments ∪ ENROLLED enrollments, strict)
        String email = userRepository.findById(userId).map(User::getEmail).orElse(null);
        return classAccessGuard.isLearnerInClass(email, liveClass.getClassGroupId());
    }

    /** Loads a live class for a recording check, or empty when it is missing or deleted. */
    public LiveClass findClass(UUID classId) {
        return classId == null ? null : liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
    }
}
