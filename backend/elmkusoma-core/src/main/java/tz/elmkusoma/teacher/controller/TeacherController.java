package tz.elmkusoma.teacher.controller;

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
    private final tz.elmkusoma.teacher.repository.TeacherRepository teacherRepository;

    @GetMapping("/me/profile")
    @Operation(summary = "Get current teacher's profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TeacherResponse>> getMyProfile(
            // Optional: an independent (school-less) teacher resolves no institution at all.
            // Affiliated teachers keep sending the header; a mismatching one is still rejected
            // by OrganizationContextResolver before this method runs.
            @RequestHeader(value = "X-Institution-Id", required = false) UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        TeacherResponse teacher = teacherService.getTeacherByUserId(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(teacher));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update current teacher's profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TeacherResponse>> updateMyProfile(
            // Optional, like GET /me/profile: an independent teacher has no institution to declare.
            @RequestHeader(value = "X-Institution-Id", required = false) UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestBody TeacherRequest request) {
        TeacherResponse teacher = teacherService.getTeacherByUserId(userId, institutionId);
        TeacherResponse updated = teacherService.updateTeacher(institutionId, teacher.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @GetMapping("/me/qualifications")
    @Operation(summary = "Get current teacher's qualifications (works for independent teachers too)")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<TeacherQualificationResponse>>> getMyQualifications(
            @RequestHeader(value = "X-Institution-Id", required = false) UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        // Keyed by the caller's own teacher row, so no institution-scoped path parameter
        // blocks an independent (school-less) teacher from their own records.
        tz.elmkusoma.teacher.domain.Teacher teacher =
                teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(
                teacherService.getQualifications(teacher.getInstitutionId(), teacher.getId())));
    }

    @PostMapping("/me/qualifications")
    @Operation(summary = "Add a qualification to current teacher's profile (works for independent teachers too)")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TeacherQualificationResponse>> addMyQualification(
            @RequestHeader(value = "X-Institution-Id", required = false) UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody TeacherQualificationRequest request) {
        tz.elmkusoma.teacher.domain.Teacher teacher =
                teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        TeacherQualificationResponse qualification = teacherService.addQualification(
                teacher.getInstitutionId(), teacher.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Qualification added successfully", qualification));
    }

    @GetMapping("/me/classes")
    @Operation(summary = "Get current teacher's assigned classes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<TeacherClassResponse>>> getMyClasses(
            @RequestHeader(value = "X-Institution-Id", required = false) UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        List<TeacherClassResponse> classes = teacherService.getTeacherClasses(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(classes));
    }

    @GetMapping("/me/students")
    @Operation(summary = "Get students in current teacher's assigned classes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<TeacherStudentResponse>>> getMyStudents(
            @RequestHeader(value = "X-Institution-Id", required = false) UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        List<TeacherStudentResponse> students = teacherService.getTeacherStudents(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/me/dashboard")
    @Operation(summary = "Get current teacher's dashboard data")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TeacherDashboardResponse>> getMyDashboard(
            @RequestHeader(value = "X-Institution-Id", required = false) UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        TeacherDashboardResponse dashboard = teacherService.getTeacherDashboard(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PostMapping
    @Operation(summary = "Create a new teacher profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherResponse>> createTeacher(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody TeacherRequest request) {
        TeacherResponse teacher = teacherService.createTeacher(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Teacher created successfully", teacher));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a teacher by ID")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherResponse>> getTeacher(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            TeacherResponse me = teacherService.getTeacherByUserId(userId, institutionId);
            if (!me.getId().equals(id)) {
                throw new tz.elmkusoma.exception.ForbiddenException("Cannot access another teacher's profile");
            }
        }
        TeacherResponse teacher = teacherService.getTeacher(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(teacher));
    }

    @GetMapping
    @Operation(summary = "List all teachers in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<TeacherResponse>>> listTeachers(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<TeacherResponse> teachers = teacherService.listTeachers(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(teachers));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a teacher profile")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherResponse>> updateTeacher(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody TeacherRequest request,
            @RequestAttribute("userId") UUID userId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            TeacherResponse me = teacherService.getTeacherByUserId(userId, institutionId);
            if (!me.getId().equals(id)) {
                throw new tz.elmkusoma.exception.ForbiddenException("Cannot update another teacher's profile");
            }
        }
        TeacherResponse teacher = teacherService.updateTeacher(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Teacher updated successfully", teacher));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a teacher")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        teacherService.deleteTeacher(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Teacher deleted successfully", null));
    }

    @PostMapping("/{id}/assignments")
    @Operation(summary = "Add a class-subject assignment to a teacher")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
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
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<TeacherAssignmentResponse>>> getAssignments(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            TeacherResponse me = teacherService.getTeacherByUserId(userId, institutionId);
            if (!me.getId().equals(id)) {
                throw new tz.elmkusoma.exception.ForbiddenException("Cannot access another teacher's assignments");
            }
        }
        List<TeacherAssignmentResponse> assignments = teacherService.getAssignments(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(assignments));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @Operation(summary = "Remove a teacher assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeAssignment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID assignmentId) {
        teacherService.removeAssignment(institutionId, assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Assignment removed successfully", null));
    }

    @PostMapping("/assignments/{assignmentId}/end")
    @Operation(summary = "End a teacher assignment (reassignment keeps the history row)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> endAssignment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID assignmentId) {
        TeacherAssignmentResponse assignment = teacherService.endAssignment(institutionId, assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Assignment ended successfully", assignment));
    }

    @PostMapping("/{id}/qualifications")
    @Operation(summary = "Add a qualification to a teacher")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherQualificationResponse>> addQualification(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody TeacherQualificationRequest request,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        verifyTeacherAccess(id, serverInstitutionId, userRole);
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            TeacherResponse me = teacherService.getTeacherByUserId(userId, institutionId);
            if (!me.getId().equals(id)) {
                throw new tz.elmkusoma.exception.ForbiddenException("Cannot add qualifications to another teacher's profile");
            }
        }
        TeacherQualificationResponse qualification = teacherService.addQualification(institutionId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Qualification added successfully", qualification));
    }

    @GetMapping("/{id}/qualifications")
    @Operation(summary = "Get all qualifications for a teacher")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<TeacherQualificationResponse>>> getQualifications(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        verifyTeacherAccess(id, serverInstitutionId, userRole);
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            TeacherResponse me = teacherService.getTeacherByUserId(userId, institutionId);
            if (!me.getId().equals(id)) {
                throw new tz.elmkusoma.exception.ForbiddenException("Cannot access another teacher's qualifications");
            }
        }
        List<TeacherQualificationResponse> qualifications = teacherService.getQualifications(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(qualifications));
    }

    @DeleteMapping("/qualifications/{qualificationId}")
    @Operation(summary = "Remove a teacher qualification")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeQualification(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID qualificationId) {
        teacherService.removeQualification(institutionId, qualificationId);
        return ResponseEntity.ok(ApiResponse.success("Qualification removed successfully", null));
    }

    private void verifyTeacherAccess(UUID id, UUID serverInstitutionId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return;
        }
        // A missing row is a 404, not a silent pass-through: the previous ifPresent-skip let
        // an unknown id bypass the institution check entirely.
        tz.elmkusoma.teacher.domain.Teacher teacher = teacherRepository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException("Teacher", "id", id));
        if (teacher.getInstitutionId() == null || !teacher.getInstitutionId().equals(serverInstitutionId)) {
            throw new tz.elmkusoma.exception.ForbiddenException("Teacher", "access");
        }
    }
}
