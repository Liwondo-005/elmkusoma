package tz.elmkusoma.parent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.parent.dto.response.*;
import tz.elmkusoma.parent.service.ParentDashboardService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/my")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "Parent Self-Service", description = "Parent dashboard and family management endpoints")
public class ParentSelfController {

    private final ParentDashboardService dashboardService;

    private UUID getCurrentUserId(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr instanceof UUID uuid) {
            return uuid;
        }
        throw new RuntimeException("User not authenticated");
    }

    @GetMapping("/overview")
    @Operation(summary = "Get family overview with all children and today's actions")
    public ResponseEntity<ApiResponse<FamilyOverviewResponse>> getFamilyOverview(
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        FamilyOverviewResponse overview = dashboardService.getFamilyOverview(userId);
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/children")
    @Operation(summary = "Get all children linked to the authenticated parent")
    public ResponseEntity<ApiResponse<List<ChildOverviewResponse>>> getMyChildren(
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        List<ChildOverviewResponse> children = dashboardService.getMyChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/children/{studentId}")
    @Operation(summary = "Get detailed overview for a specific child")
    public ResponseEntity<ApiResponse<ChildOverviewResponse>> getChildOverview(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ChildOverviewResponse overview = dashboardService.getChildOverview(userId, studentId);
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/children/{studentId}/attendance")
    @Operation(summary = "Get attendance details for a specific child")
    public ResponseEntity<ApiResponse<ParentAttendanceResponse>> getChildAttendance(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentAttendanceResponse attendance = dashboardService.getChildAttendance(userId, studentId);
        return ResponseEntity.ok(ApiResponse.success(attendance));
    }

    @GetMapping("/children/{studentId}/assignments")
    @Operation(summary = "Get assignments for a specific child")
    public ResponseEntity<ApiResponse<ParentAssignmentResponse>> getChildAssignments(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentAssignmentResponse assignments = dashboardService.getChildAssignments(userId, studentId);
        return ResponseEntity.ok(ApiResponse.success(assignments));
    }

    @GetMapping("/children/{studentId}/results")
    @Operation(summary = "Get results and report cards for a specific child")
    public ResponseEntity<ApiResponse<ParentResultResponse>> getChildResults(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentResultResponse results = dashboardService.getChildResults(userId, studentId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
