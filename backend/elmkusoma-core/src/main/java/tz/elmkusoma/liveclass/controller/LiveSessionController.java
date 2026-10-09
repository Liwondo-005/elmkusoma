package tz.elmkusoma.liveclass.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.ClassAccessGuard;
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
    private final ClassAccessGuard classAccessGuard;

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
        if (!"IN_PROGRESS".equals(classStatus) && !"LIVE".equals(classStatus)
                && !"STARTING".equals(classStatus)) {
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

        // Class-scoped session (classGroupId set): learners must prove membership of
        // the target class via the existing union model (student_class_assignments ∪
        // ENROLLED enrollments). Sessions without a classGroupId stay open to any
        // institution member. Teachers bypass.
        if (!isTeacher && liveClass.getClassGroupId() != null
                && !classAccessGuard.isLearnerInClass(user.getEmail(), liveClass.getClassGroupId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("You are not a member of this class"));
        }

        if (!isTeacher) {
            if (liveClass.getMaxParticipants() != null) {
                long currentCount = participantRepository.countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(classId);
                if (currentCount >= liveClass.getMaxParticipants()) {
                    return ResponseEntity.status(400).body(ApiResponse.error("Live class is full"));
                }
            }
        }

        // Existing participant role (LEARNER / MODERATOR / OBSERVER) decides media grants, so a
        // host's role change is enforced by LiveKit itself rather than being advisory.
        String participantRole = participantRepository
                .findByLiveClassIdAndUserIdAndIsDeletedFalse(classId, userId)
                .map(tz.elmkusoma.liveclass.domain.LiveClassParticipant::getRole)
                .orElse(null);

        String token = liveKitService.generateToken(classId, userId, userId.toString(), isTeacher, participantRole);
        String roomName = liveKitService.generateRoomName(classId);

        LiveSessionJoinResponse response = LiveSessionJoinResponse.builder()
                .liveKitToken(token)
                .liveKitUrl(liveKitService.isAvailable() ? liveKitService.getServerUrl() : null)
                .roomName(roomName)
                .liveKitAvailable(liveKitService.isAvailable())
                .classStatus(liveClass.getStatus())
                .message(liveKitService.isAvailable()
                        ? "Joined live session"
                        : (liveKitService.isConfigured()
                                ? "LiveKit server unreachable - chat only"
                                : "LiveKit not configured - chat only"))
                .build();

        log.info("User {} joined live session class={} teacher={}", userId, classId, isTeacher);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/participants/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN','ADMIN')")
    @Operation(summary = "Get live class participants")
    public ResponseEntity<ApiResponse<List<ParticipantInfo>>> getParticipants(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userRole") String userRole,
            @PathVariable UUID classId) {

        var liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Live class not found"));
        }
        boolean isParticipant = participantRepository
                .existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, userId);
        boolean isTeacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .map(t -> liveClass.getTeacherId() != null && liveClass.getTeacherId().equals(t.getId()))
                .orElse(false);
        boolean isAdmin = "ADMIN".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole);
        if (!isParticipant && !isTeacher && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"));
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

    @PostMapping("/participants/{classId}/role")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
    @Operation(summary = "Promote or demote a live class participant role (§30)")
    public ResponseEntity<ApiResponse<ParticipantInfo>> updateParticipantRole(
            @RequestAttribute("userId") UUID actorId,
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID classId,
            @Valid @RequestBody ParticipantRoleRequest request) {

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }
        if (!liveClass.getInstitutionId().equals(institutionId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        boolean isActorTeacher = teacherRepository.findByUserIdAndInstitutionId(actorId, institutionId)
                .map(t -> liveClass.getTeacherId() != null
                        && (liveClass.getTeacherId().equals(t.getId()) || liveClass.getTeacherId().equals(actorId)))
                .orElse(liveClass.getTeacherId() != null && liveClass.getTeacherId().equals(actorId));
        User actor = userRepository.findById(actorId).orElse(null);
        boolean isAdmin = actor != null
                && (actor.getRole() == User.Role.ADMIN || actor.getRole() == User.Role.INSTITUTION_ADMIN);
        if (!isActorTeacher && !isAdmin) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only the class teacher or an admin can change roles"));
        }

        String role = request.getRole() == null ? "" : request.getRole().trim().toUpperCase(Locale.ROOT);
        Set<String> allowed = Set.of(
                LiveClassParticipant.ROLE_LEARNER,
                LiveClassParticipant.ROLE_TEACHER,
                LiveClassParticipant.ROLE_MODERATOR,
                LiveClassParticipant.ROLE_OBSERVER);
        if (!allowed.contains(role)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid role: " + role));
        }

        UUID targetUserId = request.getUserId();
        if (targetUserId == null || targetUserId.equals(actorId)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot change own role here"));
        }

        LiveClassParticipant participant = participantRepository
                .findByLiveClassIdAndUserIdAndIsDeletedFalse(classId, targetUserId)
                .orElse(null);
        if (participant == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Participant not found in this class"));
        }

        participant.setRole(role);
        participantRepository.save(participant);

        User pUser = userRepository.findById(targetUserId).orElse(null);
        ParticipantInfo info = ParticipantInfo.builder()
                .userId(targetUserId.toString())
                .userName(pUser != null ? pUser.getFullName() : "Unknown")
                .role(role)
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .durationSeconds(participant.getDurationSeconds())
                .online(participant.getLeftAt() == null)
                .build();

        log.info("Participant role changed: class={} user={} role={} by={}",
                classId, targetUserId, role, actorId);
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

        // Real peak concurrency: sweep the join/leave intervals and keep the maximum number
        // of participants present at any moment. Previously this was just the current online
        // count (or the total participant count), which is not a peak at all.
        int peak = computePeakConcurrency(allParticipants);
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

    /**
     * Maximum number of participants simultaneously present, computed from the persisted
     * join/leave intervals (sweep line over +1/-1 events). Participants still present have
     * no leftAt and therefore stay counted for the rest of the sweep.
     */
    public static int computePeakConcurrency(List<LiveClassParticipant> participants) {
        if (participants == null || participants.isEmpty()) {
            return 0;
        }
        record Event(LocalDateTime at, int delta) { }
        List<Event> events = new ArrayList<>();
        for (LiveClassParticipant p : participants) {
            if (p.getJoinedAt() != null) {
                events.add(new Event(p.getJoinedAt(), 1));
            }
            if (p.getLeftAt() != null) {
                events.add(new Event(p.getLeftAt(), -1));
            }
        }
        if (events.isEmpty()) {
            return 0;
        }
        // Sweep in chronological order; at an identical instant a join is applied before a
        // leave so a participant who leaves exactly as another joins is not double-counted.
        events.sort(Comparator.comparing(Event::at).thenComparingInt(e -> -e.delta()));
        int current = 0;
        int peak = 0;
        for (Event e : events) {
            current += e.delta();
            peak = Math.max(peak, current);
        }
        return peak;
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

        // Institution check: only active members of the class's institution may
        // file issues (blocks cross-institution issue spam; header alone is never
        // trusted — membership is verified server-side).
        if (liveClass.getInstitutionId() == null
                || !membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, liveClass.getInstitutionId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
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

        if (!isClassHostOrAdmin(liveClass, userId, institutionId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only the teacher can start recording"));
        }

        if (!liveKitService.isAvailable()) {
            return ResponseEntity.status(503).body(ApiResponse.error("LiveKit not configured"));
        }
        if (!liveKitService.isRecordingConfigured()) {
            // Spec: "Recording: Enabled only if configured." Be explicit instead of a generic 500.
            return ResponseEntity.status(503).body(ApiResponse.error(
                    "Recording storage is not configured on the server. Ask your administrator to enable it."));
        }

        // Idempotency: recordingUrl holds the "egress:<id>" marker while a capture is running.
        // A repeated start (double click, retried request) must not open a second LiveKit egress
        // job - it would duplicate the recording and orphan the first job's file. Report the
        // already-running capture instead, which is truthful and settles the UI on one id.
        String existing = liveClass.getRecordingUrl();
        if (existing != null && existing.startsWith("egress:")) {
            Map<String, String> alreadyRunning = new HashMap<>();
            alreadyRunning.put("egressId", existing.substring("egress:".length()));
            alreadyRunning.put("alreadyRecording", "true");
            log.info("Recording already active for class {} (egress {}); ignoring duplicate start",
                    classId, alreadyRunning.get("egressId"));
            return ResponseEntity.ok(ApiResponse.success("Recording already in progress", alreadyRunning));
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

        // Institution membership alone is not enough to stop someone else's recording:
        // require the owning teacher or an admin, exactly like the ingest endpoints.
        if (!isClassHostOrAdmin(liveClass, userId, institutionId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(
                    "Only the teacher of this class or an administrator can stop its recording"));
        }

        String recordingUrl = liveClass.getRecordingUrl();
        if (recordingUrl == null || !recordingUrl.startsWith("egress:")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No active recording found"));
        }

        String egressId = recordingUrl.substring("egress:".length());
        boolean stopped = liveKitService.stopRecording(egressId);

        if (stopped) {
            // resolve the final file URL right away so download/replay can serve it
            try {
                String url = liveKitService.resolveRecordingUrl(egressId);
                if (url != null) {
                    liveClass.setRecordingUrl(url);
                    liveClassRepository.save(liveClass);
                }
            } catch (Exception e) {
                log.warn("Recording URL resolve skipped for class {}: {}", classId, e.getMessage());
            }
            log.info("Recording stopped for class {} by teacher {}", classId, userId);
            return ResponseEntity.ok(ApiResponse.success("Recording stopped", egressId));
        }
        return ResponseEntity.status(500).body(ApiResponse.error("Failed to stop recording"));
    }

    @GetMapping("/classes/{classId}/recording/download")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN','ADMIN')")
    @Operation(summary = "Get recording download URL")
    public ResponseEntity<ApiResponse<Map<String, String>>> getRecordingDownload(
            @RequestAttribute(value = "userId", required = false) UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestHeader(value = "X-Institution-Id", required = false) UUID headerInstitutionId,
            @PathVariable UUID classId) {

        // §41/§60/§98: institution + ownership check before any recording data is revealed
        UUID callerInstitutionId = institutionId != null ? institutionId : headerInstitutionId;

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }

        if (callerInstitutionId == null || !liveClass.getInstitutionId().equals(callerInstitutionId)) {
            // Cross-institution ID manipulation: deny without leaking existence (403)
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        boolean allowed;
        if ("ADMIN".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole) || "NATIONAL_ADMIN".equals(userRole)) {
            allowed = true;
        } else if ("TEACHER".equals(userRole)) {
            // teacher owns the class (teacherId may store either Teacher.id or User.id)
            allowed = teacherRepository.findByUserIdAndInstitutionId(userId, callerInstitutionId)
                    .map(t -> liveClass.getTeacherId() != null
                            && (liveClass.getTeacherId().equals(t.getId()) || liveClass.getTeacherId().equals(userId)))
                    .orElse(liveClass.getTeacherId() != null && liveClass.getTeacherId().equals(userId));
        } else {
            // learner: must be an active member of the class's institution
            allowed = membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, callerInstitutionId);
            if (allowed && liveClass.getClassGroupId() != null) {
                // class-scoped session: prove membership of the target class
                // (student_class_assignments ∪ ENROLLED enrollments, strict)
                String email = userRepository.findById(userId).map(User::getEmail).orElse(null);
                allowed = classAccessGuard.isLearnerInClass(email, liveClass.getClassGroupId());
            }
        }
        if (!allowed) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
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

    // ------------------------------------------------------------------
    // External ingest: OBS / hardware encoder / studio -> existing room
    /**
     * Host-continuity state for the current live class. A host that reloads, navigates
     * away, or loses the room must be able to resume the SAME in-progress session with its
     * real recording state instead of guessing from local component state. This is
     * read-only and derived entirely from persisted LiveClass/participant data.
     */
    @GetMapping("/classes/{classId}/host-state")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
    @Operation(summary = "Get live class host state (session continuity, recording, participants)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHostState(
            @PathVariable UUID classId,
            @RequestAttribute UUID userId,
            @RequestAttribute UUID institutionId) {

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }
        if (!liveClass.getInstitutionId().equals(institutionId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        if (!isClassHostOrAdmin(liveClass, userId, institutionId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(
                    "Only the teacher of this class or an administrator can view its host state"));
        }

        String status = liveClass.getStatus();
        boolean active = "IN_PROGRESS".equals(status) || "LIVE".equals(status) || "STARTING".equals(status);
        String rawRecordingUrl = liveClass.getRecordingUrl();
        boolean recordingActive = rawRecordingUrl != null && rawRecordingUrl.startsWith("egress:");
        String egressId = recordingActive ? rawRecordingUrl.substring("egress:".length()) : null;
        boolean recordingAvailable = rawRecordingUrl != null && rawRecordingUrl.startsWith("http");
        List<tz.elmkusoma.liveclass.domain.LiveClassParticipant> present =
                participantRepository.findByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(classId);

        Map<String, Object> state = new HashMap<>();
        state.put("classId", liveClass.getId());
        state.put("status", status);
        state.put("active", active);
        state.put("startedAt", liveClass.getStartedAt());
        state.put("scheduledAt", liveClass.getScheduledAt());
        state.put("durationMinutes", liveClass.getDurationMinutes());
        state.put("recordingEnabled", liveClass.getRecordingEnabled());
        state.put("recordingActive", recordingActive);
        state.put("recordingEgressId", egressId);
        state.put("recordingAvailable", recordingAvailable);
        state.put("recordingUrl", recordingAvailable ? rawRecordingUrl : null);
        state.put("liveKitAvailable", liveKitService.isAvailable());
        state.put("liveKitUrl", liveKitService.isAvailable() ? liveKitService.getServerUrl() : null);
        state.put("roomName", liveKitService.generateRoomName(classId));
        state.put("presentParticipants", present.size());
        state.put("canEndSession", active);

        return ResponseEntity.ok(ApiResponse.success("Host state", state));
    }

    // ------------------------------------------------------------------

    /**
     * Shared guard for the ingest endpoints: the class exists, belongs to the
     * caller's institution, and the caller is the owning teacher or an admin.
     * Returns null when authorized, otherwise the error response to send.
     */
    private ResponseEntity<ApiResponse<Map<String, Object>>> denyIngestAccess(
            LiveClass liveClass, UUID userId, UUID institutionId) {
        if (liveClass == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        }
        if (liveClass.getInstitutionId() == null || !liveClass.getInstitutionId().equals(institutionId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        if (!isClassHostOrAdmin(liveClass, userId, institutionId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(
                    "Only the teacher of this class can manage its ingest source"));
        }
        return null;
    }

    /**
     * Shared ownership guard: true when the caller owns the class as its teacher, or is an
     * ADMIN / INSTITUTION_ADMIN. Reused by the ingest endpoints and the recording controls
     * so "any teacher of the institution" can never operate another teacher's session.
     */
    private boolean isClassHostOrAdmin(LiveClass liveClass, UUID userId, UUID institutionId) {
        if (liveClass == null || userId == null) {
            return false;
        }
        if (institutionId != null && liveClass.getInstitutionId() != null
                && !liveClass.getInstitutionId().equals(institutionId)) {
            return false;
        }
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        if (liveClass.getTeacherId() != null && teacher != null && liveClass.getTeacherId().equals(teacher.getId())) {
            return true;
        }
        User user = userRepository.findById(userId).orElse(null);
        return user != null && (user.getRole() == User.Role.ADMIN
                || user.getRole() == User.Role.INSTITUTION_ADMIN);
    }

    @GetMapping("/classes/{classId}/ingress")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
    @Operation(summary = "Inspect external ingest (OBS/encoder/studio) endpoints for a live class")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIngress(
            @PathVariable UUID classId,
            @RequestAttribute UUID userId,
            @RequestAttribute UUID institutionId) {

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        ResponseEntity<ApiResponse<Map<String, Object>>> denied = denyIngestAccess(liveClass, userId, institutionId);
        if (denied != null) {
            return denied;
        }

        Map<String, Object> payload = new HashMap<>();
        boolean configured = liveKitService.isIngressConfigured();
        payload.put("configured", configured);
        if (!configured) {
            // Honest state instead of a fake endpoint: nothing is pushed anywhere.
            payload.put("ingresses", List.of());
            payload.put("message", "External ingest (WHIP/RTMP/SRT) is not configured on this server. "
                    + "Deploy LiveKit Ingress and set LIVEKIT_INGRESS_ENABLED=true to enable OBS/encoder/studio sources.");
            return ResponseEntity.ok(ApiResponse.success(payload));
        }
        payload.put("ingresses", liveKitService.listIngress(classId));
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @PostMapping("/classes/{classId}/ingress")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
    @Operation(summary = "Create an ingest endpoint (WHIP/RTMP/SRT) that pushes OBS/encoder/studio into this live class")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createIngress(
            @PathVariable UUID classId,
            @RequestAttribute UUID userId,
            @RequestAttribute UUID institutionId,
            @RequestBody(required = false) Map<String, String> body) {

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        ResponseEntity<ApiResponse<Map<String, Object>>> denied = denyIngestAccess(liveClass, userId, institutionId);
        if (denied != null) {
            return denied;
        }

        if (!liveKitService.isIngressConfigured()) {
            return ResponseEntity.status(503).body(ApiResponse.error(
                    "External ingest is not configured on this server. Ask your administrator to deploy "
                    + "LiveKit Ingress and set LIVEKIT_INGRESS_ENABLED=true."));
        }

        String protocol = body != null ? body.get("protocol") : null;
        if (protocol == null || protocol.isBlank()) {
            protocol = "WHIP";
        }

        Map<String, String> ingress;
        try {
            ingress = liveKitService.createIngress(classId, protocol,
                    "ingress-" + classId, "ELMKUSOMA Ingest " + classId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
        if (ingress == null) {
            return ResponseEntity.status(502).body(ApiResponse.error(
                    "The LiveKit server rejected the ingest request"));
        }

        Map<String, Object> payload = new HashMap<>(ingress);
        payload.put("configured", true);
        log.info("Ingest endpoint created for class {} by user {} (protocol={})", classId, userId, protocol);
        return ResponseEntity.ok(ApiResponse.success("Ingest endpoint created", payload));
    }

    @DeleteMapping("/classes/{classId}/ingress")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
    @Operation(summary = "Remove all ingest endpoints for a live class")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteIngress(
            @PathVariable UUID classId,
            @RequestAttribute UUID userId,
            @RequestAttribute UUID institutionId) {

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        ResponseEntity<ApiResponse<Map<String, Object>>> denied = denyIngestAccess(liveClass, userId, institutionId);
        if (denied != null) {
            return denied;
        }

        Map<String, Object> payload = new HashMap<>();
        if (!liveKitService.isIngressConfigured()) {
            payload.put("configured", false);
            payload.put("removed", 0);
            return ResponseEntity.ok(ApiResponse.success(payload));
        }

        int removed = 0;
        int failed = 0;
        for (Map<String, String> ingress : liveKitService.listIngress(classId)) {
            String ingressId = ingress.get("ingressId");
            if (ingressId == null) {
                continue;
            }
            if (liveKitService.deleteIngress(ingressId)) {
                removed++;
            } else {
                failed++;
            }
        }
        payload.put("configured", true);
        payload.put("removed", removed);
        payload.put("failed", failed);
        log.info("Ingest cleanup for class {} by user {}: removed={}, failed={}", classId, userId, removed, failed);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @GetMapping("/calendar/{classId}/export")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN','ADMIN')")
    @Operation(summary = "Export live class as .ics calendar event")
    public ResponseEntity<String> exportCalendarEvent(
            @PathVariable UUID classId,
            @RequestAttribute("userId") UUID userId) {
        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(404).build();
        }

        // Institution check: only active members of the class's institution may
        // export (blocks cross-institution title/schedule disclosure). Membership
        // is verified server-side; deny without leaking (403, empty body).
        if (liveClass.getInstitutionId() == null
                || !membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, liveClass.getInstitutionId())) {
            return ResponseEntity.status(403).build();
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
