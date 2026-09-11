package tz.elmkusoma.grading.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.grading.dto.request.CreateGradeBoundaryRequest;
import tz.elmkusoma.grading.dto.request.CreateGradingScaleRequest;
import tz.elmkusoma.grading.dto.request.GenerateReportCardRequest;
import tz.elmkusoma.grading.dto.response.GradeBoundaryResponse;
import tz.elmkusoma.grading.dto.response.GradingScaleResponse;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;
import tz.elmkusoma.grading.service.GradeBoundaryService;
import tz.elmkusoma.grading.service.GradingScaleService;
import tz.elmkusoma.grading.service.ReportCardService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/grading")
@RequiredArgsConstructor
@Tag(name = "Grading", description = "Grading scales, boundaries, and report cards")
public class GradingController {

    private final GradingScaleService gradingScaleService;
    private final GradeBoundaryService gradeBoundaryService;
    private final ReportCardService reportCardService;

    @PostMapping("/scales")
    @Operation(summary = "Create a grading scale")
    public ResponseEntity<ApiResponse<GradingScaleResponse>> createGradingScale(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody CreateGradingScaleRequest request) {
        GradingScaleResponse response = gradingScaleService.create(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grading scale created successfully", response));
    }

    @GetMapping("/scales")
    @Operation(summary = "Get grading scales by institution")
    public ResponseEntity<ApiResponse<List<GradingScaleResponse>>> getGradingScales(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<GradingScaleResponse> response = gradingScaleService.getByInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/scales/{id}")
    @Operation(summary = "Get grading scale by ID")
    public ResponseEntity<ApiResponse<GradingScaleResponse>> getGradingScale(@PathVariable UUID id) {
        GradingScaleResponse response = gradingScaleService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/scales/{id}")
    @Operation(summary = "Update grading scale")
    public ResponseEntity<ApiResponse<GradingScaleResponse>> updateGradingScale(
            @PathVariable UUID id,
            @Valid @RequestBody CreateGradingScaleRequest request) {
        GradingScaleResponse response = gradingScaleService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Grading scale updated successfully", response));
    }

    @DeleteMapping("/scales/{id}")
    @Operation(summary = "Delete grading scale")
    public ResponseEntity<ApiResponse<Void>> deleteGradingScale(@PathVariable UUID id) {
        gradingScaleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Grading scale deleted successfully", null));
    }

    @PostMapping("/boundaries")
    @Operation(summary = "Create a grade boundary")
    public ResponseEntity<ApiResponse<GradeBoundaryResponse>> createGradeBoundary(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody CreateGradeBoundaryRequest request) {
        GradeBoundaryResponse response = gradeBoundaryService.create(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grade boundary created successfully", response));
    }

    @GetMapping("/boundaries/scale/{scaleId}")
    @Operation(summary = "Get grade boundaries by scale")
    public ResponseEntity<ApiResponse<List<GradeBoundaryResponse>>> getGradeBoundaries(@PathVariable UUID scaleId) {
        List<GradeBoundaryResponse> response = gradeBoundaryService.getByGradingScaleId(scaleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/boundaries/{id}")
    @Operation(summary = "Get grade boundary by ID")
    public ResponseEntity<ApiResponse<GradeBoundaryResponse>> getGradeBoundary(@PathVariable UUID id) {
        GradeBoundaryResponse response = gradeBoundaryService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/boundaries/{id}")
    @Operation(summary = "Update grade boundary")
    public ResponseEntity<ApiResponse<GradeBoundaryResponse>> updateGradeBoundary(
            @PathVariable UUID id,
            @Valid @RequestBody CreateGradeBoundaryRequest request) {
        GradeBoundaryResponse response = gradeBoundaryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Grade boundary updated successfully", response));
    }

    @DeleteMapping("/boundaries/{id}")
    @Operation(summary = "Delete grade boundary")
    public ResponseEntity<ApiResponse<Void>> deleteGradeBoundary(@PathVariable UUID id) {
        gradeBoundaryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Grade boundary deleted successfully", null));
    }

    @PostMapping("/report-cards/generate")
    @Operation(summary = "Generate a report card for a student")
    public ResponseEntity<ApiResponse<ReportCardResponse>> generateReportCard(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody GenerateReportCardRequest request) {
        ReportCardResponse response = reportCardService.generate(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report card generated successfully", response));
    }

    @GetMapping("/report-cards/{id}")
    @Operation(summary = "Get report card by ID")
    public ResponseEntity<ApiResponse<ReportCardResponse>> getReportCard(@PathVariable UUID id) {
        ReportCardResponse response = reportCardService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/report-cards/student/{studentId}")
    @Operation(summary = "Get report cards by student")
    public ResponseEntity<ApiResponse<List<ReportCardResponse>>> getReportCardsByStudent(@PathVariable UUID studentId) {
        List<ReportCardResponse> response = reportCardService.getByStudentId(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/report-cards/term/{termId}")
    @Operation(summary = "Get report cards by term")
    public ResponseEntity<ApiResponse<List<ReportCardResponse>>> getReportCardsByTerm(@PathVariable UUID termId) {
        List<ReportCardResponse> response = reportCardService.getByTermId(termId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/report-cards/{id}/status")
    @Operation(summary = "Update report card status")
    public ResponseEntity<ApiResponse<ReportCardResponse>> updateReportCardStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        ReportCardResponse response = reportCardService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Report card status updated", response));
    }
}