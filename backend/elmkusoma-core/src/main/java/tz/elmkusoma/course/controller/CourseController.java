package tz.elmkusoma.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.dto.*;
import tz.elmkusoma.course.service.CourseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "Full CRUD for courses, modules, and lessons")
public class CourseController {

    private final CourseService courseService;

    // ── Course Endpoints ──

    @PostMapping
    @Operation(summary = "Create a new course")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        CourseResponse response = courseService.createCourse(request, institutionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Course created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all courses for institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCourses(
            @RequestAttribute("institutionId") UUID institutionId) {
        List<CourseResponse> response = courseService.getCourses(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Get a specific course")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(
            @PathVariable UUID courseId,
            @RequestAttribute("institutionId") UUID institutionId) {
        CourseResponse response = courseService.getCourseById(courseId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{courseId}")
    @Operation(summary = "Update a course")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseRequest request,
            @RequestAttribute("institutionId") UUID institutionId) {
        CourseResponse response = courseService.updateCourse(courseId, request, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Course updated successfully", response));
    }

    @DeleteMapping("/{courseId}")
    @Operation(summary = "Delete a course")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @PathVariable UUID courseId,
            @RequestAttribute("institutionId") UUID institutionId) {
        courseService.deleteCourse(courseId, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Course deleted successfully", null));
    }

    @PostMapping("/{courseId}/toggle-publish")
    @Operation(summary = "Toggle course publish status")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> togglePublish(
            @PathVariable UUID courseId,
            @RequestAttribute("institutionId") UUID institutionId) {
        CourseResponse response = courseService.togglePublish(courseId, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Course publish status toggled", response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get course statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CourseStatsResponse>> getCourseStats(
            @RequestAttribute("institutionId") UUID institutionId) {
        CourseStatsResponse response = courseService.getCourseStats(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Module Endpoints ──

    @PostMapping("/{courseId}/modules")
    @Operation(summary = "Create a course module")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CourseModuleResponse>> createModule(
            @Valid @RequestBody CourseModuleRequest request,
            @PathVariable UUID courseId,
            @RequestAttribute("institutionId") UUID institutionId) {
        CourseModuleResponse response = courseService.createModule(request, courseId, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Module created successfully", response));
    }

    @GetMapping("/{courseId}/modules")
    @Operation(summary = "List modules for a course")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<CourseModuleResponse>>> getModules(
            @PathVariable UUID courseId) {
        List<CourseModuleResponse> response = courseService.getModules(courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/modules/{moduleId}")
    @Operation(summary = "Delete a course module")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteModule(
            @PathVariable UUID moduleId,
            @RequestAttribute("institutionId") UUID institutionId) {
        courseService.deleteModule(moduleId, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Module deleted successfully", null));
    }

    // ── Lesson Endpoints ──

    @PostMapping("/modules/{moduleId}/lessons")
    @Operation(summary = "Create a course lesson")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CourseLessonResponse>> createLesson(
            @Valid @RequestBody CourseLessonRequest request,
            @PathVariable UUID moduleId,
            @RequestAttribute("institutionId") UUID institutionId) {
        CourseLessonResponse response = courseService.createLesson(request, moduleId, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lesson created successfully", response));
    }

    @GetMapping("/modules/{moduleId}/lessons")
    @Operation(summary = "List lessons for a module")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<CourseLessonResponse>>> getLessons(
            @PathVariable UUID moduleId) {
        List<CourseLessonResponse> response = courseService.getLessons(moduleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/lessons/{lessonId}")
    @Operation(summary = "Delete a course lesson")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @PathVariable UUID lessonId,
            @RequestAttribute("institutionId") UUID institutionId) {
        courseService.deleteLesson(lessonId, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Lesson deleted successfully", null));
    }
}
