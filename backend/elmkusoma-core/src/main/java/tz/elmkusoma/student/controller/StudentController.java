package tz.elmkusoma.student.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody StudentRequest request) {
        StudentResponse student = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student created", student));
    }

    @GetMapping
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
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        StudentResponse student = studentService.getStudent(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(student));
    }

    @GetMapping("/admission/{admissionNumber}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentByAdmission(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable String admissionNumber) {
        StudentResponse student = studentService.getStudentByAdmissionNumber(admissionNumber, institutionId);
        return ResponseEntity.ok(ApiResponse.success(student));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody StudentRequest request) {
        StudentResponse student = studentService.updateStudent(id, institutionId, request);
        return ResponseEntity.ok(ApiResponse.success("Student updated", student));
    }

    @PostMapping("/{id}/assign-class")
    public ResponseEntity<ApiResponse<StudentClassAssignment>> assignToClass(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody AssignClassRequest request) {
        StudentClassAssignment assignment = studentService.assignToClass(id, institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student assigned to class", assignment));
    }

    @GetMapping("/stats/count")
    public ResponseEntity<ApiResponse<Long>> countStudents(
            @RequestAttribute("institutionId") UUID institutionId) {
        long count = studentService.countStudents(institutionId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/stats/active")
    public ResponseEntity<ApiResponse<Long>> countActiveStudents(
            @RequestAttribute("institutionId") UUID institutionId) {
        long count = studentService.countActiveStudents(institutionId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
