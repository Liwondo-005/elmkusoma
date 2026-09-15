package tz.elmkusoma.liveclass.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.dto.ParticipantInfo;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/admin/live-sessions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','ADMIN')")
@Tag(name = "Admin Live Session Monitoring", description = "Monitor active live sessions across institution")
public class AdminLiveSessionController {

    private final LiveClassRepository liveClassRepository;
    private final LiveClassParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @GetMapping("/active")
    @Operation(summary = "Get all active live sessions across institution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveSessions(
            @RequestHeader("X-Institution-Id") UUID institutionId) {

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
            @RequestHeader("X-Institution-Id") UUID institutionId) {

        long total = liveClassRepository.countByInstitutionIdAndIsDeletedFalse(institutionId);
        long scheduled = liveClassRepository.findByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "SCHEDULED").size();
        long inProgress = liveClassRepository.findByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "IN_PROGRESS").size();
        long completed = liveClassRepository.findByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "COMPLETED").size();

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
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID classId) {

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
}
