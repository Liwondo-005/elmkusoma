package tz.elmkusoma.grading.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.grading.dto.request.CreateGradeBoundaryRequest;
import tz.elmkusoma.grading.dto.request.CreateGradingScaleRequest;
import tz.elmkusoma.grading.dto.request.CreateRubricCriteriaRequest;
import tz.elmkusoma.grading.dto.request.CreateRubricRequest;
import tz.elmkusoma.grading.dto.request.GenerateReportCardRequest;
import tz.elmkusoma.grading.dto.response.GradeBoundaryResponse;
import tz.elmkusoma.grading.dto.response.GradebookResponse;
import tz.elmkusoma.grading.dto.response.GradingScaleResponse;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;
import tz.elmkusoma.grading.dto.response.RubricResponse;
import tz.elmkusoma.grading.service.GradeBoundaryService;
import tz.elmkusoma.grading.service.GradebookService;
import tz.elmkusoma.grading.service.GradingScaleService;
import tz.elmkusoma.grading.service.ReportCardService;
import tz.elmkusoma.grading.service.RubricService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/grading")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
@Tag(name = "Grading", description = "Grading scales, boundaries, and report cards")
public class GradingController {

    private final GradingScaleService gradingScaleService;
    private final GradeBoundaryService gradeBoundaryService;
    private final ReportCardService reportCardService;
    private final GradebookService gradebookService;
    private final RubricService rubricService;

    @PostMapping("/scales")
    @Operation(summary = "Create a grading scale")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<GradingScaleResponse>> createGradingScale(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody CreateGradingScaleRequest request) {
        GradingScaleResponse response = gradingScaleService.create(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grading scale created successfully", response));
    }

    @GetMapping("/scales")
    @Operation(summary = "Get grading scales by institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<GradingScaleResponse>>> getGradingScales(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<GradingScaleResponse> response = gradingScaleService.getByInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/scales/{id}")
    @Operation(summary = "Get grading scale by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<GradingScaleResponse>> getGradingScale(@PathVariable UUID id) {
        GradingScaleResponse response = gradingScaleService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/scales/{id}")
    @Operation(summary = "Update grading scale")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<GradingScaleResponse>> updateGradingScale(
            @PathVariable UUID id,
            @Valid @RequestBody CreateGradingScaleRequest request) {
        GradingScaleResponse response = gradingScaleService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Grading scale updated successfully", response));
    }

    @DeleteMapping("/scales/{id}")
    @Operation(summary = "Delete grading scale")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGradingScale(@PathVariable UUID id) {
        gradingScaleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Grading scale deleted successfully", null));
    }

    @PostMapping("/boundaries")
    @Operation(summary = "Create a grade boundary")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<GradeBoundaryResponse>> createGradeBoundary(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody CreateGradeBoundaryRequest request) {
        GradeBoundaryResponse response = gradeBoundaryService.create(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grade boundary created successfully", response));
    }

    @GetMapping("/boundaries/scale/{scaleId}")
    @Operation(summary = "Get grade boundaries by scale")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<GradeBoundaryResponse>>> getGradeBoundaries(@PathVariable UUID scaleId) {
        List<GradeBoundaryResponse> response = gradeBoundaryService.getByGradingScaleId(scaleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/boundaries/{id}")
    @Operation(summary = "Get grade boundary by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<GradeBoundaryResponse>> getGradeBoundary(@PathVariable UUID id) {
        GradeBoundaryResponse response = gradeBoundaryService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/boundaries/{id}")
    @Operation(summary = "Update grade boundary")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<GradeBoundaryResponse>> updateGradeBoundary(
            @PathVariable UUID id,
            @Valid @RequestBody CreateGradeBoundaryRequest request) {
        GradeBoundaryResponse response = gradeBoundaryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Grade boundary updated successfully", response));
    }

    @DeleteMapping("/boundaries/{id}")
    @Operation(summary = "Delete grade boundary")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGradeBoundary(@PathVariable UUID id) {
        gradeBoundaryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Grade boundary deleted successfully", null));
    }

    @PostMapping("/report-cards/generate")
    @Operation(summary = "Generate a report card for a student")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ReportCardResponse>> generateReportCard(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody GenerateReportCardRequest request) {
        ReportCardResponse response = reportCardService.generate(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report card generated successfully", response));
    }

    @GetMapping("/report-cards/{id}")
    @Operation(summary = "Get report card by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ReportCardResponse>> getReportCard(@PathVariable UUID id) {
        ReportCardResponse response = reportCardService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/report-cards/student/{studentId}")
    @Operation(summary = "Get report cards by student")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ReportCardResponse>>> getReportCardsByStudent(@PathVariable UUID studentId) {
        List<ReportCardResponse> response = reportCardService.getByStudentId(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/report-cards/term/{termId}")
    @Operation(summary = "Get report cards by term")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ReportCardResponse>>> getReportCardsByTerm(@PathVariable UUID termId) {
        List<ReportCardResponse> response = reportCardService.getByTermId(termId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/report-cards/{id}/status")
    @Operation(summary = "Update report card status (PUBLISHED releases the card to the learner)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ReportCardResponse>> updateReportCardStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        ReportCardResponse response = reportCardService.updateStatus(id, status, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Report card status updated", response));
    }

    @GetMapping("/report-cards/for-students")
    @Operation(summary = "Batch: every report card of the given students (one request)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ReportCardResponse>>> getReportCardsForStudents(
            @RequestParam("ids") List<UUID> studentIds) {
        List<ReportCardResponse> response = reportCardService.getByStudentIds(studentIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Gradebook ─────────────────────────────────────────────────────────────

    @GetMapping("/gradebook/class/{classGroupId}")
    @Operation(summary = "Class gradebook: learners + assignment submissions + assessment results in one request")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<GradebookResponse>> getGradebook(
            @PathVariable UUID classGroupId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        GradebookResponse response = gradebookService.getGradebook(classGroupId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Rubrics (§36 rubric-based assessment) ─────────────────────────────────

    @PostMapping("/rubrics")
    @Operation(summary = "Create a grading rubric (optionally with criteria)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<RubricResponse>> createRubric(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole,
            @Valid @RequestBody CreateRubricRequest request) {
        RubricResponse response = rubricService.create(institutionId, request, userEmail, userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rubric created", response));
    }

    @GetMapping("/rubrics")
    @Operation(summary = "List grading rubrics for the institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<RubricResponse>>> getRubrics(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<RubricResponse> response = rubricService.getByInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/rubrics/{id}")
    @Operation(summary = "Get a rubric with its criteria")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<RubricResponse>> getRubric(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        RubricResponse response = rubricService.getById(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/rubrics/{id}/criteria")
    @Operation(summary = "Add criteria lines to a rubric")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<RubricResponse>> addRubricCriteria(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole,
            @Valid @RequestBody List<CreateRubricCriteriaRequest> criteria) {
        RubricResponse response = rubricService.addCriteria(id, institutionId, criteria, userEmail, userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Criteria added", response));
    }

    @DeleteMapping("/rubrics/{id}")
    @Operation(summary = "Delete a rubric and its criteria")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteRubric(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        rubricService.delete(id, institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Rubric deleted", null));
    }
}
