package tz.elmkusoma.teacher.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.dto.CreateLiveClassRequest;
import tz.elmkusoma.course.dto.LiveClassResponse;
import tz.elmkusoma.course.service.LiveClassService;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/teachers/me/live-classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Live Classes", description = "Manage live classes for teachers")
public class TeacherLiveClassController {

    private final LiveClassService liveClassService;
    private final TeacherRepository teacherRepository;

    @GetMapping
    @Operation(summary = "List my live classes")
    public ResponseEntity<ApiResponse<List<LiveClassResponse>>> getMyLiveClasses(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));
        List<LiveClassResponse> classes = liveClassService.getTeacherLiveClasses(teacher.getId());
        return ResponseEntity.ok(ApiResponse.success(classes));
    }

    @PostMapping
    @Operation(summary = "Create a live class")
    public ResponseEntity<ApiResponse<LiveClassResponse>> createLiveClass(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestBody CreateLiveClassRequest request) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));
        LiveClassResponse created = liveClassService.createLiveClass(teacher.getId(), institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Live class created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a live class")
    public ResponseEntity<ApiResponse<LiveClassResponse>> updateLiveClass(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id,
            @RequestBody CreateLiveClassRequest request) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));
        LiveClassResponse updated = liveClassService.updateLiveClass(teacher.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Live class updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a live class")
    public ResponseEntity<ApiResponse<Void>> cancelLiveClass(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));
        liveClassService.cancelLiveClass(teacher.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Live class cancelled successfully", null));
    }
}
