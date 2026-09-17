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
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.domain.LiveClassIssue;
import tz.elmkusoma.liveclass.dto.*;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.liveclass.repository.LiveClassChatMessageRepository;
import tz.elmkusoma.liveclass.repository.LiveClassIssueRepository;
import tz.elmkusoma.liveclass.service.LiveKitService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.teacher.service.TeacherService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/live-session")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Live Session", description = "Live session join, participants, and reporting")
public class LiveSessionController {

    private final LiveKitService liveKitService;
    private final LiveClassRepository liveClassRepository;
    private final LiveClassParticipantRepository participantRepository;
    private final LiveClassIssueRepository issueRepository;
    private final LiveClassChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherService teacherService;
    private final InstitutionMembershipRepository membershipRepository;

    @PostMapping("/join/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER')")
    @Operation(summary = "Join a live class session and receive LiveKit token")
    public ResponseEntity<ApiResponse<LiveSessionJoinResponse>> joinSession(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId) {

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }

        if (!liveClass.getInstitutionId().equals(institutionId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized for this class"));
        }

        if (!"IN_PROGRESS".equals(liveClass.getStatus())) {
            return ResponseEntity.status(400).body(ApiResponse.error("Live class is not currently in session"));
        }

        boolean isMember = membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, institutionId);
        if (!isMember) {
            return ResponseEntity.status(403).body(ApiResponse.error("You are not a member of this institution"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found"));
        }

        boolean isTeacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId).isPresent();

        if (!isTeacher) {
            if (liveClass.getMaxParticipants() != null) {
                long currentCount = participantRepository.countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(classId);
                if (currentCount >= liveClass.getMaxParticipants()) {
                    return ResponseEntity.status(400).body(ApiResponse.error("Live class is full"));
                }
            }
        }

        String token = liveKitService.generateToken(classId, userId, userId.toString(), isTeacher);
        String roomName = liveKitService.generateRoomName(classId);

        LiveSessionJoinResponse response = LiveSessionJoinResponse.builder()
                .liveKitToken(token)
                .liveKitUrl(liveKitService.isAvailable() ? liveKitService.getServerUrl() : null)
                .roomName(roomName)
                .liveKitAvailable(liveKitService.isAvailable())
                .classStatus(liveClass.getStatus())
                .message(liveKitService.isAvailable() ? "Joined live session" : "LiveKit not configured - chat only")
                .build();

        log.info("User {} joined live session class={} teacher={}", userId, classId, isTeacher);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/participants/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER')")
    @Operation(summary = "Get live class participants")
    public ResponseEntity<ApiResponse<List<ParticipantInfo>>> getParticipants(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId) {

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

    @GetMapping("/analytics/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get live class analytics (teacher only)")
    public ResponseEntity<ApiResponse<LiveClassAnalytics>> getAnalytics(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId) {

        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null || !liveClass.getTeacherId().equals(teacher.getId())) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }

        long totalParticipants = participantRepository.countByLiveClassIdAndIsDeletedFalse(classId);
        long currentOnline = participantRepository.countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(classId);
        long totalChatMessages = chatMessageRepository.countByLiveClassIdAndIsDeletedFalse(classId);

        List<LiveClassParticipant> allParticipants = participantRepository
                .findByLiveClassIdAndIsDeletedFalse(classId);

        int peak = allParticipants.stream()
                .filter(p -> p.getLeftAt() == null)
                .mapToInt(p -> 1).sum();
        if (peak == 0) peak = (int) totalParticipants;
        long avgDuration = (long) allParticipants.stream()
                .filter(p -> p.getDurationSeconds() != null)
                .mapToLong(LiveClassParticipant::getDurationSeconds)
                .average().orElse(0.0);

        LiveClassAnalytics analytics = LiveClassAnalytics.builder()
                .totalParticipants((int) totalParticipants)
                .currentOnline((int) currentOnline)
                .peakParticipants(peak)
                .totalChatMessages(totalChatMessages)
                .averageDurationSeconds(avgDuration)
                .build();

        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @PostMapping("/report/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER')")
    @Operation(summary = "Report an issue during a live class")
    public ResponseEntity<ApiResponse<String>> reportIssue(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId,
            @Valid @RequestBody LiveClassReportRequest request) {

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }

        LiveClassIssue issue = LiveClassIssue.builder()
                .liveClassId(classId)
                .userId(userId)
                .issueType(request.getIssueType())
                .description(request.getDescription())
                .severity(request.getSeverity() != null ? request.getSeverity() : "MEDIUM")
                .build();
        issueRepository.save(issue);

        log.info("Issue reported for live class {} by user {}: {}", classId, userId, request.getIssueType());
        return ResponseEntity.ok(ApiResponse.success("Issue reported successfully", null));
    }

    @PostMapping("/classes/{classId}/recording/start")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
    @Operation(summary = "Start recording a live class session (teacher only)")
    public ResponseEntity<ApiResponse<Map<String, String>>> startRecording(
            @PathVariable UUID classId,
            @RequestAttribute UUID userId,
            @RequestAttribute UUID institutionId) {

        LiveClass liveClass = liveClassRepository.findById(classId).orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }
        if (!liveClass.getInstitutionId().equals(institutionId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        boolean isTeacher = liveClass.getTeacherId() != null && liveClass.getTeacherId().equals(userId);
        if (!isTeacher) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.INSTITUTION_ADMIN)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Only the teacher can start recording"));
            }
        }

        if (!liveKitService.isAvailable()) {
            return ResponseEntity.status(503).body(ApiResponse.error("LiveKit not configured"));
        }

        String egressId = liveKitService.startRecording(classId);
        if (egressId != null) {
            liveClass.setRecordingUrl("egress:" + egressId);
            liveClassRepository.save(liveClass);

            Map<String, String> result = new HashMap<>();
            result.put("egressId", egressId);
            log.info("Recording started for class {} by teacher {}", classId, userId);
            return ResponseEntity.ok(ApiResponse.success("Recording started", result));
        }
        return ResponseEntity.status(500).body(ApiResponse.error("Failed to start recording"));
    }

    @PostMapping("/classes/{classId}/recording/stop")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
    @Operation(summary = "Stop recording a live class session (teacher only)")
    public ResponseEntity<ApiResponse<String>> stopRecording(
            @PathVariable UUID classId,
            @RequestAttribute UUID userId,
            @RequestAttribute UUID institutionId) {

        LiveClass liveClass = liveClassRepository.findById(classId).orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }
        if (!liveClass.getInstitutionId().equals(institutionId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        String recordingUrl = liveClass.getRecordingUrl();
        if (recordingUrl == null || !recordingUrl.startsWith("egress:")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No active recording found"));
        }

        String egressId = recordingUrl.substring("egress:".length());
        boolean stopped = liveKitService.stopRecording(egressId);

        if (stopped) {
            liveClass.setRecordingUrl(null);
            liveClassRepository.save(liveClass);
            log.info("Recording stopped for class {} by teacher {}", classId, userId);
            return ResponseEntity.ok(ApiResponse.success("Recording stopped", egressId));
        }
        return ResponseEntity.status(500).body(ApiResponse.error("Failed to stop recording"));
    }
}
