package tz.elmkusoma.teacher.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.teacher.dto.response.TeacherAnalyticsResponse;
import tz.elmkusoma.teacher.service.TeacherAnalyticsService;

import java.util.UUID;

@RestController
@RequestMapping("/v1/teachers/me/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Analytics", description = "Analytics data for teachers")
public class TeacherAnalyticsController {

    private final TeacherAnalyticsService teacherAnalyticsService;

    @GetMapping
    @Operation(summary = "Get teacher analytics")
    public ResponseEntity<ApiResponse<TeacherAnalyticsResponse>> getAnalytics(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        TeacherAnalyticsResponse response = teacherAnalyticsService.getAnalytics(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
