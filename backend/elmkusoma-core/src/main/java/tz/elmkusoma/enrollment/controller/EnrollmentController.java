package tz.elmkusoma.enrollment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.dto.request.EnrollmentRequest;
import tz.elmkusoma.enrollment.dto.request.TransferRequest;
import tz.elmkusoma.enrollment.dto.response.EnrollmentResponse;
import tz.elmkusoma.enrollment.dto.response.TransferResponse;
import tz.elmkusoma.enrollment.service.EnrollmentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment", description = "Student enrollment management")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @Operation(summary = "Enroll a student in a class")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.enroll(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student enrolled successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all enrollments for an institution")
    public ResponseEntity<ApiResponse<PageResponse<EnrollmentResponse>>> getEnrollments(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<EnrollmentResponse> response = enrollmentService.getEnrollments(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get enrollments by student")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getByStudent(@PathVariable UUID studentId) {
        List<EnrollmentResponse> response = enrollmentService.getEnrollmentsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/class/{classGroupId}")
    @Operation(summary = "Get enrollments by class group")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getByClass(@PathVariable UUID classGroupId) {
        List<EnrollmentResponse> response = enrollmentService.getEnrollmentsByClass(classGroupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update enrollment status")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam Enrollment.EnrollmentStatus status) {
        EnrollmentResponse response = enrollmentService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated", response));
    }

    @PostMapping("/{id}/transfer")
    @Operation(summary = "Transfer student to another class")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody TransferRequest request) {
        TransferResponse response = enrollmentService.transfer(id, institutionId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student transferred successfully", response));
    }

    @GetMapping("/{id}/transfers")
    @Operation(summary = "Get transfer history for an enrollment")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getTransferHistory(@PathVariable UUID id) {
        List<TransferResponse> response = enrollmentService.getTransferHistory(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
