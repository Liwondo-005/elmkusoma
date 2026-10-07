package tz.elmkusoma.liveclass.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.dto.LiveClassResponse;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.course.service.LiveClassService;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.liveclass.domain.LiveClassIssue;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.dto.ParticipantInfo;
import tz.elmkusoma.liveclass.repository.LiveClassIssueRepository;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/admin/live-sessions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','ADMIN')")
@Tag(name = "Admin Live Session Monitoring", description = "Monitor active live sessions across institution")
public class AdminLiveSessionController {

    private final LiveClassRepository liveClassRepository;
    private final LiveClassParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final LiveClassService liveClassService;
    private final NotificationService notificationService;
    private final LiveClassIssueRepository issueRepository;

    @GetMapping("/active")
    @Operation(summary = "Get all active live sessions across institution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveSessions(
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {

        if (institutionId == null) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        List<LiveClass> activeClasses = liveClassRepository
                .findByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "IN_PROGRESS");

        List<Map<String, Object>> sessions = activeClasses.stream().map(lc -> {
            long participantCount = participantRepository
                    .countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(lc.getId());

            Map<String, Object> session = new LinkedHashMap<>();
            session.put("id", lc.getId());
            session.put("title", lc.getTitle());
            session.put("teacherId", lc.getTeacherId());
            session.put("scheduledAt", lc.getScheduledAt());
            session.put("durationMinutes", lc.getDurationMinutes());
            session.put("maxParticipants", lc.getMaxParticipants());
            session.put("currentParticipants", participantCount);
            session.put("status", lc.getStatus());
            return session;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get live session statistics for institution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {

        if (institutionId == null) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        long total = liveClassRepository.countByInstitutionIdAndIsDeletedFalse(institutionId);
        long scheduled = liveClassRepository.countByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "SCHEDULED");
        long inProgress = liveClassRepository.countByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "IN_PROGRESS");
        long completed = liveClassRepository.countByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "COMPLETED");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSessions", total);
        stats.put("scheduled", scheduled);
        stats.put("inProgress", inProgress);
        stats.put("completed", completed);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/participants/{classId}")
    @Operation(summary = "Get participants of a specific live session (admin)")
    public ResponseEntity<ApiResponse<List<ParticipantInfo>>> getSessionParticipants(
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @PathVariable UUID classId) {

        if (institutionId == null) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null || !liveClass.getInstitutionId().equals(institutionId)) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }

        List<LiveClassParticipant> participants = participantRepository
                .findByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(classId);

        List<ParticipantInfo> info = participants.stream().map(p -> {
            User pUser = userRepository.findById(p.getUserId()).orElse(null);
            return ParticipantInfo.builder()
                    .userId(p.getUserId().toString())
                    .userName(pUser != null ? pUser.getFullName() : "Unknown")
                    .role(p.getRole())
                    .joinedAt(p.getJoinedAt())
                    .leftAt(p.getLeftAt())
                    .durationSeconds(p.getDurationSeconds())
                    .online(p.getLeftAt() == null)
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(info));
    }

    @PostMapping("/{classId}/force-end")
    @Operation(summary = "Force-end an in-progress live session (admin)")
    public ResponseEntity<ApiResponse<LiveClassResponse>> forceEndSession(
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID userId,
            @PathVariable UUID classId) {

        if (institutionId == null) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }
        if (!institutionId.equals(liveClass.getInstitutionId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        String status = liveClass.getStatus();
        if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
            return ResponseEntity.status(400).body(ApiResponse.error("Live class is already " + status));
        }
        if (liveClass.getTeacherId() == null) {
            return ResponseEntity.status(400).body(ApiResponse.error("Live class has no assigned teacher"));
        }

        LiveClassResponse ended = liveClassService.endSession(liveClass.getTeacherId(), classId, userId);

        try {
            notificationService.notifyInstitutionStudentsExcluding(
                    institutionId, userId,
                    "Live Class Ended",
                    "Your live class \"" + liveClass.getTitle() + "\" has ended.",
                    "LIVE_CLASS_COMPLETED", "live_class", ended.getId());
        } catch (Exception e) {
            log.warn("Failed to send live class ended notification: {}", e.getMessage());
        }

        log.info("Live class {} force-ended by admin {}", classId, userId);
        return ResponseEntity.ok(ApiResponse.success("Live session ended", ended));
    }

    @GetMapping("/issues")
    @Operation(summary = "Get reported live class issues for the institution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSessionIssues(
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {

        if (institutionId == null) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        List<LiveClass> classes = liveClassRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
        if (classes.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        Map<UUID, String> titles = new LinkedHashMap<>();
        for (LiveClass lc : classes) {
            titles.put(lc.getId(), lc.getTitle());
        }

        List<LiveClassIssue> issues = issueRepository
                .findByLiveClassIdInAndIsDeletedFalseOrderByCreatedAtDesc(titles.keySet());

        List<Map<String, Object>> payload = issues.stream().map(issue -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", issue.getId());
            item.put("liveClassId", issue.getLiveClassId());
            item.put("classTitle", titles.get(issue.getLiveClassId()));
            item.put("issueType", issue.getIssueType());
            item.put("description", issue.getDescription());
            item.put("severity", issue.getSeverity());
            item.put("status", issue.getStatus());
            item.put("userId", issue.getUserId());
            item.put("reportedBy", userRepository.findById(issue.getUserId())
                    .map(User::getFullName).orElse("Unknown"));
            item.put("createdAt", issue.getCreatedAt());
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(payload));
    }
}
