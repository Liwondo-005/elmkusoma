package tz.elmkusoma.highereducation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.dto.*;
import tz.elmkusoma.highereducation.service.CompetencyService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/competencies")
@RequiredArgsConstructor
@Tag(name = "Competency", description = "Competency-based education management")
public class CompetencyController {

    private final CompetencyService competencyService;

    @GetMapping
    @Operation(summary = "List all competencies filtered by institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<CompetencyDTO>>> getAll(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<CompetencyDTO> result = competencyService.getAllCompetencies(institutionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a competency by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<CompetencyDTO>> getById(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        CompetencyDTO result = competencyService.getCompetencyById(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @Operation(summary = "Create a new competency")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CompetencyDTO>> create(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestBody CompetencyDTO dto) {
        CompetencyDTO result = competencyService.createCompetency(institutionId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Competency created", result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a competency")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CompetencyDTO>> update(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestBody CompetencyDTO dto) {
        CompetencyDTO result = competencyService.updateCompetency(id, institutionId, dto);
        return ResponseEntity.ok(ApiResponse.success("Competency updated", result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a competency")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        competencyService.deleteCompetency(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Competency deleted", null));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get student's competency records")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentCompetencyDTO>>> getStudentCompetencies(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<StudentCompetencyDTO> result = competencyService.getStudentCompetencies(studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/student/{studentId}/competency/{competencyId}")
    @Operation(summary = "Update student competency status")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CompetencyRecordDTO>> updateCompetencyRecord(
            @PathVariable UUID studentId,
            @PathVariable UUID competencyId,
            @RequestParam String status,
            @RequestParam(required = false) String evidence,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID assessedBy) {
        CompetencyRecordDTO result = competencyService.updateCompetencyRecord(
                studentId, competencyId, status, evidence, assessedBy);
        return ResponseEntity.ok(ApiResponse.success("Competency record updated", result));
    }

    @GetMapping("/student/{studentId}/summary")
    @Operation(summary = "Get student competency summary counts")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CompetencySummaryDTO>> getSummary(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        CompetencySummaryDTO result = competencyService.getCompetencySummary(studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/assessments")
    @Operation(summary = "Link an assessment to a competency")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CompetencyAssessmentDTO>> linkAssessment(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        UUID assessmentId = UUID.fromString((String) body.get("assessmentId"));
        Integer weight = body.get("weight") != null ? (Integer) body.get("weight") : 100;
        CompetencyAssessmentDTO result = competencyService.linkAssessmentToCompetency(id, assessmentId, weight);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assessment linked to competency", result));
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(summary = "Get competencies for a subject")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<CompetencyDTO>>> getBySubject(
            @PathVariable UUID subjectId) {
        List<CompetencyDTO> result = competencyService.getCompetenciesForSubject(subjectId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
