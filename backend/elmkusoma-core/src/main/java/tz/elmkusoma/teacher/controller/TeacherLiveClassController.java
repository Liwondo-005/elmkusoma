package tz.elmkusoma.teacher.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.course.dto.CreateLiveClassRequest;
import tz.elmkusoma.course.dto.LiveClassResponse;
import tz.elmkusoma.course.dto.LinkLessonRequest;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.course.service.LiveClassService;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.teacher.service.TeacherService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/teachers/me/live-classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Live Classes", description = "Manage live classes for teachers")
@Slf4j
public class TeacherLiveClassController {

    private final LiveClassService liveClassService;
    private final LiveClassRepository liveClassRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherService teacherService;
    private final NotificationService notificationService;
    private final LiveClassParticipantRepository participantRepository;
    private final tz.elmkusoma.shared.repository.UserRepository userRepository;
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "List my live classes")
    public ResponseEntity<ApiResponse<List<LiveClassResponse>>> getMyLiveClasses(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        List<LiveClassResponse> classes = liveClassService.getTeacherLiveClasses(teacher.getId());
        return ResponseEntity.ok(ApiResponse.success(classes));
    }

    @PostMapping
    @Operation(summary = "Create a live class")
    public ResponseEntity<ApiResponse<LiveClassResponse>> createLiveClass(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "userEmail", required = false) String userEmail,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @Valid @RequestBody CreateLiveClassRequest request) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        LiveClassResponse created = liveClassService.createLiveClass(teacher.getId(), institutionId, request);

        String schedInfo = created.getScheduledAt() != null
                ? " on " + created.getScheduledAt()
                : "";
        try {
            notificationService.notifyInstitutionStudentsExcluding(
                    institutionId, userId,
                    "New Live Class Scheduled",
                    "A new live class \"" + created.getTitle() + "\"" + schedInfo + " has been scheduled.",
                    "LIVE_CLASS_SCHEDULED", "live_class", created.getId());
        } catch (Exception e) {
            log.warn("Failed to send live class scheduled notification: {}", e.getMessage());
        }

        auditSafely(institutionId, userId, userEmail, userRole, "live_class", created.getId(),
                created.getTitle(), AuditLog.AuditAction.CREATE, null, Map.of(
                        "title", String.valueOf(created.getTitle()),
                        "scheduledAt", String.valueOf(created.getScheduledAt()),
                        "lessonId", String.valueOf(created.getLessonId())));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Live class created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a live class")
    public ResponseEntity<ApiResponse<LiveClassResponse>> updateLiveClass(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "userEmail", required = false) String userEmail,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @PathVariable UUID id,
            @Valid @RequestBody CreateLiveClassRequest request) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        LiveClassResponse updated = liveClassService.updateLiveClass(teacher.getId(), id, request);

        LiveClass liveClass = liveClassRepository.findById(id).orElse(null);
        if (liveClass != null && request.getScheduledAt() != null) {
            try {
                notificationService.notifyInstitutionStudentsExcluding(
                        institutionId, userId,
                        "Live Class Rescheduled",
                        "The live class \"" + updated.getTitle() + "\" has been rescheduled to " + updated.getScheduledAt(),
                        "LIVE_CLASS_RESCHEDULED",
                        "LIVE_CLASS", id);
            } catch (Exception e) {
                log.warn("Failed to send live class rescheduled notification: {}", e.getMessage());
            }
        }

        auditSafely(institutionId, userId, userEmail, userRole, "live_class", id,
                updated.getTitle(), AuditLog.AuditAction.UPDATE, null, Map.of(
                        "title", String.valueOf(updated.getTitle()),
                        "scheduledAt", String.valueOf(updated.getScheduledAt()),
                        "lessonId", String.valueOf(updated.getLessonId())));

        return ResponseEntity.ok(ApiResponse.success("Live class updated successfully", updated));
    }

    @PutMapping("/{id}/lesson")
    @Operation(summary = "Link an existing lesson to this live class (Lesson ↔ Live Class)")
    public ResponseEntity<ApiResponse<LiveClassResponse>> linkLesson(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "userEmail", required = false) String userEmail,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @PathVariable UUID id,
            @Valid @RequestBody LinkLessonRequest request) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        LiveClassResponse updated = liveClassService.linkLesson(teacher.getId(), id, request.getLessonId());

        auditSafely(institutionId, userId, userEmail, userRole, "live_class", id,
                updated.getTitle(), AuditLog.AuditAction.UPDATE, null, Map.of(
                        "lessonId", String.valueOf(request.getLessonId()),
                        "lessonTitle", String.valueOf(updated.getLessonTitle()),
                        "action", "LESSON_LINKED"));

        return ResponseEntity.ok(ApiResponse.success("Lesson linked successfully", updated));
    }

    @DeleteMapping("/{id}/lesson")
    @Operation(summary = "Unlink the lesson from this live class")
    public ResponseEntity<ApiResponse<LiveClassResponse>> unlinkLesson(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "userEmail", required = false) String userEmail,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @PathVariable UUID id) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        String previousLessonId = liveClassRepository.findById(id)
                .map(lc -> String.valueOf(lc.getLessonId())).orElse(null);
        LiveClassResponse updated = liveClassService.unlinkLesson(teacher.getId(), id);

        auditSafely(institutionId, userId, userEmail, userRole, "live_class", id,
                updated.getTitle(), AuditLog.AuditAction.UPDATE,
                previousLessonId != null ? Map.of("lessonId", previousLessonId) : null,
                Map.of("action", "LESSON_UNLINKED"));

        return ResponseEntity.ok(ApiResponse.success("Lesson unlinked successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a live class")
    public ResponseEntity<ApiResponse<Void>> cancelLiveClass(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "userEmail", required = false) String userEmail,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @PathVariable UUID id) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        liveClassService.cancelLiveClass(teacher.getId(), id);

        LiveClass liveClass = liveClassRepository.findById(id).orElse(null);
        if (liveClass != null) {
            try {
                notificationService.notifyInstitutionStudentsExcluding(
                        institutionId, userId,
                        "Live Class Cancelled",
                        "Your live class \"" + liveClass.getTitle() + "\" has been cancelled.",
                        "LIVE_CLASS_CANCELLED", "live_class", id);
            } catch (Exception e) {
                log.warn("Failed to send live class cancelled notification: {}", e.getMessage());
            }
        }

        auditSafely(institutionId, userId, userEmail, userRole, "live_class", id,
                liveClass != null ? liveClass.getTitle() : null, AuditLog.AuditAction.UPDATE,
                null, Map.of("status", "CANCELLED"));

        return ResponseEntity.ok(ApiResponse.success("Live class cancelled successfully", null));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start a live session (SCHEDULED -> IN_PROGRESS)")
    public ResponseEntity<ApiResponse<LiveClassResponse>> startSession(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "userEmail", required = false) String userEmail,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @PathVariable UUID id) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        LiveClassResponse started = liveClassService.startSession(teacher.getId(), id);

        try {
            notificationService.notifyInstitutionStudentsExcluding(
                    institutionId, userId,
                    "Live Class Started",
                    "Your live class \"" + started.getTitle() + "\" has started. Join now!",
                    "LIVE_CLASS_STARTED", "live_class", started.getId());
        } catch (Exception e) {
            log.warn("Failed to send live class started notification: {}", e.getMessage());
        }

        auditSafely(institutionId, userId, userEmail, userRole, "live_class", started.getId(),
                started.getTitle(), AuditLog.AuditAction.UPDATE, null, Map.of("status", "IN_PROGRESS"));

        return ResponseEntity.ok(ApiResponse.success("Live session started", started));
    }

    @PostMapping("/{id}/end")
    @Operation(summary = "End a live session (IN_PROGRESS -> COMPLETED)")
    public ResponseEntity<ApiResponse<LiveClassResponse>> endSession(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "userEmail", required = false) String userEmail,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @PathVariable UUID id) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        LiveClassResponse ended = liveClassService.endSession(teacher.getId(), id, userId);

        LiveClass liveClass = liveClassRepository.findById(id).orElse(null);
        if (liveClass != null) {
            try {
                notificationService.notifyInstitutionStudentsExcluding(
                        institutionId, userId,
                        "Live Class Ended",
                        "Your live class \"" + liveClass.getTitle() + "\" has ended.",
                        "LIVE_CLASS_COMPLETED", "live_class", ended.getId());
            } catch (Exception e) {
                log.warn("Failed to send live class ended notification: {}", e.getMessage());
            }
        }

        auditSafely(institutionId, userId, userEmail, userRole, "live_class", ended.getId(),
                ended.getTitle(), AuditLog.AuditAction.UPDATE, null, Map.of("status", "COMPLETED"));

        return ResponseEntity.ok(ApiResponse.success("Live session ended", ended));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get live class detail")
    public ResponseEntity<ApiResponse<LiveClassResponse>> getLiveClass(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);

        LiveClass liveClass = liveClassRepository.findById(id)
                .filter(lc -> lc.getTeacherId().equals(teacher.getId()) && !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("LiveClass", "id", id));

        LiveClassResponse response = liveClassService.getLiveClassById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "Get participants for a live class")
    public ResponseEntity<ApiResponse<List<tz.elmkusoma.liveclass.dto.ParticipantInfo>>> getParticipants(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @PathVariable UUID id) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        verifyLiveClassOwnership(id, teacher.getId(), serverInstitutionId);
        List<LiveClassParticipant> participants = participantRepository.findByLiveClassIdAndIsDeletedFalse(id);

        List<tz.elmkusoma.liveclass.dto.ParticipantInfo> info = participants.stream().map(p -> {
            tz.elmkusoma.shared.domain.User pUser = userRepository.findById(p.getUserId()).orElse(null);
            return tz.elmkusoma.liveclass.dto.ParticipantInfo.builder()
                    .userId(p.getUserId().toString())
                    .userName(pUser != null ? pUser.getFullName() : "Unknown")
                    .role(p.getRole())
                    .joinedAt(p.getJoinedAt())
                    .leftAt(p.getLeftAt())
                    .durationSeconds(p.getDurationSeconds())
                    .online(p.getLeftAt() == null)
                    .build();
        }).toList();
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get participation stats for a live class")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getParticipantStats(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @PathVariable UUID id) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        verifyLiveClassOwnership(id, teacher.getId(), serverInstitutionId);
        long totalJoined = participantRepository.countByLiveClassIdAndIsDeletedFalse(id);
        long currentlyConnected = participantRepository.countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalJoined", totalJoined,
                "currentlyConnected", currentlyConnected
        )));
    }

    /** Audit write that can never fail the business transaction (§13). */
    private void auditSafely(UUID institutionId, UUID userId, String userEmail, String userRole,
                             String entityType, UUID entityId, String entityName,
                             AuditLog.AuditAction action,
                             Map<String, Object> oldValues, Map<String, Object> newValues) {
        try {
            if (auditService == null) {
                return;
            }
            auditService.recordAuditLog(institutionId, userId, userEmail, userRole,
                    entityType, entityId, entityName, action, oldValues, newValues);
        } catch (Exception ex) {
            log.warn("Audit write failed for {} {}: {}", entityType, entityId, ex.getMessage());
        }
    }

    private void verifyLiveClassOwnership(UUID liveClassId, UUID teacherId, UUID serverInstitutionId) {
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass != null) {
            if (liveClass.getInstitutionId() == null || !liveClass.getInstitutionId().equals(serverInstitutionId)
                    || liveClass.getTeacherId() == null || !liveClass.getTeacherId().equals(teacherId)) {
                throw new ForbiddenException("Live class", "access");
            }
        }
    }
}
