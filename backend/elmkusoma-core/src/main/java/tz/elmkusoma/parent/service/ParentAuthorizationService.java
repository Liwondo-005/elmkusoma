package tz.elmkusoma.parent.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParentAuthorizationService {

    private final ParentRepository parentRepository;
    private final ParentStudentLinkRepository studentLinkRepository;
    private final InstitutionMembershipRepository membershipRepository;

    /**
     * Resolve the Parent entity from the authenticated user's userId.
     * Throws ResourceNotFoundException if no parent profile exists.
     */
    public Parent resolveParent(UUID userId) {
        return parentRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException("Parent profile", "userId", userId));
    }

    /**
     * Verify that the authenticated parent is authorized to access the given student.
     * Throws ForbiddenException if not authorized.
     * Returns the Parent entity for further use.
     */
    public Parent requireChildAccess(UUID userId, UUID studentId) {
        Parent parent = resolveParent(userId);
        boolean authorized = studentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(parent.getId(), studentId);
        if (!authorized) {
            throw new ForbiddenException("Parent is not authorized to access this student's data");
        }
        return parent;
    }

    /**
     * Check if the parent is authorized to access the given student (no exception).
     */
    public boolean isAuthorizedChild(UUID userId, UUID studentId) {
        Parent parent = resolveParent(userId);
        return studentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(parent.getId(), studentId);
    }

    /**
     * Verify the parent belongs to the given institution.
     */
    public void requireInstitutionMembership(UUID userId, UUID institutionId) {
        if (institutionId != null) {
            boolean isMember = membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, institutionId);
            if (!isMember) {
                throw new ForbiddenException("Not a member of this institution");
            }
        }
    }
}
