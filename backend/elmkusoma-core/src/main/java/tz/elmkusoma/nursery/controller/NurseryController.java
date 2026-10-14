package tz.elmkusoma.nursery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.nursery.dto.request.CreateNurseryActivityRequest;
import tz.elmkusoma.nursery.dto.request.CreateNurseryMilestoneRequest;
import tz.elmkusoma.nursery.dto.response.NurseryActivityResponse;
import tz.elmkusoma.nursery.dto.response.NurseryMilestoneResponse;
import tz.elmkusoma.nursery.service.NurseryActivityService;
import tz.elmkusoma.nursery.service.NurseryMilestoneService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/nursery")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
@Tag(name = "Nursery", description = "Nursery activities and milestone tracking")
public class NurseryController {

    private final NurseryActivityService nurseryActivityService;
    private final NurseryMilestoneService nurseryMilestoneService;

    @PostMapping("/activities")
    @Operation(summary = "Create a nursery activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<NurseryActivityResponse>> createActivity(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader(value = "X-User-Id", required = false) UUID legacyUserId,
            @RequestAttribute("userId") UUID conductedBy,
            @Valid @RequestBody CreateNurseryActivityRequest request) {
        NurseryActivityResponse response = nurseryActivityService.create(institutionId, conductedBy, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nursery activity created successfully", response));
    }

    @GetMapping("/activities")
    @Operation(summary = "Get nursery activities by class")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<List<NurseryActivityResponse>>> getActivitiesByClass(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam UUID classId) {
        List<NurseryActivityResponse> response = nurseryActivityService.getByClassGroupId(institutionId, classId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/activities/class/{classId}/date/{date}")
    @Operation(summary = "Get nursery activities by class and date")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<List<NurseryActivityResponse>>> getActivitiesByClassAndDate(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID classId,
            @PathVariable @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        List<NurseryActivityResponse> response = nurseryActivityService.getByClassAndDate(institutionId, classId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/activities/{id}")
    @Operation(summary = "Get nursery activity by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<NurseryActivityResponse>> getActivity(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        NurseryActivityResponse response = nurseryActivityService.getById(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/activities/{id}")
    @Operation(summary = "Update nursery activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<NurseryActivityResponse>> updateActivity(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateNurseryActivityRequest request) {
        NurseryActivityResponse response = nurseryActivityService.update(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Nursery activity updated successfully", response));
    }

    @DeleteMapping("/activities/{id}")
    @Operation(summary = "Delete nursery activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        nurseryActivityService.delete(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Nursery activity deleted successfully", null));
    }

    @GetMapping("/activities/type/{type}")
    @Operation(summary = "Get nursery activities by type")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<List<NurseryActivityResponse>>> getActivitiesByType(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable String type) {
        List<NurseryActivityResponse> response = nurseryActivityService.getByType(institutionId, type);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/milestones")
    @Operation(summary = "Create a nursery milestone")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<NurseryMilestoneResponse>> createMilestone(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody CreateNurseryMilestoneRequest request) {
        NurseryMilestoneResponse response = nurseryMilestoneService.create(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nursery milestone created successfully", response));
    }

    @GetMapping("/milestones/student/{studentId}")
    @Operation(summary = "Get milestones by student")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<List<NurseryMilestoneResponse>>> getMilestonesByStudent(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID studentId) {
        List<NurseryMilestoneResponse> response = nurseryMilestoneService.getByStudentId(institutionId, studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/milestones/student/{studentId}/category/{category}")
    @Operation(summary = "Get milestones by student and category")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<List<NurseryMilestoneResponse>>> getMilestonesByStudentAndCategory(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID studentId,
            @PathVariable String category) {
        List<NurseryMilestoneResponse> response = nurseryMilestoneService.getByStudentAndCategory(institutionId, studentId, category);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/milestones/{id}")
    @Operation(summary = "Get milestone by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<NurseryMilestoneResponse>> getMilestone(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        NurseryMilestoneResponse response = nurseryMilestoneService.getById(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/milestones/{id}")
    @Operation(summary = "Update milestone")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<NurseryMilestoneResponse>> updateMilestone(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateNurseryMilestoneRequest request) {
        NurseryMilestoneResponse response = nurseryMilestoneService.update(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Milestone updated successfully", response));
    }

    @DeleteMapping("/milestones/{id}")
    @Operation(summary = "Delete milestone")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        nurseryMilestoneService.delete(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Milestone deleted successfully", null));
    }
}
