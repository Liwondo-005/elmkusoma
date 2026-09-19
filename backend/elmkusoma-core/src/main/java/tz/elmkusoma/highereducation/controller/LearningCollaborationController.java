package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.dto.LearningCollaborationDTO;
import tz.elmkusoma.highereducation.service.LearningCollaborationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/college/learner/collaborations")
@RequiredArgsConstructor
public class LearningCollaborationController {

    private final LearningCollaborationService collaborationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<LearningCollaborationDTO>> createCollaboration(
            @RequestBody LearningCollaborationDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Collaboration created", collaborationService.createCollaboration(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<LearningCollaborationDTO>> getCollaboration(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(collaborationService.getCollaboration(id)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<LearningCollaborationDTO>>> getStudentCollaborations(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(collaborationService.getStudentCollaborations(studentId)));
    }

    @GetMapping("/student/{studentId}/active")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<LearningCollaborationDTO>>> getActiveCollaborations(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(collaborationService.getStudentActiveCollaborations(studentId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<LearningCollaborationDTO>> updateCollaboration(
            @PathVariable UUID id, @RequestBody LearningCollaborationDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Collaboration updated", collaborationService.updateCollaboration(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCollaboration(@PathVariable UUID id) {
        collaborationService.deleteCollaboration(id);
        return ResponseEntity.ok(ApiResponse.success("Collaboration deleted", null));
    }
}
