package tz.elmkusoma.liveclass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.domain.*;
import tz.elmkusoma.liveclass.repository.*;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.service.TeacherService;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/v1/live-session")
@RequiredArgsConstructor
@Tag(name = "Live Class Interactions", description = "Quizzes, polls, breakout rooms, shared media, and attendance detail")
public class LiveClassInteractionController {

    private final ObjectMapper objectMapper;
    private final LiveClassRepository liveClassRepository;
    private final TeacherService teacherService;
    private final LiveClassQuizRepository quizRepository;
    private final LiveClassQuizQuestionRepository quizQuestionRepository;
    private final LiveClassQuizResponseRepository quizResponseRepository;
    private final LiveClassPollRepository pollRepository;
    private final LiveClassPollVoteRepository pollVoteRepository;
    private final LiveClassBreakoutRoomRepository breakoutRoomRepository;
    private final LiveClassBreakoutAssignmentRepository breakoutAssignmentRepository;
    private final LiveClassSharedMediaRepository sharedMediaRepository;
    private final LiveClassAttendanceDetailRepository attendanceDetailRepository;

    /** Poll/quiz options live in JSONB columns — persist canonical JSON, never Object#toString. */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    // ==================== QUIZZES ====================

    @PostMapping("/classes/{classId}/quizzes")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create a quiz for a live class")
    public ResponseEntity<ApiResponse<LiveClassQuiz>> createQuiz(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId,
            @RequestBody Map<String, Object> body) {

        LiveClass liveClass = liveClassRepository.findById(classId).filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted())).orElse(null);
        if (liveClass == null) return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        if (!liveClass.getInstitutionId().equals(institutionId)) return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));

        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        if (!liveClass.getTeacherId().equals(teacher.getId())) return ResponseEntity.status(403).body(ApiResponse.error("Only the teacher can create quizzes"));

        String title = (String) body.getOrDefault("title", "Untitled Quiz");

        LiveClassQuiz quiz = LiveClassQuiz.builder()
                .liveClassId(classId)
                .teacherId(userId)
                .title(title)
                .status("ACTIVE")
                .build();
        LiveClassQuiz saved = quizRepository.save(quiz);

        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        if (questions != null) {
            int order = 0;
            for (Map<String, Object> q : questions) {
                LiveClassQuizQuestion question = LiveClassQuizQuestion.builder()
                        .quizId(saved.getId())
                        .questionText((String) q.get("questionText"))
                        .questionType((String) q.getOrDefault("questionType", "MULTIPLE_CHOICE"))
                        .options(q.get("options") != null ? toJson(q.get("options")) : null)
                        .correctAnswer((String) q.get("correctAnswer"))
                        .displayOrder(order++)
                        .build();
                quizQuestionRepository.save(question);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(saved));
    }

    @GetMapping("/classes/{classId}/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get quizzes for a live class")
    public ResponseEntity<ApiResponse<List<LiveClassQuiz>>> getQuizzes(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId) {
        return ResponseEntity.ok(ApiResponse.success(quizRepository.findByLiveClassIdAndIsDeletedFalse(classId)));
    }

    @GetMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get quiz questions")
    public ResponseEntity<ApiResponse<List<LiveClassQuizQuestion>>> getQuizQuestions(
            @PathVariable UUID quizId) {
        return ResponseEntity.ok(ApiResponse.success(quizQuestionRepository.findByQuizIdAndIsDeletedFalseOrderByDisplayOrderAsc(quizId)));
    }

    @PostMapping("/quizzes/{quizId}/respond")
    @PreAuthorize("hasAnyRole('OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Submit quiz response")
    public ResponseEntity<ApiResponse<String>> submitQuizResponse(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID quizId,
            @RequestBody List<Map<String, Object>> responses) {

        for (Map<String, Object> resp : responses) {
            UUID questionId = UUID.fromString((String) resp.get("questionId"));
            String answer = (String) resp.get("answer");

            LiveClassQuizQuestion question = quizQuestionRepository.findById(questionId).orElse(null);
            if (question == null) continue;

            boolean isCorrect = answer != null && answer.equalsIgnoreCase(question.getCorrectAnswer());

            LiveClassQuizResponse quizResponse = LiveClassQuizResponse.builder()
                    .quizId(quizId)
                    .questionId(questionId)
                    .userId(userId)
                    .answerText(answer)
                    .isCorrect(isCorrect)
                    .respondedAt(LocalDateTime.now())
                    .build();
            quizResponseRepository.save(quizResponse);
        }

        return ResponseEntity.ok(ApiResponse.success("Responses submitted", null));
    }

    @GetMapping("/quizzes/{quizId}/results")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get quiz results (teacher only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuizResults(
            @PathVariable UUID quizId) {

        List<LiveClassQuizResponse> responses = quizResponseRepository.findByQuizIdAndIsDeletedFalse(quizId);
        long totalCorrect = responses.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        long totalResponses = responses.size();

        Map<String, Object> results = new HashMap<>();
        results.put("totalResponses", totalResponses);
        results.put("totalCorrect", totalCorrect);
        results.put("accuracy", totalResponses > 0 ? (double) totalCorrect / totalResponses * 100 : 0);
        results.put("responses", responses);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ==================== POLLS ====================

    @PostMapping("/classes/{classId}/polls")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create a poll for a live class")
    public ResponseEntity<ApiResponse<LiveClassPoll>> createPoll(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId,
            @RequestBody Map<String, Object> body) {

        LiveClass liveClass = liveClassRepository.findById(classId).filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted())).orElse(null);
        if (liveClass == null) return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        if (!liveClass.getInstitutionId().equals(institutionId)) return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));

        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        if (!liveClass.getTeacherId().equals(teacher.getId())) return ResponseEntity.status(403).body(ApiResponse.error("Only the teacher can create polls"));

        String question = (String) body.get("question");
        Object options = body.get("options");

        LiveClassPoll poll = LiveClassPoll.builder()
                .liveClassId(classId)
                .teacherId(userId)
                .question(question)
                .options(options != null ? toJson(options) : "[]")
                .status("ACTIVE")
                .build();
        LiveClassPoll saved = pollRepository.save(poll);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(saved));
    }

    @PostMapping("/polls/{pollId}/vote")
    @PreAuthorize("hasAnyRole('OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Vote on a poll")
    public ResponseEntity<ApiResponse<String>> votePoll(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID pollId,
            @RequestBody Map<String, Object> body) {

        LiveClassPoll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) return ResponseEntity.status(404).body(ApiResponse.error("Poll not found"));
        if (!"ACTIVE".equals(poll.getStatus())) return ResponseEntity.status(400).body(ApiResponse.error("Poll is no longer active"));

        pollVoteRepository.findByPollIdAndIsDeletedFalse(pollId).stream()
                .filter(v -> v.getUserId().equals(userId))
                .findFirst()
                .ifPresent(v -> {
                    v.setOptionIndex((Integer) body.get("optionIndex"));
                    v.setVotedAt(LocalDateTime.now());
                    pollVoteRepository.save(v);
                });

        if (pollVoteRepository.findByPollIdAndIsDeletedFalse(pollId).stream().noneMatch(v -> v.getUserId().equals(userId))) {
            LiveClassPollVote vote = LiveClassPollVote.builder()
                    .pollId(pollId)
                    .userId(userId)
                    .optionIndex((Integer) body.get("optionIndex"))
                    .votedAt(LocalDateTime.now())
                    .build();
            pollVoteRepository.save(vote);
        }

        return ResponseEntity.ok(ApiResponse.success("Vote recorded", null));
    }

    @PostMapping("/polls/{pollId}/close")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Close a poll")
    public ResponseEntity<ApiResponse<String>> closePoll(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID pollId) {

        LiveClassPoll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) return ResponseEntity.status(404).body(ApiResponse.error("Poll not found"));

        poll.setStatus("CLOSED");
        poll.setClosedAt(LocalDateTime.now());
        pollRepository.save(poll);

        return ResponseEntity.ok(ApiResponse.success("Poll closed", null));
    }

    @GetMapping("/polls/{pollId}/results")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get poll results")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPollResults(
            @PathVariable UUID pollId) {

        List<LiveClassPollVote> votes = pollVoteRepository.findByPollIdAndIsDeletedFalse(pollId);
        Map<String, Integer> voteCounts = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) voteCounts.put(String.valueOf(i), 0);
        for (LiveClassPollVote vote : votes) {
            voteCounts.merge(String.valueOf(vote.getOptionIndex()), 1, Integer::sum);
        }

        Map<String, Object> results = new HashMap<>();
        results.put("totalVotes", votes.size());
        results.put("results", voteCounts);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ==================== BREAKOUT ROOMS ====================

    @PostMapping("/classes/{classId}/breakout-rooms")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create a breakout room")
    public ResponseEntity<ApiResponse<LiveClassBreakoutRoom>> createBreakoutRoom(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID classId,
            @RequestBody Map<String, Object> body) {

        LiveClass liveClass = liveClassRepository.findById(classId).filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted())).orElse(null);
        if (liveClass == null) return ResponseEntity.status(404).body(ApiResponse.error("Live class not found"));
        if (!liveClass.getInstitutionId().equals(institutionId)) return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));

        String name = (String) body.getOrDefault("name", "Breakout Room");
        Integer maxParticipants = body.get("maxParticipants") != null ? (Integer) body.get("maxParticipants") : 10;

        LiveClassBreakoutRoom room = LiveClassBreakoutRoom.builder()
                .liveClassId(classId)
                .name(name)
                .maxParticipants(maxParticipants)
                .status("WAITING")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(breakoutRoomRepository.save(room)));
    }

    @PostMapping("/breakout-rooms/{roomId}/assign")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Assign participant to breakout room")
    public ResponseEntity<ApiResponse<String>> assignToBreakoutRoom(
            @PathVariable UUID roomId,
            @RequestBody Map<String, String> body) {

        UUID userId = UUID.fromString(body.get("userId"));
        LiveClassBreakoutRoom room = breakoutRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.status(404).body(ApiResponse.error("Breakout room not found"));

        LiveClassBreakoutAssignment assignment = LiveClassBreakoutAssignment.builder()
                .breakoutRoomId(roomId)
                .userId(userId)
                .assignedAt(LocalDateTime.now())
                .build();
        breakoutAssignmentRepository.save(assignment);

        return ResponseEntity.ok(ApiResponse.success("Assigned", null));
    }

    @PostMapping("/breakout-rooms/{roomId}/join")
    @PreAuthorize("hasAnyRole('OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Self-join a breakout room")
    public ResponseEntity<ApiResponse<String>> joinBreakoutRoom(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID roomId) {

        LiveClassBreakoutRoom room = breakoutRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.status(404).body(ApiResponse.error("Breakout room not found"));
        if (!"ACTIVE".equals(room.getStatus())) return ResponseEntity.status(400).body(ApiResponse.error("Breakout room is not active"));

        boolean alreadyAssigned = breakoutAssignmentRepository.findByBreakoutRoomIdAndIsDeletedFalse(roomId).stream()
                .anyMatch(a -> a.getUserId().equals(userId));
        if (alreadyAssigned) return ResponseEntity.status(409).body(ApiResponse.error("Already joined this room"));

        LiveClassBreakoutAssignment assignment = LiveClassBreakoutAssignment.builder()
                .breakoutRoomId(roomId)
                .userId(userId)
                .assignedAt(LocalDateTime.now())
                .build();
        breakoutAssignmentRepository.save(assignment);

        return ResponseEntity.ok(ApiResponse.success("Joined breakout room", null));
    }

    @GetMapping("/classes/{classId}/breakout-rooms")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','STUDENT')")
    @Operation(summary = "Get breakout rooms for a live class")
    public ResponseEntity<ApiResponse<List<LiveClassBreakoutRoom>>> getBreakoutRooms(
            @PathVariable UUID classId) {
        return ResponseEntity.ok(ApiResponse.success(breakoutRoomRepository.findByLiveClassIdAndIsDeletedFalse(classId)));
    }

    @PostMapping("/breakout-rooms/{roomId}/start")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Start a breakout room")
    public ResponseEntity<ApiResponse<String>> startBreakoutRoom(@PathVariable UUID roomId) {
        LiveClassBreakoutRoom room = breakoutRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.status(404).body(ApiResponse.error("Not found"));
        room.setStatus("ACTIVE");
        breakoutRoomRepository.save(room);
        return ResponseEntity.ok(ApiResponse.success("Breakout room started", null));
    }

    @PostMapping("/breakout-rooms/{roomId}/end")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "End a breakout room")
    public ResponseEntity<ApiResponse<String>> endBreakoutRoom(@PathVariable UUID roomId) {
        LiveClassBreakoutRoom room = breakoutRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.status(404).body(ApiResponse.error("Not found"));
        room.setStatus("ENDED");
        breakoutRoomRepository.save(room);
        return ResponseEntity.ok(ApiResponse.success("Breakout room ended", null));
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
