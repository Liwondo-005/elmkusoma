package tz.elmkusoma.learning.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.learning.service.SecondaryExtendedService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/secondary/extended")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN','STUDENT')")
@Tag(name = "Secondary Extended", description = "Concept Bank, Problem Bank, Error Bank, Study Planner")
public class SecondaryExtendedController {

    private final SecondaryExtendedService service;

    // ==================== CONCEPTS ====================

    @PostMapping("/concepts")
    @Operation(summary = "Create a concept")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createConcept(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Concept created", service.createConcept(institutionId, userId, request)));
    }

    @GetMapping("/concepts")
    @Operation(summary = "Get concepts by subject")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConceptsBySubject(@RequestParam UUID subjectId) {
        return ResponseEntity.ok(ApiResponse.success(service.getConceptsBySubject(subjectId)));
    }

    @GetMapping("/concepts/class/{classGroupId}")
    @Operation(summary = "Get concepts by class group")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConceptsByClass(@PathVariable UUID classGroupId) {
        return ResponseEntity.ok(ApiResponse.success(service.getConceptsByClass(classGroupId)));
    }

    @PutMapping("/concepts/{id}")
    @Operation(summary = "Update concept")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateConcept(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success("Concept updated", service.updateConcept(id, request)));
    }

    @DeleteMapping("/concepts/{id}")
    @Operation(summary = "Delete concept")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteConcept(@PathVariable UUID id) {
        service.deleteConcept(id);
        return ResponseEntity.ok(ApiResponse.success("Concept deleted", null));
    }

    // ==================== PROBLEMS ====================

    @PostMapping("/problems")
    @Operation(summary = "Create a problem")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createProblem(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Problem created", service.createProblem(institutionId, userId, request)));
    }

    @GetMapping("/problems")
    @Operation(summary = "Get problems by subject")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProblemsBySubject(@RequestParam UUID subjectId) {
        return ResponseEntity.ok(ApiResponse.success(service.getProblemsBySubject(subjectId)));
    }

    @GetMapping("/problems/class/{classGroupId}")
    @Operation(summary = "Get problems by class group")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProblemsByClass(@PathVariable UUID classGroupId) {
        return ResponseEntity.ok(ApiResponse.success(service.getProblemsByClass(classGroupId)));
    }

    @PutMapping("/problems/{id}")
    @Operation(summary = "Update problem")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProblem(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success("Problem updated", service.updateProblem(id, request)));
    }

    @DeleteMapping("/problems/{id}")
    @Operation(summary = "Delete problem")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteProblem(@PathVariable UUID id) {
        service.deleteProblem(id);
        return ResponseEntity.ok(ApiResponse.success("Problem deleted", null));
    }

    // ==================== ERRORS ====================

    @PostMapping("/errors")
    @Operation(summary = "Create an error entry")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createError(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Error entry created", service.createError(institutionId, userId, request)));
    }

    @GetMapping("/errors")
    @Operation(summary = "Get errors by subject")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getErrorsBySubject(@RequestParam UUID subjectId) {
        return ResponseEntity.ok(ApiResponse.success(service.getErrorsBySubject(subjectId)));
    }

    @GetMapping("/errors/class/{classGroupId}")
    @Operation(summary = "Get errors by class group")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getErrorsByClass(@PathVariable UUID classGroupId) {
        return ResponseEntity.ok(ApiResponse.success(service.getErrorsByClass(classGroupId)));
    }

    @PutMapping("/errors/{id}")
    @Operation(summary = "Update error entry")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateError(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success("Error entry updated", service.updateError(id, request)));
    }

    @DeleteMapping("/errors/{id}")
    @Operation(summary = "Delete error entry")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteError(@PathVariable UUID id) {
        service.deleteError(id);
        return ResponseEntity.ok(ApiResponse.success("Error entry deleted", null));
    }

    // ==================== STUDY PLANNER ====================

    @PostMapping("/study-planner")
    @Operation(summary = "Create a study plan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createStudyPlan(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Study plan created", service.createStudyPlan(institutionId, userId, request)));
    }

    @GetMapping("/study-planner/student/{studentId}")
    @Operation(summary = "Get study plans by student")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStudyPlansByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(service.getStudyPlansByStudent(studentId)));
    }

    @GetMapping("/study-planner/student/{studentId}/range")
    @Operation(summary = "Get study plans by student and date range")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStudyPlansByDateRange(
            @PathVariable UUID studentId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(ApiResponse.success(service.getStudyPlansByStudentAndDateRange(studentId, startDate, endDate)));
    }

    @PutMapping("/study-planner/{id}")
    @Operation(summary = "Update study plan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStudyPlan(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success("Study plan updated", service.updateStudyPlan(id, request)));
    }

    @DeleteMapping("/study-planner/{id}")
    @Operation(summary = "Delete study plan")
    public ResponseEntity<ApiResponse<Void>> deleteStudyPlan(@PathVariable UUID id) {
        service.deleteStudyPlan(id);
        return ResponseEntity.ok(ApiResponse.success("Study plan deleted", null));
    }
}
