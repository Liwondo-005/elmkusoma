package tz.elmkusoma.highereducation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.domain.DemonstrationStatus;
import tz.elmkusoma.highereducation.dto.DemonstrationDTO;
import tz.elmkusoma.highereducation.service.DemonstrationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/demonstrations")
@RequiredArgsConstructor
public class DemonstrationController {

    private final DemonstrationService demonstrationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<DemonstrationDTO>>> listDemonstrations(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(required = false) DemonstrationStatus status) {
        List<DemonstrationDTO> demonstrations = status != null
                ? demonstrationService.getDemonstrationsByStatus(institutionId, status)
                : demonstrationService.getDemonstrations(institutionId);
        return ResponseEntity.ok(ApiResponse.success(demonstrations));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<DemonstrationDTO>> getDemonstration(@PathVariable UUID id) {
        DemonstrationDTO demonstration = demonstrationService.getDemonstration(id);
        return ResponseEntity.ok(ApiResponse.success(demonstration));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DemonstrationDTO>> createDemonstration(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody DemonstrationDTO dto) {
        dto.setInstitutionId(institutionId);
        DemonstrationDTO created = demonstrationService.createDemonstration(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demonstration created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DemonstrationDTO>> updateDemonstration(
            @PathVariable UUID id,
            @Valid @RequestBody DemonstrationDTO dto) {
        DemonstrationDTO updated = demonstrationService.updateDemonstration(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Demonstration updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDemonstration(@PathVariable UUID id) {
        demonstrationService.deleteDemonstration(id);
        return ResponseEntity.ok(ApiResponse.success("Demonstration deleted", null));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DemonstrationDTO>> submitForReview(@PathVariable UUID id) {
        DemonstrationDTO submitted = demonstrationService.submitForReview(id);
        return ResponseEntity.ok(ApiResponse.success("Demonstration submitted for review", submitted));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<DemonstrationDTO>> reviewDemonstration(
            @PathVariable UUID id,
            @RequestParam UUID reviewerId,
            @RequestParam DemonstrationStatus status,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Integer score) {
        DemonstrationDTO reviewed = demonstrationService.review(id, reviewerId, status, notes, score);
        return ResponseEntity.ok(ApiResponse.success("Demonstration reviewed", reviewed));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<DemonstrationDTO>>> getStudentDemonstrations(
            @PathVariable UUID studentId) {
        List<DemonstrationDTO> demonstrations = demonstrationService.getStudentDemonstrations(studentId);
        return ResponseEntity.ok(ApiResponse.success(demonstrations));
    }
}
