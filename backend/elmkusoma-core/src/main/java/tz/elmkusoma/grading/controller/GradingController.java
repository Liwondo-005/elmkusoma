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
            @RequestAttribute("institutionId") UUID institutionId,
            @Valid @RequestBody CreateGradingScaleRequest request) {
        GradingScaleResponse response = gradingScaleService.create(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grading scale created successfully", response));
    }

    @GetMapping("/scales")
    @Operation(summary = "Get grading scales by institution")
    public ResponseEntity<ApiResponse<List<GradingScaleResponse>>> getGradingScales(
            @RequestAttribute("institutionId") UUID institutionId) {
        List<GradingScaleResponse> response = gradingScaleService.getByInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/scales/{id}")
    @Operation(summary = "Get grading scale by ID")
    public ResponseEntity<ApiResponse<GradingScaleResponse>> getGradingScale(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        GradingScaleResponse response = gradingScaleService.getById(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/scales/{id}")
    @Operation(summary = "Update grading scale")
    public ResponseEntity<ApiResponse<GradingScaleResponse>> updateGradingScale(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateGradingScaleRequest request) {
        GradingScaleResponse response = gradingScaleService.update(id, institutionId, request);
        return ResponseEntity.ok(ApiResponse.success("Grading scale updated successfully", response));
    }

    @DeleteMapping("/scales/{id}")
    @Operation(summary = "Delete grading scale")
    public ResponseEntity<ApiResponse<Void>> deleteGradingScale(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        gradingScaleService.delete(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Grading scale deleted successfully", null));
    }

    @PostMapping("/boundaries")
    @Operation(summary = "Create a grade boundary")
    public ResponseEntity<ApiResponse<GradeBoundaryResponse>> createGradeBoundary(
            @RequestAttribute("institutionId") UUID institutionId,
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
    public ResponseEntity<ApiResponse<GradeBoundaryResponse>> getGradeBoundary(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        GradeBoundaryResponse response = gradeBoundaryService.getById(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/boundaries/{id}")
    @Operation(summary = "Update grade boundary")
    public ResponseEntity<ApiResponse<GradeBoundaryResponse>> updateGradeBoundary(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateGradeBoundaryRequest request) {
        GradeBoundaryResponse response = gradeBoundaryService.update(id, institutionId, request);
        return ResponseEntity.ok(ApiResponse.success("Grade boundary updated successfully", response));
    }

    @DeleteMapping("/boundaries/{id}")
    @Operation(summary = "Delete grade boundary")
    public ResponseEntity<ApiResponse<Void>> deleteGradeBoundary(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        gradeBoundaryService.delete(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Grade boundary deleted successfully", null));
    }

    @PostMapping("/report-cards/generate")
    @Operation(summary = "Generate a report card for a student")
    public ResponseEntity<ApiResponse<ReportCardResponse>> generateReportCard(
            @RequestAttribute("institutionId") UUID institutionId,
            @Valid @RequestBody GenerateReportCardRequest request) {
        ReportCardResponse response = reportCardService.generate(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report card generated successfully", response));
    }

    @GetMapping("/report-cards/{id}")
    @Operation(summary = "Get report card by ID")
    public ResponseEntity<ApiResponse<ReportCardResponse>> getReportCard(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        ReportCardResponse response = reportCardService.getById(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/report-cards/student/{studentId}")
    @Operation(summary = "Get report cards by student")
    public ResponseEntity<ApiResponse<List<ReportCardResponse>>> getReportCardsByStudent(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID studentId) {
        List<ReportCardResponse> response = reportCardService.getByStudentId(studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/report-cards/term/{termId}")
    @Operation(summary = "Get report cards by term")
    public ResponseEntity<ApiResponse<List<ReportCardResponse>>> getReportCardsByTerm(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID termId) {
        List<ReportCardResponse> response = reportCardService.getByTermId(termId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/report-cards/{id}/status")
    @Operation(summary = "Update report card status")
    public ResponseEntity<ApiResponse<ReportCardResponse>> updateReportCardStatus(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id,
            @RequestParam String status) {
        ReportCardResponse response = reportCardService.updateStatus(id, institutionId, status);
        return ResponseEntity.ok(ApiResponse.success("Report card status updated", response));
    }
}