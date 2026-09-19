package tz.elmkusoma.nfe.learner.controller;

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
import tz.elmkusoma.nfe.learner.dto.LearnerRequest;
import tz.elmkusoma.nfe.learner.dto.LearnerResponse;
import tz.elmkusoma.nfe.learner.service.LearnerService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/nfe/learners")
@RequiredArgsConstructor
@Tag(name = "NFE Learner Management", description = "CRUD operations for NFE learners/participants")
public class NfeLearnerController {

    private final LearnerService learnerService;

    @PostMapping
    @Operation(summary = "Create a new learner/participant")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LearnerResponse>> createLearner(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody LearnerRequest request) {
        LearnerResponse learner = learnerService.createLearner(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Learner created successfully", learner));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a learner by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LearnerResponse>> getLearner(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        LearnerResponse learner = learnerService.getLearner(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(learner));
    }

    @GetMapping
    @Operation(summary = "List all learners in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<LearnerResponse>>> listLearners(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<LearnerResponse> learners = learnerService.listLearners(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(learners));
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "Get learners by provider ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<LearnerResponse>>> getLearnersByProvider(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId) {
        List<LearnerResponse> learners = learnerService.getLearnersByProvider(institutionId, providerId);
        return ResponseEntity.ok(ApiResponse.success(learners));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get learners by user ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<LearnerResponse>>> getLearnersByUser(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID userId) {
        List<LearnerResponse> learners = learnerService.getLearnersByUser(institutionId, userId);
        return ResponseEntity.ok(ApiResponse.success(learners));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a learner")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LearnerResponse>> updateLearner(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody LearnerRequest request) {
        LearnerResponse learner = learnerService.updateLearner(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Learner updated successfully", learner));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a learner")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLearner(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        learnerService.deleteLearner(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Learner deleted successfully", null));
    }
}
