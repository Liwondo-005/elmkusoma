package tz.elmkusoma.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.attendance.dto.request.BulkMarkAttendanceRequest;
import tz.elmkusoma.attendance.dto.request.MarkAttendanceRequest;
import tz.elmkusoma.attendance.dto.response.AttendanceRecordResponse;
import tz.elmkusoma.attendance.dto.response.AttendanceSummaryResponse;
import tz.elmkusoma.attendance.service.AttendanceService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.student.repository.StudentRepository;

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
    private final StudentRepository studentRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;

    @PostMapping("/mark")
    @Operation(summary = "Mark attendance for a student")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> markAttendance(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID markedBy,
            @Valid @RequestBody MarkAttendanceRequest request) {
        AttendanceRecordResponse response = attendanceService.markAttendance(institutionId, markedBy, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attendance marked successfully", response));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk mark attendance for a class")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> bulkMarkAttendance(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID markedBy,
            @Valid @RequestBody BulkMarkAttendanceRequest request) {
        attendanceService.markBulkAttendance(institutionId, markedBy, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bulk attendance marked successfully", null));
    }

    @GetMapping
    @Operation(summary = "Get attendance by class and date")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getAttendance(
            @RequestParam UUID classId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceRecordResponse> response = attendanceService.getByClassAndDate(classId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get attendance by student and date range")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getStudentAttendance(
            @PathVariable UUID studentId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        verifyStudentAccess(studentId, request);
        List<AttendanceRecordResponse> response = attendanceService.getByStudentAndDateRange(studentId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get attendance summary for a student")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getAttendanceSummary(
            @RequestParam UUID studentId,
            @RequestParam UUID termId,
            HttpServletRequest request) {
        verifyStudentAccess(studentId, request);
        AttendanceSummaryResponse response = attendanceService.getSummary(studentId, termId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private void verifyStudentAccess(UUID studentId, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if ("STUDENT".equals(role)) {
            UUID userId = (UUID) request.getAttribute("userId");
            var student = studentRepository.findByUserIdAndIsDeletedFalse(userId).orElse(null);
            if (student == null || !student.getId().equals(studentId)) {
                throw new AccessDeniedException("You can only access your own attendance");
            }
        } else if ("PARENT".equals(role)) {
            UUID userId = (UUID) request.getAttribute("userId");
            var links = parentStudentLinkRepository.findAllByParentId(userId);
            boolean isChild = links.stream().anyMatch(link -> link.getStudentId().equals(studentId));
            if (!isChild) {
                throw new AccessDeniedException("You can only access your child's attendance");
            }
        }
    }

    @GetMapping("/summary/class/{classId}")
    @Operation(summary = "Get attendance summary for a class")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceSummaryResponse>>> getClassAttendanceSummary(
            @PathVariable UUID classId,
            @RequestParam UUID termId) {
        List<AttendanceSummaryResponse> response = attendanceService.getByClassAndTerm(classId, termId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
