package tz.elmkusoma.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Student attendance management")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/mark")
    @Operation(summary = "Mark attendance for a student")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> markAttendance(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID markedBy,
            @Valid @RequestBody MarkAttendanceRequest request) {
        AttendanceRecordResponse response = attendanceService.markAttendance(institutionId, markedBy, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Attendance marked successfully"));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk mark attendance for a class")
    public ResponseEntity<ApiResponse<Void>> bulkMarkAttendance(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID markedBy,
            @Valid @RequestBody BulkMarkAttendanceRequest request) {
        attendanceService.markBulkAttendance(institutionId, markedBy, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Bulk attendance marked successfully"));
    }

    @GetMapping
    @Operation(summary = "Get attendance by class and date")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getAttendance(
            @RequestParam UUID classId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceRecordResponse> response = attendanceService.getByClassAndDate(classId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get attendance by student and date range")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getStudentAttendance(
            @PathVariable UUID studentId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceRecordResponse> response = attendanceService.getByStudentAndDateRange(studentId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get attendance summary for a student")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getAttendanceSummary(
            @RequestParam UUID studentId,
            @RequestParam UUID termId) {
        AttendanceSummaryResponse response = attendanceService.getSummary(studentId, termId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary/class/{classId}")
    @Operation(summary = "Get attendance summary for a class")
    public ResponseEntity<ApiResponse<List<AttendanceSummaryResponse>>> getClassAttendanceSummary(
            @PathVariable UUID classId,
            @RequestParam UUID termId) {
        List<AttendanceSummaryResponse> response = attendanceService.getByClassAndTerm(classId, termId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}