package tz.elmkusoma.highereducation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.dto.DepartmentDTO;
import tz.elmkusoma.highereducation.service.DepartmentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/departments")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "CRUD operations for higher education departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "List all departments in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> listDepartments(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<DepartmentDTO> departments = departmentService.listByInstitution(institutionId);
        return ResponseEntity.ok(ApiResponse.success(departments));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a department by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getDepartment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        DepartmentDTO department = departmentService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(department));
    }

    @GetMapping("/count")
    @Operation(summary = "Count departments in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countDepartments(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        long count = departmentService.countByInstitution(institutionId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping
    @Operation(summary = "Create a new department")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> createDepartment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody DepartmentDTO request) {
        DepartmentDTO department = departmentService.create(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Department created successfully", department));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a department")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> updateDepartment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentDTO request) {
        DepartmentDTO department = departmentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Department updated successfully", department));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a department")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Department deleted successfully", null));
    }
}
