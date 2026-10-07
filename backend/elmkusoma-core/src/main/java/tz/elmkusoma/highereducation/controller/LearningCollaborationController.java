package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.dto.LearningCollaborationDTO;
import tz.elmkusoma.highereducation.service.HighEdIdentity;
import tz.elmkusoma.highereducation.service.LearningCollaborationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/college/learner/collaborations")
@RequiredArgsConstructor
public class LearningCollaborationController {

    private final LearningCollaborationService collaborationService;
    private final HighEdIdentity highEdIdentity;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<LearningCollaborationDTO>> createCollaboration(
            @RequestBody LearningCollaborationDTO dto,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, dto.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        dto.setStudentId(learnerId);
        dto.setInstitutionId(serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success("Collaboration created", collaborationService.createCollaboration(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<LearningCollaborationDTO>> getCollaboration(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        LearningCollaborationDTO collaboration = collaborationService.getCollaboration(id);
        assertCollaborationReadable(collaboration, callerUserId, userRole, serverInstitutionId, id);
        return ResponseEntity.ok(ApiResponse.success(collaboration));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<LearningCollaborationDTO>>> getStudentCollaborations(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(collaborationService.getStudentCollaborations(learnerId)));
    }

    @GetMapping("/student/{studentId}/active")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<LearningCollaborationDTO>>> getActiveCollaborations(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(collaborationService.getStudentActiveCollaborations(learnerId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<LearningCollaborationDTO>> updateCollaboration(
            @PathVariable UUID id, @RequestBody LearningCollaborationDTO dto,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        LearningCollaborationDTO existing = collaborationService.getCollaboration(id);
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, existing.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success("Collaboration updated", collaborationService.updateCollaboration(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCollaboration(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        LearningCollaborationDTO existing = collaborationService.getCollaboration(id);
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, existing.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        collaborationService.deleteCollaboration(id);
        return ResponseEntity.ok(ApiResponse.success("Collaboration deleted", null));
    }

    private void assertCollaborationReadable(LearningCollaborationDTO collaboration, UUID callerUserId, String userRole,
                                             UUID serverInstitutionId, UUID id) {
        if (HighEdIdentity.isLearner(userRole)) {
            if (collaboration.getStudentId() != null) {
                try {
                    highEdIdentity.resolveStudentId(callerUserId, userRole, collaboration.getStudentId());
                } catch (ForbiddenException e) {
                    throw new ResourceNotFoundException("Learning collaboration", "id", id);
                }
            } else if (serverInstitutionId == null || !serverInstitutionId.equals(collaboration.getInstitutionId())) {
                throw new ResourceNotFoundException("Learning collaboration", "id", id);
            }
            return;
        }
        highEdIdentity.assertStudentInInstitution(collaboration.getStudentId(), serverInstitutionId);
    }
}
