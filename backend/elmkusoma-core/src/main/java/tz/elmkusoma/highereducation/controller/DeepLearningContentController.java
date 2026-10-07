package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.dto.DeepLearningContentDTO;
import tz.elmkusoma.highereducation.service.DeepLearningContentService;
import tz.elmkusoma.highereducation.service.HighEdIdentity;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/college/learner/deep-learning")
@RequiredArgsConstructor
public class DeepLearningContentController {

    private final DeepLearningContentService contentService;
    private final HighEdIdentity highEdIdentity;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DeepLearningContentDTO>> createContent(
            @RequestBody DeepLearningContentDTO dto,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, dto.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        dto.setStudentId(learnerId);
        dto.setInstitutionId(serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success("Content created", contentService.createContent(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DeepLearningContentDTO>> getContent(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        DeepLearningContentDTO content = contentService.getContent(id);
        assertContentReadable(content, callerUserId, userRole, serverInstitutionId, id);
        return ResponseEntity.ok(ApiResponse.success(content));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<DeepLearningContentDTO>>> getStudentContent(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(contentService.getStudentContent(learnerId)));
    }

    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<DeepLearningContentDTO>>> getStudentCourseContent(
            @PathVariable UUID studentId, @PathVariable UUID courseId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(contentService.getStudentCourseContent(learnerId, courseId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DeepLearningContentDTO>> updateContent(
            @PathVariable UUID id, @RequestBody DeepLearningContentDTO dto,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        DeepLearningContentDTO existing = contentService.getContent(id);
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, existing.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success("Content updated", contentService.updateContent(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteContent(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        DeepLearningContentDTO existing = contentService.getContent(id);
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, existing.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        contentService.deleteContent(id);
        return ResponseEntity.ok(ApiResponse.success("Content deleted", null));
    }

    private void assertContentReadable(DeepLearningContentDTO content, UUID callerUserId, String userRole,
                                       UUID serverInstitutionId, UUID id) {
        if (HighEdIdentity.isLearner(userRole)) {
            if (content.getStudentId() != null) {
                try {
                    highEdIdentity.resolveStudentId(callerUserId, userRole, content.getStudentId());
                } catch (ForbiddenException e) {
                    throw new ResourceNotFoundException("Deep learning content", "id", id);
                }
            } else if (serverInstitutionId == null || !serverInstitutionId.equals(content.getInstitutionId())) {
                throw new ResourceNotFoundException("Deep learning content", "id", id);
            }
            return;
        }
        highEdIdentity.assertStudentInInstitution(content.getStudentId(), serverInstitutionId);
    }
}
