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
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.course.dto.CreateLiveClassRequest;
import tz.elmkusoma.course.dto.LiveClassResponse;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.course.service.LiveClassService;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/teachers/me/live-classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Live Classes", description = "Manage live classes for teachers")
public class TeacherLiveClassController {

    private final LiveClassService liveClassService;
    private final LiveClassRepository liveClassRepository;
    private final TeacherRepository teacherRepository;
    private final NotificationService notificationService;
    private final LiveClassParticipantRepository participantRepository;

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
            @Valid @RequestBody CreateLiveClassRequest request) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));
        LiveClassResponse created = liveClassService.createLiveClass(teacher.getId(), institutionId, request);

        String schedInfo = created.getScheduledAt() != null
                ? " on " + created.getScheduledAt()
                : "";
        notificationService.notifyInstitutionStudentsExcluding(
                institutionId, userId,
                "New Live Class Scheduled",
                "A new live class \"" + created.getTitle() + "\"" + schedInfo + " has been scheduled.",
                "LIVE_CLASS_SCHEDULED", "live_class", created.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Live class created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a live class")
    public ResponseEntity<ApiResponse<LiveClassResponse>> updateLiveClass(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateLiveClassRequest request) {
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

        LiveClass liveClass = liveClassRepository.findById(id).orElse(null);
        if (liveClass != null) {
            notificationService.notifyInstitutionStudentsExcluding(
                    institutionId, userId,
                    "Live Class Cancelled",
                    "Your live class \"" + liveClass.getTitle() + "\" has been cancelled.",
                    "LIVE_CLASS_CANCELLED", "live_class", id);
        }

        return ResponseEntity.ok(ApiResponse.success("Live class cancelled successfully", null));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start a live session (SCHEDULED -> IN_PROGRESS)")
    public ResponseEntity<ApiResponse<LiveClassResponse>> startSession(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));
        LiveClassResponse started = liveClassService.startSession(teacher.getId(), id);

        notificationService.notifyInstitutionStudentsExcluding(
                institutionId, userId,
                "Live Class Started",
                "Your live class \"" + started.getTitle() + "\" has started. Join now!",
                "LIVE_CLASS_STARTED", "live_class", started.getId());

        return ResponseEntity.ok(ApiResponse.success("Live session started", started));
    }

    @PostMapping("/{id}/end")
    @Operation(summary = "End a live session (IN_PROGRESS -> COMPLETED)")
    public ResponseEntity<ApiResponse<LiveClassResponse>> endSession(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));
        LiveClassResponse ended = liveClassService.endSession(teacher.getId(), id);

        LiveClass liveClass = liveClassRepository.findById(id).orElse(null);
        if (liveClass != null) {
            notificationService.notifyInstitutionStudentsExcluding(
                    institutionId, userId,
                    "Live Class Ended",
                    "Your live class \"" + liveClass.getTitle() + "\" has ended.",
                    "LIVE_CLASS_COMPLETED", "live_class", ended.getId());
        }

        return ResponseEntity.ok(ApiResponse.success("Live session ended", ended));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get live class detail")
    public ResponseEntity<ApiResponse<LiveClassResponse>> getLiveClass(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));

        LiveClass liveClass = liveClassRepository.findById(id)
                .filter(lc -> lc.getTeacherId().equals(teacher.getId()) && !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("LiveClass", "id", id));

        LiveClassResponse response = liveClassService.getLiveClassById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "Get participants for a live class")
    public ResponseEntity<ApiResponse<List<LiveClassParticipant>>> getParticipants(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));
        List<LiveClassParticipant> participants = participantRepository.findByLiveClassIdAndIsDeletedFalse(id);
        return ResponseEntity.ok(ApiResponse.success(participants));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get participation stats for a live class")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getParticipantStats(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));
        long totalJoined = participantRepository.countByLiveClassIdAndIsDeletedFalse(id);
        long currentlyConnected = participantRepository.countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalJoined", totalJoined,
                "currentlyConnected", currentlyConnected
        )));
    }
}
