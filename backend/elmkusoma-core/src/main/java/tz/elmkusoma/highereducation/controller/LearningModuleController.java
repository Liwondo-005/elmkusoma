package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.dto.LearningModuleDTO;
import tz.elmkusoma.highereducation.service.LearningModuleService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/college/learner/modules")
@RequiredArgsConstructor
public class LearningModuleController {

    private final LearningModuleService learningModuleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LearningModuleDTO>> createModule(
            @RequestBody LearningModuleDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Module created", learningModuleService.createModule(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LearningModuleDTO>> getModule(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(learningModuleService.getModule(id)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<LearningModuleDTO>>> getStudentModules(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(learningModuleService.getStudentModules(studentId)));
    }

    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<LearningModuleDTO>>> getStudentCourseModules(
            @PathVariable UUID studentId, @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(learningModuleService.getStudentCourseModules(studentId, courseId)));
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<LearningModuleDTO>>> getInstitutionModules(
            @PathVariable UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(learningModuleService.getInstitutionModules(institutionId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LearningModuleDTO>> updateModule(
            @PathVariable UUID id, @RequestBody LearningModuleDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Module updated", learningModuleService.updateModule(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteModule(@PathVariable UUID id) {
        learningModuleService.deleteModule(id);
        return ResponseEntity.ok(ApiResponse.success("Module deleted", null));
    }
}
