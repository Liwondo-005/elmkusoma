package tz.elmkusoma.student.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.dto.AssignClassRequest;
import tz.elmkusoma.student.dto.StudentRequest;
import tz.elmkusoma.student.dto.StudentResponse;
import tz.elmkusoma.student.service.StudentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/students")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody StudentRequest request) {
        StudentResponse student = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student created", student));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudents(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) String query) {
        List<StudentResponse> students;
        if (query != null && !query.isBlank()) {
            students = studentService.searchStudents(institutionId, query);
        } else {
            students = studentService.getStudents(institutionId, classId);
        }
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(
            @PathVariable UUID id, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if ("STUDENT".equals(role)) {
            UUID userId = (UUID) request.getAttribute("userId");
            StudentResponse student = studentService.getStudent(id);
            if (!id.equals(studentService.getStudentIdByUserId(userId))) {
                throw new AccessDeniedException("You can only view your own profile");
            }
            return ResponseEntity.ok(ApiResponse.success(student));
        }
        StudentResponse student = studentService.getStudent(id);
        return ResponseEntity.ok(ApiResponse.success(student));
    }

    @GetMapping("/admission/{admissionNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentByAdmission(
            @PathVariable String admissionNumber) {
        StudentResponse student = studentService.getStudentByAdmissionNumber(admissionNumber);
        return ResponseEntity.ok(ApiResponse.success(student));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable UUID id,
            @Valid @RequestBody StudentRequest request) {
        StudentResponse student = studentService.updateStudent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Student updated", student));
    }

    @PostMapping("/{id}/assign-class")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<StudentClassAssignment>> assignToClass(
            @PathVariable UUID id,
            @Valid @RequestBody AssignClassRequest request) {
        StudentClassAssignment assignment = studentService.assignToClass(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student assigned to class", assignment));
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countStudents(
            @RequestAttribute("institutionId") UUID institutionId) {
        long count = studentService.countStudents(institutionId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/stats/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countActiveStudents(
            @RequestAttribute("institutionId") UUID institutionId) {
        long count = studentService.countActiveStudents(institutionId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
