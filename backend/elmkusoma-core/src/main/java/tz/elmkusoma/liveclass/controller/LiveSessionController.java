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
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER')")
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

        String classStatus = liveClass.getStatus();
        if (!"IN_PROGRESS".equals(classStatus) && !"LIVE".equals(classStatus)) {
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
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER')")
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
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER')")
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

    @GetMapping("/classes/{classId}/recording/download")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER')")
    @Operation(summary = "Get recording download URL")
    public ResponseEntity<ApiResponse<Map<String, String>>> getRecordingDownload(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId) {

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }

        String recordingUrl = liveClass.getRecordingUrl();
        if (recordingUrl == null || recordingUrl.isBlank()) {
            return ResponseEntity.status(404).body(ApiResponse.error("No recording available for this class"));
        }

        Map<String, String> result = new HashMap<>();
        if (recordingUrl.startsWith("egress:")) {
            result.put("status", "PROCESSING");
            result.put("message", "Recording is still being processed");
        } else if (recordingUrl.startsWith("http")) {
            result.put("status", "READY");
            result.put("downloadUrl", recordingUrl);
            result.put("filename", "live-class-" + classId + ".mp4");
        } else {
            result.put("status", "UNKNOWN");
            result.put("message", "Recording status unknown");
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/calendar/{classId}/export")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER')")
    @Operation(summary = "Export live class as .ics calendar event")
    public ResponseEntity<String> exportCalendarEvent(@PathVariable UUID classId) {
        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).build();
        }

        String tz = liveClass.getTimezone() != null ? liveClass.getTimezone() : "Africa/Dar_es_Salaam";
        java.time.LocalDateTime start = liveClass.getScheduledAt();
        java.time.LocalDateTime end = start.plusMinutes(
                liveClass.getDurationMinutes() != null ? liveClass.getDurationMinutes() : 60);

        String ics = "BEGIN:VCALENDAR\r\n"
                + "VERSION:2.0\r\n"
                + "PRODID:-//ELMKUSOMA//Live Classes//EN\r\n"
                + "BEGIN:VEVENT\r\n"
                + "UID:" + classId + "@elmkusoma\r\n"
                + "DTSTART:" + formatIcsDateTime(start) + "\r\n"
                + "DTEND:" + formatIcsDateTime(end) + "\r\n"
                + "SUMMARY:" + escapeIcs(liveClass.getTitle()) + "\r\n"
                + (liveClass.getDescription() != null ? "DESCRIPTION:" + escapeIcs(liveClass.getDescription()) + "\r\n" : "")
                + "LOCATION:ELMKUSOMA Live\r\n"
                + "STATUS:CONFIRMED\r\n"
                + "BEGIN:VALARM\r\n"
                + "TRIGGER:-PT15M\r\n"
                + "ACTION:DISPLAY\r\n"
                + "DESCRIPTION:Live class starting in 15 minutes\r\n"
                + "END:VALARM\r\n"
                + "END:VEVENT\r\n"
                + "END:VCALENDAR\r\n";

        return ResponseEntity.ok()
                .header("Content-Type", "text/calendar; charset=utf-8")
                .header("Content-Disposition", "attachment; filename=\"live-class-" + classId + ".ics\"")
                .body(ics);
    }

    private String formatIcsDateTime(java.time.LocalDateTime ldt) {
        return ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
    }

    private String escapeIcs(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }
}
