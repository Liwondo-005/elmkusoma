package tz.elmkusoma.nfe.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.assessment.dto.AssessmentRequest;
import tz.elmkusoma.nfe.assessment.dto.AssessmentResponse;
import tz.elmkusoma.nfe.assessment.service.NfeAssessmentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/nfe/assessments")
@RequiredArgsConstructor
@Tag(name = "NFE Assessment Management", description = "CRUD operations for NFE assessments")
public class NfeAssessmentController {

    private final NfeAssessmentService assessmentService;

    @PostMapping("/providers/{providerId}")
    @Operation(summary = "Create a new assessment for a provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> createAssessment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId,
            @Valid @RequestBody AssessmentRequest request) {
        AssessmentResponse assessment = assessmentService.createAssessment(institutionId, providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assessment created successfully", assessment));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an assessment by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> getAssessment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        AssessmentResponse assessment = assessmentService.getAssessment(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(assessment));
    }

    @GetMapping("/providers/{providerId}")
    @Operation(summary = "List all assessments for a provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<AssessmentResponse>>> listAssessments(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AssessmentResponse> assessments = assessmentService.listAssessments(institutionId, providerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(assessments));
    }

    @GetMapping("/providers/{providerId}/published")
    @Operation(summary = "Get all published assessments for a provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getPublishedAssessments(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId) {
        List<AssessmentResponse> assessments = assessmentService.getPublishedAssessments(institutionId, providerId);
        return ResponseEntity.ok(ApiResponse.success(assessments));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an assessment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> updateAssessment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody AssessmentRequest request) {
        AssessmentResponse assessment = assessmentService.updateAssessment(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Assessment updated successfully", assessment));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an assessment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAssessment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        assessmentService.deleteAssessment(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Assessment deleted successfully", null));
    }
}
