package tz.elmkusoma.liveclass.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.liveclass.domain.*;
import tz.elmkusoma.liveclass.repository.*;
import tz.elmkusoma.liveclass.service.LiveClassInteractionService;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/v1/live-session")
@RequiredArgsConstructor
@Tag(name = "Live Class Interactions", description = "Quizzes, polls, breakout rooms, shared media, and attendance detail")
public class LiveClassInteractionController {

    private final LiveClassInteractionService interactionService;
    private final LiveClassSharedMediaRepository sharedMediaRepository;
    private final LiveClassAttendanceDetailRepository attendanceDetailRepository;

    /** ROLE_TEACHER decides whether the question payload may carry the answer key. */
    private boolean isTeacher(Authentication authentication) {
        return authentication != null && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));
    }

    // ==================== QUIZZES ====================

    @PostMapping("/classes/{classId}/quizzes")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create and launch a quiz for a live class (validates, persists, broadcasts QUIZ_STARTED)")
    public ResponseEntity<ApiResponse<LiveClassQuiz>> createQuiz(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId,
            @RequestBody Map<String, Object> body) {
        return interactionService.createQuiz(institutionId, userId, classId, body);
    }

    @GetMapping("/classes/{classId}/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get quizzes for a live class (session members only)")
    public ResponseEntity<ApiResponse<List<LiveClassQuiz>>> getQuizzes(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId) {
        return interactionService.getQuizzes(userId, classId);
    }

    @GetMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get quiz questions — the correct answer is included for the teacher only")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuizQuestions(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID quizId,
            Authentication authentication) {
        return interactionService.getQuizQuestions(userId, quizId, isTeacher(authentication));
    }

    @PostMapping("/quizzes/{quizId}/respond")
    @PreAuthorize("hasAnyRole('OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Submit quiz responses (participants only, one submission per quiz, server-side scoring)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitQuizResponse(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID quizId,
            @RequestBody List<Map<String, Object>> responses) {
        return interactionService.submitQuizResponse(userId, quizId, responses);
    }

    @GetMapping("/quizzes/{quizId}/results")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get quiz results (teacher of that class only): participants, answered, correct")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuizResults(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID quizId) {
        return interactionService.getQuizResults(userId, quizId);
    }

    @PostMapping("/quizzes/{quizId}/close")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Close a quiz (teacher of that class only, broadcasts QUIZ_CLOSED)")
    public ResponseEntity<ApiResponse<LiveClassQuiz>> closeQuiz(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID quizId) {
        return interactionService.closeQuiz(userId, quizId);
    }

    @GetMapping("/quizzes/{quizId}/my-responses")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "My own answers for one quiz (score only after the quiz is closed)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyQuizResponses(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID quizId) {
        return interactionService.getMyQuizResponses(userId, quizId);
    }

    // ==================== POLLS ====================

    @PostMapping("/classes/{classId}/polls")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create and launch a poll for a live class (validates, persists, broadcasts POLL_STARTED)")
    public ResponseEntity<ApiResponse<LiveClassPoll>> createPoll(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId,
            @RequestBody Map<String, Object> body) {
        return interactionService.createPoll(institutionId, userId, classId, body);
    }

    @GetMapping("/classes/{classId}/polls")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get polls for a live class (with my own vote + aggregate totals)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPolls(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId) {
        return interactionService.getPolls(userId, classId);
    }

    @PostMapping("/polls/{pollId}/vote")
    @PreAuthorize("hasAnyRole('OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Vote on a poll (participants only, option validated, broadcasts POLL_RESULT)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> votePoll(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID pollId,
            @RequestBody Map<String, Object> body) {
        return interactionService.votePoll(userId, pollId, body);
    }

    @PostMapping("/polls/{pollId}/close")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Close a poll (teacher of that class only, broadcasts POLL_CLOSED)")
    public ResponseEntity<ApiResponse<String>> closePoll(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID pollId) {
        return interactionService.closePoll(userId, pollId);
    }

    @GetMapping("/polls/{pollId}/results")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get poll results (real option count, aggregate counts only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPollResults(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID pollId) {
        return interactionService.getPollResults(userId, pollId);
    }

    // ==================== BREAKOUT ROOMS ====================

    @PostMapping("/classes/{classId}/breakout-rooms")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create a breakout room (teacher of that class only, broadcasts BREAKOUT_ROOMS_UPDATED)")
    public ResponseEntity<ApiResponse<LiveClassBreakoutRoom>> createBreakoutRoom(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId,
            @RequestBody Map<String, Object> body) {
        return interactionService.createBreakoutRoom(institutionId, userId, classId, body);
    }

    @PostMapping("/breakout-rooms/{roomId}/assign")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Assign a participant to a breakout room (capacity + one-room-per-class enforced)")
    public ResponseEntity<ApiResponse<String>> assignToBreakoutRoom(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID roomId,
            @RequestBody Map<String, String> body) {
        return interactionService.assignToBreakoutRoom(userId, roomId, body);
    }

    @PostMapping("/breakout-rooms/{roomId}/join")
    @PreAuthorize("hasAnyRole('OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Join your assigned breakout room (returns a breakout LiveKit token)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> joinBreakoutRoom(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID roomId) {
        return interactionService.joinBreakoutRoom(userId, roomId);
    }

    @GetMapping("/classes/{classId}/breakout-rooms")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get breakout rooms with assignments and per-viewer assignment state")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBreakoutRooms(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId) {
        return interactionService.getBreakoutRooms(userId, classId);
    }

    @PostMapping("/breakout-rooms/{roomId}/start")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Open a breakout room (teacher of that class only)")
    public ResponseEntity<ApiResponse<String>> startBreakoutRoom(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID roomId) {
        return interactionService.startBreakoutRoom(userId, roomId);
    }

    @PostMapping("/breakout-rooms/{roomId}/end")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Close a breakout room (teacher of that class only)")
    public ResponseEntity<ApiResponse<String>> endBreakoutRoom(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID roomId) {
        return interactionService.endBreakoutRoom(userId, roomId);
    }

    // ==================== SHARED MEDIA ====================

    @PostMapping("/classes/{classId}/shared-media")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Share media during live class")
    public ResponseEntity<ApiResponse<LiveClassSharedMedia>> shareMedia(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId,
            @RequestBody Map<String, Object> body) {

        LiveClassSharedMedia media = LiveClassSharedMedia.builder()
                .liveClassId(classId)
                .sharedBy(userId)
                .mediaType((String) body.getOrDefault("mediaType", "VIDEO"))
                .title((String) body.get("title"))
                .url((String) body.get("url"))
                .durationSeconds(body.get("durationSeconds") != null ? (Integer) body.get("durationSeconds") : null)
                .sharedAt(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(sharedMediaRepository.save(media)));
    }

    @GetMapping("/classes/{classId}/shared-media")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get shared media for a live class")
    public ResponseEntity<ApiResponse<List<LiveClassSharedMedia>>> getSharedMedia(
            @PathVariable UUID classId) {
        return ResponseEntity.ok(ApiResponse.success(sharedMediaRepository.findByLiveClassIdAndIsDeletedFalse(classId)));
    }

    // ==================== ATTENDANCE DETAIL ====================

    @PostMapping("/classes/{classId}/attendance-detail")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Record detailed attendance entry")
    public ResponseEntity<ApiResponse<LiveClassAttendanceDetail>> recordAttendanceDetail(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId,
            @RequestBody Map<String, Object> body) {

        LiveClassAttendanceDetail detail = LiveClassAttendanceDetail.builder()
                .liveClassId(classId)
                .userId(userId)
                .build();

        if (body.get("joinedAt") != null) detail.setJoinedAt(LocalDateTime.parse((String) body.get("joinedAt")));
        if (body.get("leftAt") != null) detail.setLeftAt(LocalDateTime.parse((String) body.get("leftAt")));
        if (body.get("totalSeconds") != null) detail.setTotalSeconds((Integer) body.get("totalSeconds"));
        if (body.get("percentage") != null) detail.setPercentage(new java.math.BigDecimal(body.get("percentage").toString()));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(attendanceDetailRepository.save(detail)));
    }

    @GetMapping("/classes/{classId}/attendance-detail")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get detailed attendance for a live class")
    public ResponseEntity<ApiResponse<List<LiveClassAttendanceDetail>>> getAttendanceDetail(
            @PathVariable UUID classId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceDetailRepository.findByLiveClassIdAndIsDeletedFalse(classId)));
    }
}
