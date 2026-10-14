package tz.elmkusoma.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.UUID;

/**
 * Guards the audience a piece of teacher-authored content is aimed at.
 *
 * <p>A class group or subject id arriving in a request body is a claim, not proof: it is chosen
 * by the client, so it must be checked against the caller's own institution before it is stored.
 * Without that check a teacher can publish lessons, assessments, announcements, live sessions or
 * media into another school's class simply by naming its id.</p>
 *
 * <p>The rule has two halves and they deliberately differ in who they bind:</p>
 * <ul>
 *   <li><b>Institution containment</b> applies to <em>every</em> caller and every content type.
 *       A missing target answers 404 and a foreign target answers 403, so the API never confirms
 *       that an id belonging to someone else exists.</li>
 *   <li><b>Teaching assignment</b> applies only when the role is {@code TEACHER}, because naming
 *       a class in your own institution is not the same as being entitled to teach it. Admin
 *       roles keep institution containment alone so they can target any class in their own
 *       school.</li>
 * </ul>
 *
 * <p>This is the single implementation of that rule; the services that used to hold private
 * copies delegate here so the policy cannot drift between content types.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AudienceTargetGuard {

    private final OwnershipGuard ownershipGuard;
    private final ClassGroupRepository classGroupRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;

    /**
     * Validates an audience target for create or retarget.
     *
     * @param institutionId the caller's institution, from the resolved request context
     * @param classGroupId  target class, or null when the content is institution-wide
     * @param subjectId     target subject, or null when unassigned
     * @param callerUserId  the authenticated user, needed for the teacher-assignment half
     * @param userRole      the authenticated role
     */
    public void validateTarget(UUID institutionId, UUID classGroupId, UUID subjectId,
                               UUID callerUserId, String userRole) {
        verifyClassGroupBelongsToInstitution(classGroupId, institutionId);
        verifySubjectBelongsToInstitution(institutionId, subjectId);
        if (classGroupId == null || !"TEACHER".equals(userRole)) {
            return;
        }
        UUID teacherId = callerUserId == null ? null
                : teacherRepository.findByUserIdAndInstitutionId(callerUserId, institutionId)
                        .map(Teacher::getId)
                        .orElse(null);
        boolean assigned = teacherId != null
                && teacherAssignmentRepository.existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(
                        teacherId, classGroupId);
        if (!assigned) {
            throw new SecurityException("You are not assigned to this class");
        }
    }

    /**
     * Cross-tenant guard for a class-group target: no-op when absent, 404 when unknown,
     * 403 when it belongs to another institution.
     */
    public void verifyClassGroupBelongsToInstitution(UUID classGroupId, UUID institutionId) {
        if (classGroupId == null) {
            return;
        }
        ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                .filter(cg -> !Boolean.TRUE.equals(cg.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("ClassGroup", "id", classGroupId));
        ownershipGuard.verifyInstitution(classGroup.getInstitutionId(), institutionId);
    }

    /**
     * Cross-tenant guard for a subject target: no-op when absent, 404 when unknown,
     * 403 when it belongs to another institution.
     */
    public void verifySubjectBelongsToInstitution(UUID institutionId, UUID subjectId) {
        if (subjectId == null) {
            return;
        }
        Subject subject = subjectRepository.findById(subjectId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
        ownershipGuard.verifyInstitution(subject.getInstitutionId(), institutionId);
    }
}
