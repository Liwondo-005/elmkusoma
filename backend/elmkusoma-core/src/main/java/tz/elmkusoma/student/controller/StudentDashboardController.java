package tz.elmkusoma.student.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.student.service.StudentDashboardService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/student/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student Dashboard", description = "Student dashboard summary, results, and attendance")
public class StudentDashboardController {

    private final StudentDashboardService studentDashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get student dashboard summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary(
            @RequestAttribute("userId") UUID userId) {
        Map<String, Object> summary = studentDashboardService.getDashboardSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/continue-learning")
    @Operation(summary = "Get continue learning items (in-progress lessons)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getContinueLearning(
            @RequestAttribute("userId") UUID userId) {
        List<Map<String, Object>> items = studentDashboardService.getContinueLearning(userId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/recent-activity")
    @Operation(summary = "Get recent activity")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivity(
            @RequestAttribute("userId") UUID userId) {
        List<Map<String, Object>> activities = studentDashboardService.getRecentActivity(userId);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    @GetMapping("/results")
    @Operation(summary = "Get student results (report cards with subject grades)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStudentResults(
            @RequestAttribute("userId") UUID userId) {
        List<Map<String, Object>> results = studentDashboardService.getStudentResults(userId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/attendance")
    @Operation(summary = "Get attendance summary and records")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendanceSummary(
            @RequestAttribute("userId") UUID userId) {
        Map<String, Object> summary = studentDashboardService.getAttendanceSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/live-classes")
    @Operation(summary = "Get upcoming live classes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUpcomingLiveClasses(
            @RequestAttribute("userId") UUID userId) {
        List<Map<String, Object>> classes = studentDashboardService.getUpcomingLiveClasses(userId);
        return ResponseEntity.ok(ApiResponse.success(classes));
    }
}
