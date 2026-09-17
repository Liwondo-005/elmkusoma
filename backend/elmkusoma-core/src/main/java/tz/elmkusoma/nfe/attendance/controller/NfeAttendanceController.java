package tz.elmkusoma.nfe.attendance.controller;

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
import tz.elmkusoma.nfe.attendance.dto.AttendanceRequest;
import tz.elmkusoma.nfe.attendance.dto.AttendanceResponse;
import tz.elmkusoma.nfe.attendance.service.NfeAttendanceService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/nfe/attendance")
@RequiredArgsConstructor
@Tag(name = "NFE Attendance Management", description = "CRUD operations for NFE attendance records")
public class NfeAttendanceController {

    private final NfeAttendanceService attendanceService;

    @PostMapping("/providers/{providerId}")
    @Operation(summary = "Record attendance for a session")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> recordAttendance(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId,
            @Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse attendance = attendanceService.recordAttendance(institutionId, providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attendance recorded successfully", attendance));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an attendance record by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getAttendance(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        AttendanceResponse attendance = attendanceService.getAttendance(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(attendance));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "List attendance records for a session")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceResponse>>> listAttendanceBySession(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AttendanceResponse> attendance = attendanceService.listAttendanceBySession(institutionId, sessionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(attendance));
    }

    @GetMapping("/learners/{learnerId}")
    @Operation(summary = "Get attendance records for a learner")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceByLearner(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID learnerId) {
        List<AttendanceResponse> attendance = attendanceService.getAttendanceByLearner(institutionId, learnerId);
        return ResponseEntity.ok(ApiResponse.success(attendance));
    }

    @GetMapping("/providers/{providerId}")
    @Operation(summary = "Get attendance records for a provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceByProvider(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId) {
        List<AttendanceResponse> attendance = attendanceService.getAttendanceByProvider(institutionId, providerId);
        return ResponseEntity.ok(ApiResponse.success(attendance));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an attendance record")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> updateAttendance(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse attendance = attendanceService.updateAttendance(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Attendance updated successfully", attendance));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an attendance record")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAttendance(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        attendanceService.deleteAttendance(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Attendance deleted successfully", null));
    }
}
