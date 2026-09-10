package tz.elmkusoma.teacher.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.teacher.dto.request.TeacherAssignmentRequest;
import tz.elmkusoma.teacher.dto.request.TeacherQualificationRequest;
import tz.elmkusoma.teacher.dto.request.TeacherRequest;
import tz.elmkusoma.teacher.dto.response.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher Management", description = "CRUD operations for teachers, assignments, and qualifications")
public class TeacherController {

    private final tz.elmkusoma.teacher.service.TeacherService teacherService;

    @GetMapping("/me/profile")
    @Operation(summary = "Get current teacher's profile")
    public ResponseEntity<ApiResponse<TeacherResponse>> getMyProfile(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        TeacherResponse teacher = teacherService.getTeacherByUserId(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(teacher));
    }

    @GetMapping("/me/classes")
    @Operation(summary = "Get current teacher's assigned classes")
    public ResponseEntity<ApiResponse<List<TeacherClassResponse>>> getMyClasses(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        List<TeacherClassResponse> classes = teacherService.getTeacherClasses(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(classes));
    }

    @GetMapping("/me/students")
    @Operation(summary = "Get students in current teacher's assigned classes")
    public ResponseEntity<ApiResponse<List<TeacherStudentResponse>>> getMyStudents(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        List<TeacherStudentResponse> students = teacherService.getTeacherStudents(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/me/dashboard")
    @Operation(summary = "Get current teacher's dashboard data")
    public ResponseEntity<ApiResponse<TeacherDashboardResponse>> getMyDashboard(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        TeacherDashboardResponse dashboard = teacherService.getTeacherDashboard(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PostMapping
    @Operation(summary = "Create a new teacher profile")
    public ResponseEntity<ApiResponse<TeacherResponse>> createTeacher(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody TeacherRequest request) {
        TeacherResponse teacher = teacherService.createTeacher(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Teacher created successfully", teacher));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a teacher by ID")
    public ResponseEntity<ApiResponse<TeacherResponse>> getTeacher(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        TeacherResponse teacher = teacherService.getTeacher(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(teacher));
    }

    @GetMapping
    @Operation(summary = "List all teachers in an institution")
    public ResponseEntity<ApiResponse<PageResponse<TeacherResponse>>> listTeachers(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<TeacherResponse> teachers = teacherService.listTeachers(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(teachers));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a teacher profile")
    public ResponseEntity<ApiResponse<TeacherResponse>> updateTeacher(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody TeacherRequest request) {
        TeacherResponse teacher = teacherService.updateTeacher(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Teacher updated successfully", teacher));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a teacher")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        teacherService.deleteTeacher(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Teacher deleted successfully", null));
    }

    @PostMapping("/{id}/assignments")
    @Operation(summary = "Add a class-subject assignment to a teacher")
    public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> addAssignment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody TeacherAssignmentRequest request) {
        TeacherAssignmentResponse assignment = teacherService.addAssignment(institutionId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment added successfully", assignment));
    }

    @GetMapping("/{id}/assignments")
    @Operation(summary = "Get all assignments for a teacher")
    public ResponseEntity<ApiResponse<List<TeacherAssignmentResponse>>> getAssignments(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        List<TeacherAssignmentResponse> assignments = teacherService.getAssignments(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(assignments));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @Operation(summary = "Remove a teacher assignment")
    public ResponseEntity<ApiResponse<Void>> removeAssignment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID assignmentId) {
        teacherService.removeAssignment(institutionId, assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Assignment removed successfully", null));
    }

    @PostMapping("/{id}/qualifications")
    @Operation(summary = "Add a qualification to a teacher")
    public ResponseEntity<ApiResponse<TeacherQualificationResponse>> addQualification(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody TeacherQualificationRequest request) {
        TeacherQualificationResponse qualification = teacherService.addQualification(institutionId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Qualification added successfully", qualification));
    }

    @GetMapping("/{id}/qualifications")
    @Operation(summary = "Get all qualifications for a teacher")
    public ResponseEntity<ApiResponse<List<TeacherQualificationResponse>>> getQualifications(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        List<TeacherQualificationResponse> qualifications = teacherService.getQualifications(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(qualifications));
    }

    @DeleteMapping("/qualifications/{qualificationId}")
    @Operation(summary = "Remove a teacher qualification")
    public ResponseEntity<ApiResponse<Void>> removeQualification(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID qualificationId) {
        teacherService.removeQualification(institutionId, qualificationId);
        return ResponseEntity.ok(ApiResponse.success("Qualification removed successfully", null));
    }
}
