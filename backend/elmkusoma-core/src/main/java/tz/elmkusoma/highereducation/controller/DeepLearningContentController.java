package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.dto.DeepLearningContentDTO;
import tz.elmkusoma.highereducation.service.DeepLearningContentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/college/learner/deep-learning")
@RequiredArgsConstructor
public class DeepLearningContentController {

    private final DeepLearningContentService contentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DeepLearningContentDTO>> createContent(
            @RequestBody DeepLearningContentDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Content created", contentService.createContent(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DeepLearningContentDTO>> getContent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(contentService.getContent(id)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<DeepLearningContentDTO>>> getStudentContent(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(contentService.getStudentContent(studentId)));
    }

    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<DeepLearningContentDTO>>> getStudentCourseContent(
            @PathVariable UUID studentId, @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(contentService.getStudentCourseContent(studentId, courseId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DeepLearningContentDTO>> updateContent(
            @PathVariable UUID id, @RequestBody DeepLearningContentDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Content updated", contentService.updateContent(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteContent(@PathVariable UUID id) {
        contentService.deleteContent(id);
        return ResponseEntity.ok(ApiResponse.success("Content deleted", null));
    }
}
