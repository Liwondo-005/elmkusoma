package tz.elmkusoma.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.attendance.dto.request.BulkMarkAttendanceRequest;
import tz.elmkusoma.attendance.dto.request.MarkAttendanceRequest;
import tz.elmkusoma.attendance.dto.response.AttendanceRecordResponse;
import tz.elmkusoma.attendance.dto.response.AttendanceSummaryResponse;
import tz.elmkusoma.attendance.service.AttendanceService;
import tz.elmkusoma.common.ApiResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
@Tag(name = "Attendance", description = "Student attendance management")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/mark")
    @Operation(summary = "Mark attendance for a student")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> markAttendance(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID markedBy,
            @Valid @RequestBody MarkAttendanceRequest request) {
        AttendanceRecordResponse response = attendanceService.markAttendance(institutionId, markedBy, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attendance marked successfully", response));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk mark attendance for a class")
    public ResponseEntity<ApiResponse<Void>> bulkMarkAttendance(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID markedBy,
            @Valid @RequestBody BulkMarkAttendanceRequest request) {
        attendanceService.markBulkAttendance(institutionId, markedBy, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bulk attendance marked successfully", null));
    }

    @GetMapping
    @Operation(summary = "Get attendance by class and date")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getAttendance(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam UUID classId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceRecordResponse> response = attendanceService.getByClassAndDate(classId, date, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get attendance by student and date range")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getStudentAttendance(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID studentId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceRecordResponse> response = attendanceService.getByStudentAndDateRange(studentId, startDate, endDate, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get attendance summary for a student")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getAttendanceSummary(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam UUID studentId,
            @RequestParam UUID termId) {
        AttendanceSummaryResponse response = attendanceService.getSummary(studentId, termId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary/class/{classId}")
    @Operation(summary = "Get attendance summary for a class")
    public ResponseEntity<ApiResponse<List<AttendanceSummaryResponse>>> getClassAttendanceSummary(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID classId,
            @RequestParam UUID termId) {
        List<AttendanceSummaryResponse> response = attendanceService.getByClassAndTerm(classId, termId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}