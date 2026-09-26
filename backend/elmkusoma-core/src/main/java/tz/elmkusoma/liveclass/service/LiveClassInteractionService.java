package tz.elmkusoma.liveclass.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.domain.*;
import tz.elmkusoma.liveclass.handler.LiveClassWebSocketHandler;
import tz.elmkusoma.liveclass.repository.*;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Live Quiz / Live Poll / Breakout Rooms lifecycle (§4 architecture):
 * TEACHER ACTION → BACKEND VALIDATION → PERSISTENCE → REALTIME EVENT → both UIs.
 *
 * Everything runs against the existing V61 tables and the existing live-class
 * WebSocket ({@link LiveClassWebSocketHandler#publishToClass} is the REST→WS bridge),
 * so no second realtime transport exists. Every payload published to students is
 * stripped of correct answers; scoring happens only here, on the server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveClassInteractionService {

    private final ObjectMapper objectMapper;
    private final LiveClassRepository liveClassRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final LiveClassQuizRepository quizRepository;
    private final LiveClassQuizQuestionRepository quizQuestionRepository;
    private final LiveClassQuizResponseRepository quizResponseRepository;
    private final LiveClassPollRepository pollRepository;
    private final LiveClassPollVoteRepository pollVoteRepository;
    private final LiveClassBreakoutRoomRepository breakoutRoomRepository;
    private final LiveClassBreakoutAssignmentRepository breakoutAssignmentRepository;
    private final LiveClassParticipantRepository participantRepository;
    private final LiveClassWebSocketHandler webSocketHandler;
    private final LiveKitService liveKitService;

    // ==================== shared helpers ====================

    private static <T> ResponseEntity<ApiResponse<T>> err(int status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private LiveClass loadClass(UUID classId) {
        return liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
    }

    /** True when the caller IS the teacher of this class. Read-only lookup — never creates rows. */
    private boolean isClassTeacher(LiveClass lc, UUID userId) {
        if (lc.getTeacherId() == null || userId == null) return false;
        Optional<Teacher> teacher = teacherRepository.findByUserIdAndInstitutionId(userId, lc.getInstitutionId());
        return teacher.isPresent() && lc.getTeacherId().equals(teacher.get().getId());
    }

    /**
     * Session-scoped read access: you may read this class's interaction state only if
     * you are its teacher or a recorded participant of it (participant rows are
     * created by the WS JOIN handshake). This is what keeps a student of session B
     * from reading session A's quizzes/polls/breakouts even inside one institution.
     */
    private boolean canReadClass(LiveClass lc, UUID userId) {
        if (isClassTeacher(lc, userId)) return true;
        return participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(lc.getId(), userId);
    }

    /** Student writes (answer/vote/join) require an existing participant row of THIS class. */
    private boolean isParticipant(UUID classId, UUID userId) {
        return participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, userId);
    }

    /**
     * Publishes a typed event over the existing class socket. When called inside an
     * open transaction the broadcast is deferred to afterCommit, so a client reacting
     * to the event can never read uncommitted rows.
     */
    private void publishAfterCommit(UUID classId, Map<String, Object> event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    webSocketHandler.publishToClass(classId, event);
                }
            });
        } else {
            webSocketHandler.publishToClass(classId, event);
        }
    }

    /** Poll/quiz options live in JSONB — persist canonical JSON, never Object#toString. */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * Normalizes option input (raw "Yes,No" string, JSON-array text, or a parsed list)
     * to canonical JSON-array text: ["Yes","No"]. The value stored via {@link #toJson}
     * keeps the exact JSONB string-scalar mechanism the existing tables were written
     * with, while every API surface below emits the canonical text so clients never
     * have to guess the storage shape.
     */
    private String canonicalOptionsText(Object raw) {
        return toJson(normalizeOptions(raw));
    }

    /** Reads stored option input back into a list; tolerant of every legacy storage shape. */
    private List<String> normalizeOptions(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                String t = String.valueOf(o).trim();
                if (!t.isEmpty()) out.add(t);
            }
            return out;
        }
        String s = String.valueOf(raw).trim();
        // One unwrap round: storage may hold a quoted/escaped JSON string scalar.
        if (s.startsWith("\"")) {
            try {
                s = objectMapper.readValue(s, String.class).trim();
            } catch (Exception ignored) {
                // keep the raw text — comma split below still handles it
            }
        }
        if (s.startsWith("[")) {
            try {
                List<String> parsed = objectMapper.readValue(s, new TypeReference<List<String>>() {
                });
                for (String p : parsed) {
                    if (p == null) continue;
                    String t = p.trim();
                    if (!t.isEmpty()) out.add(t);
                }
                return out;
            } catch (Exception ignored) {
                // fall through to comma split
            }
        }
        for (String p : s.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** Unique student participants of a class (participant rows carry role TEACHER too). */
    private long studentParticipantCount(UUID classId) {
        return participantRepository.findByLiveClassIdAndIsDeletedFalse(classId).stream()
                .filter(p -> !"TEACHER".equals(p.getRole()))
                .map(LiveClassParticipant::getUserId)
                .distinct()
                .count();
    }

    private LiveClassQuiz loadQuiz(UUID quizId) {
        return quizRepository.findById(quizId)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElse(null);
    }

    private LiveClassPoll loadPoll(UUID pollId) {
        return pollRepository.findById(pollId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElse(null);
    }

    private LiveClassBreakoutRoom loadRoom(UUID roomId) {
        return breakoutRoomRepository.findById(roomId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElse(null);
    }

    /** Option counts for one poll: keys "0".."n-1" derived from the REAL option count. */
    private Map<String, Integer> pollCounts(LiveClassPoll poll) {
        List<String> options = normalizeOptions(poll.getOptions());
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < options.size(); i++) counts.put(String.valueOf(i), 0);
        for (LiveClassPollVote vote : pollVoteRepository.findByPollIdAndIsDeletedFalse(poll.getId())) {
            Integer idx = vote.getOptionIndex();
            if (idx != null && idx >= 0 && idx < options.size()) {
                counts.merge(String.valueOf(idx), 1, Integer::sum);
            }
        }
        return counts;
    }

    /** Server-side view of every breakout room of a class + who is in each (§30). */
    private List<Map<String, Object>> breakoutRoomViews(UUID classId, UUID viewerUserId) {
        List<LiveClassBreakoutRoom> rooms = breakoutRoomRepository.findByLiveClassIdAndIsDeletedFalse(classId);
        List<LiveClassBreakoutAssignment> assignments = breakoutAssignmentRepository.findByLiveClassIdAndIsDeletedFalse(classId);
        Map<UUID, List<Map<String, Object>>> byRoom = new HashMap<>();
        UUID myRoomId = null;
        for (LiveClassBreakoutAssignment a : assignments) {
            if (a.getUserId().equals(viewerUserId)) myRoomId = a.getBreakoutRoomId();
            User u = userRepository.findById(a.getUserId()).orElse(null);
            Map<String, Object> who = new HashMap<>();
            who.put("userId", a.getUserId().toString());
            who.put("userName", u != null ? u.getFullName() : "Participant");
            byRoom.computeIfAbsent(a.getBreakoutRoomId(), k -> new ArrayList<>()).add(who);
        }
        List<Map<String, Object>> views = new ArrayList<>();
        for (LiveClassBreakoutRoom room : rooms) {
            List<Map<String, Object>> who = byRoom.getOrDefault(room.getId(), List.of());
            Map<String, Object> view = new HashMap<>();
            view.put("id", room.getId().toString());
            view.put("liveClassId", room.getLiveClassId().toString());
            view.put("name", room.getName());
            view.put("maxParticipants", room.getMaxParticipants());
            view.put("status", room.getStatus());
            view.put("assignedCount", who.size());
            view.put("assignedUsers", who);
            view.put("assignedToMe", room.getId().equals(myRoomId));
            views.add(view);
        }
        return views;
    }

    private void publishBreakoutUpdate(UUID classId, UUID viewerUserId) {
        webSocketHandler.publishToClass(classId, breakoutEvent(classId, viewerUserId));
    }

    // ==================== QUIZZES ====================

    @Transactional
    public ResponseEntity<ApiResponse<LiveClassQuiz>> createQuiz(
            UUID institutionId, UUID userId, UUID classId, Map<String, Object> body) {
        LiveClass lc = loadClass(classId);
        if (lc == null) return err(404, "Live class not found");
        if (!lc.getInstitutionId().equals(institutionId)) return err(403, "Access denied");
        if (!isClassTeacher(lc, userId)) return err(403, "Only the teacher can create quizzes");

        String title = body.get("title") != null ? String.valueOf(body.get("title")).trim() : "";
        if (title.isEmpty()) return err(400, "Quiz title is required");

        Object rawQuestions = body.get("questions");
        if (!(rawQuestions instanceof List<?> list) || list.isEmpty()) {
            return err(400, "Add at least one question");
        }
        List<Map<String, Object>> questions = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map)) return err(400, "Invalid question payload");
            @SuppressWarnings("unchecked")
            Map<String, Object> q = (Map<String, Object>) o;
            if (str(q.get("questionText")).isEmpty()
                    || str(q.get("correctAnswer")).isEmpty()
                    || normalizeOptions(q.get("options")).isEmpty()) {
                return err(400, "Each question needs text, options and a correct answer");
            }
            questions.add(q);
        }

        LiveClassQuiz saved = quizRepository.save(LiveClassQuiz.builder()
                .liveClassId(classId)
                .teacherId(userId)
                .title(title)
                .status("ACTIVE")
                .build());

        int order = 0;
        for (Map<String, Object> q : questions) {
            quizQuestionRepository.save(LiveClassQuizQuestion.builder()
                    .quizId(saved.getId())
                    .questionText(str(q.get("questionText")).trim())
                    .questionType(str(q.get("questionType")).isEmpty() ? "MULTIPLE_CHOICE" : str(q.get("questionType")).trim())
                    .options(canonicalOptionsText(q.get("options")))
                    .correctAnswer(str(q.get("correctAnswer")).trim())
                    .displayOrder(order++)
                    .build());
        }

        // Students receive the announcement and fetch questions over REST — the event
        // itself never carries question content, so the answer key cannot leak here.
        Map<String, Object> event = new HashMap<>();
        event.put("type", "QUIZ_STARTED");
        event.put("quizId", saved.getId().toString());
        event.put("title", saved.getTitle());
        event.put("questionCount", questions.size());
        event.put("timestamp", LocalDateTime.now().toString());
        publishAfterCommit(classId, event);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(saved));
    }

    public ResponseEntity<ApiResponse<List<LiveClassQuiz>>> getQuizzes(UUID userId, UUID classId) {
        LiveClass lc = loadClass(classId);
        if (lc == null) return err(404, "Live class not found");
        if (!canReadClass(lc, userId)) return err(403, "Access denied");
        return ok(quizRepository.findByLiveClassIdAndIsDeletedFalse(classId));
    }

    /**
     * Question payload per viewer: the correct answer is returned ONLY to the class
     * teacher — students get their own submitted answer (and its score only once the
     * quiz is CLOSED, i.e. after evaluation), never the key.
     */
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuizQuestions(
            UUID userId, UUID quizId, boolean teacherView) {
        LiveClassQuiz quiz = loadQuiz(quizId);
        if (quiz == null) return err(404, "Quiz not found");
        LiveClass lc = loadClass(quiz.getLiveClassId());
        if (lc == null) return err(404, "Live class not found");
        if (!canReadClass(lc, userId)) return err(403, "Access denied");

        boolean closed = "CLOSED".equals(quiz.getStatus());
        Map<UUID, LiveClassQuizResponse> mine = new HashMap<>();
        if (!teacherView) {
            for (LiveClassQuizResponse r : quizResponseRepository.findByUserIdAndQuizIdAndIsDeletedFalse(userId, quizId)) {
                mine.put(r.getQuestionId(), r);
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (LiveClassQuizQuestion q : quizQuestionRepository.findByQuizIdAndIsDeletedFalseOrderByDisplayOrderAsc(quizId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", q.getId().toString());
            m.put("questionText", q.getQuestionText());
            m.put("questionType", q.getQuestionType());
            m.put("options", canonicalOptionsText(q.getOptions()));
            m.put("displayOrder", q.getDisplayOrder());
            if (teacherView) {
                m.put("correctAnswer", q.getCorrectAnswer());
            } else {
                LiveClassQuizResponse own = mine.get(q.getId());
                if (own != null) {
                    m.put("myAnswer", own.getAnswerText());
                    if (closed) m.put("isCorrect", Boolean.TRUE.equals(own.getIsCorrect()));
                }
            }
            out.add(m);
        }
        return ok(out);
    }

    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitQuizResponse(
            UUID userId, UUID quizId, List<Map<String, Object>> responses) {
        LiveClassQuiz quiz = loadQuiz(quizId);
        if (quiz == null) return err(404, "Quiz not found");
        if (!isParticipant(quiz.getLiveClassId(), userId)) {
            return err(403, "Join the live class session first");
        }
        if (!"ACTIVE".equals(quiz.getStatus())) return err(409, "This quiz is closed");
        if (responses == null || responses.isEmpty()) return err(400, "No responses submitted");

        // One submission per user per quiz — duplicated answers cannot inflate results.
        if (!quizResponseRepository.findByUserIdAndQuizIdAndIsDeletedFalse(userId, quizId).isEmpty()) {
            return err(409, "You have already submitted responses for this quiz");
        }

        // Validate every response before writing anything (all-or-nothing).
        List<LiveClassQuizQuestion> questionEntities = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        for (Map<String, Object> resp : responses) {
            String rawId = str(resp.get("questionId"));
            if (rawId.isEmpty()) return err(400, "Each response needs a questionId");
            UUID questionId;
            try {
                questionId = UUID.fromString(rawId);
            } catch (IllegalArgumentException e) {
                return err(400, "Invalid question id");
            }
            LiveClassQuizQuestion question = quizQuestionRepository.findById(questionId).orElse(null);
            if (question == null || Boolean.TRUE.equals(question.getIsDeleted())
                    || !quizId.equals(question.getQuizId())) {
                return err(400, "Question does not belong to this quiz");
            }
            questionEntities.add(question);
            answers.add(resp.get("answer") != null ? String.valueOf(resp.get("answer")) : null);
        }

        for (int i = 0; i < questionEntities.size(); i++) {
            LiveClassQuizQuestion question = questionEntities.get(i);
            String answer = answers.get(i);
            // Server-side scoring: normalized comparison against the stored key.
            boolean isCorrect = answer != null
                    && question.getCorrectAnswer() != null
                    && answer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
            quizResponseRepository.save(LiveClassQuizResponse.builder()
                    .quizId(quizId)
                    .questionId(question.getId())
                    .userId(userId)
                    .answerText(answer)
                    .isCorrect(isCorrect)
                    .respondedAt(LocalDateTime.now())
                    .build());
        }

        // Aggregate-only progress event (no user identities, no answers): the teacher's
        // answered/total/correct counters update live for everyone on the class socket.
        List<LiveClassQuizResponse> all = quizResponseRepository.findByQuizIdAndIsDeletedFalse(quizId);
        long totalCorrect = all.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        Map<String, Object> event = new HashMap<>();
        event.put("type", "QUIZ_RESULT");
        event.put("quizId", quizId.toString());
        event.put("totalResponses", all.size());
        event.put("totalCorrect", totalCorrect);
        event.put("answeredCount", all.stream().map(LiveClassQuizResponse::getUserId).distinct().count());
        event.put("participantCount", studentParticipantCount(quiz.getLiveClassId()));
        event.put("timestamp", LocalDateTime.now().toString());
        publishAfterCommit(quiz.getLiveClassId(), event);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Responses submitted");
        data.put("submittedCount", questionEntities.size());
        return ok(data);
    }

    /** Teacher results: participants / answered / not answered / correct / incorrect (§15). */
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuizResults(UUID userId, UUID quizId) {
        LiveClassQuiz quiz = loadQuiz(quizId);
        if (quiz == null) return err(404, "Quiz not found");
        LiveClass lc = loadClass(quiz.getLiveClassId());
        if (lc == null) return err(404, "Live class not found");
        if (!isClassTeacher(lc, userId)) return err(403, "Only the teacher can view quiz results");

        List<LiveClassQuizResponse> responses = quizResponseRepository.findByQuizIdAndIsDeletedFalse(quizId);
        long totalCorrect = responses.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        long totalResponses = responses.size();

        Set<UUID> responded = new HashSet<>();
        for (LiveClassQuizResponse r : responses) responded.add(r.getUserId());

        Set<UUID> students = new LinkedHashSet<>();
        List<LiveClassParticipant> participants = participantRepository.findByLiveClassIdAndIsDeletedFalse(quiz.getLiveClassId());
        for (LiveClassParticipant p : participants) {
            if (!"TEACHER".equals(p.getRole())) students.add(p.getUserId());
        }
        long answeredCount = responded.stream().filter(students::contains).count();
        long notAnswered = students.size() - answeredCount;

        Map<String, Object> results = new HashMap<>();
        results.put("totalResponses", totalResponses);
        results.put("totalCorrect", totalCorrect);
        results.put("totalIncorrect", totalResponses - totalCorrect);
        results.put("accuracy", totalResponses > 0 ? (double) totalCorrect / totalResponses * 100 : 0);
        results.put("participantCount", students.size());
        results.put("answeredCount", answeredCount);
        results.put("notAnsweredCount", Math.max(0, notAnswered));
        results.put("respondedUserIds", responded.stream().map(UUID::toString).toList());
        results.put("responses", responses);
        results.put("status", quiz.getStatus());
        return ok(results);
    }

    /** Teacher closes the quiz: submissions stop, students keep seeing it as closed. */
    public ResponseEntity<ApiResponse<LiveClassQuiz>> closeQuiz(UUID userId, UUID quizId) {
        LiveClassQuiz quiz = loadQuiz(quizId);
        if (quiz == null) return err(404, "Quiz not found");
        LiveClass lc = loadClass(quiz.getLiveClassId());
        if (lc == null) return err(404, "Live class not found");
        if (!isClassTeacher(lc, userId)) return err(403, "Only the teacher can close this quiz");

        if ("CLOSED".equals(quiz.getStatus())) return ok(quiz); // idempotent

        quiz.setStatus("CLOSED");
        quizRepository.save(quiz);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "QUIZ_CLOSED");
        event.put("quizId", quiz.getId().toString());
        event.put("title", quiz.getTitle());
        event.put("timestamp", LocalDateTime.now().toString());
        webSocketHandler.publishToClass(quiz.getLiveClassId(), event);
        return ok(quiz);
    }

    /** A student's own answers for one quiz — used to restore submitted state on refresh. */
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyQuizResponses(UUID userId, UUID quizId) {
        LiveClassQuiz quiz = loadQuiz(quizId);
        if (quiz == null) return err(404, "Quiz not found");
        LiveClass lc = loadClass(quiz.getLiveClassId());
        if (lc == null) return err(404, "Live class not found");
        if (!canReadClass(lc, userId)) return err(403, "Access denied");

        boolean closed = "CLOSED".equals(quiz.getStatus());
        List<LiveClassQuizResponse> own =
                quizResponseRepository.findByUserIdAndQuizIdAndIsDeletedFalse(userId, quizId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (LiveClassQuizResponse r : own) {
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", r.getQuestionId().toString());
            m.put("answerText", r.getAnswerText());
            m.put("respondedAt", r.getRespondedAt() != null ? r.getRespondedAt().toString() : null);
            // Score only after evaluation (quiz closed); never the answer key itself.
            if (closed) m.put("isCorrect", Boolean.TRUE.equals(r.getIsCorrect()));
            list.add(m);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("quizId", quizId.toString());
        data.put("status", quiz.getStatus());
        data.put("submitted", !own.isEmpty());
        data.put("responses", list);
        return ok(data);
    }

    // ==================== POLLS ====================

    @Transactional
    public ResponseEntity<ApiResponse<LiveClassPoll>> createPoll(
            UUID institutionId, UUID userId, UUID classId, Map<String, Object> body) {
        LiveClass lc = loadClass(classId);
        if (lc == null) return err(404, "Live class not found");
        if (!lc.getInstitutionId().equals(institutionId)) return err(403, "Access denied");
        if (!isClassTeacher(lc, userId)) return err(403, "Only the teacher can create polls");

        String question = body.get("question") != null ? String.valueOf(body.get("question")).trim() : "";
        if (question.isEmpty()) return err(400, "Poll question is required");

        List<String> options = normalizeOptions(body.get("options"));
        if (options.size() < 2) return err(400, "A poll needs at least two options");

        LiveClassPoll saved = pollRepository.save(LiveClassPoll.builder()
                .liveClassId(classId)
                .teacherId(userId)
                .question(question)
                .options(toJson(canonicalOptionsText(options)))
                .status("ACTIVE")
                .build());

        Map<String, Object> event = new HashMap<>();
        event.put("type", "POLL_STARTED");
        event.put("pollId", saved.getId().toString());
        event.put("question", saved.getQuestion());
        event.put("options", canonicalOptionsText(options));
        event.put("status", saved.getStatus());
        event.put("timestamp", LocalDateTime.now().toString());
        publishAfterCommit(classId, event);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(saved));
    }

    /**
     * Poll list for refresh/restore. Includes the caller's own vote (their choice is
     * theirs to see) and aggregate totals — never who voted for what.
     */
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPolls(UUID userId, UUID classId) {
        LiveClass lc = loadClass(classId);
        if (lc == null) return err(404, "Live class not found");
        if (!canReadClass(lc, userId)) return err(403, "Access denied");

        List<Map<String, Object>> out = new ArrayList<>();
        for (LiveClassPoll poll : pollRepository.findByLiveClassIdAndIsDeletedFalse(classId)) {
            Integer myVote = null;
            int totalVotes = 0;
            for (LiveClassPollVote vote : pollVoteRepository.findByPollIdAndIsDeletedFalse(poll.getId())) {
                totalVotes++;
                if (userId.equals(vote.getUserId())) myVote = vote.getOptionIndex();
            }
            Map<String, Object> m = new HashMap<>();
            m.put("id", poll.getId().toString());
            m.put("question", poll.getQuestion());
            m.put("options", canonicalOptionsText(poll.getOptions()));
            m.put("status", poll.getStatus());
            m.put("closedAt", poll.getClosedAt() != null ? poll.getClosedAt().toString() : null);
            m.put("myVote", myVote);
            m.put("totalVotes", totalVotes);
            out.add(m);
        }
        return ok(out);
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> votePoll(
            UUID userId, UUID pollId, Map<String, Object> body) {
        LiveClassPoll poll = loadPoll(pollId);
        if (poll == null) return err(404, "Poll not found");
        if (!isParticipant(poll.getLiveClassId(), userId)) {
            return err(403, "Join the live class session first");
        }
        if (!"ACTIVE".equals(poll.getStatus())) return err(409, "This poll is no longer active");

        Object rawIndex = body.get("optionIndex");
        if (!(rawIndex instanceof Number)) return err(400, "optionIndex is required");
        int optionIndex = ((Number) rawIndex).intValue();
        List<String> options = normalizeOptions(poll.getOptions());
        if (optionIndex < 0 || optionIndex >= options.size()) {
            return err(400, "Invalid poll option");
        }

        // Change-of-answer is an existing, explicit product behavior: one row per
        // user per poll is updated in place, so votes can change but never duplicate.
        LiveClassPollVote existing = pollVoteRepository.findByPollIdAndIsDeletedFalse(pollId).stream()
                .filter(v -> userId.equals(v.getUserId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.setOptionIndex(optionIndex);
            existing.setVotedAt(LocalDateTime.now());
            pollVoteRepository.save(existing);
        } else {
            pollVoteRepository.save(LiveClassPollVote.builder()
                    .pollId(pollId)
                    .userId(userId)
                    .optionIndex(optionIndex)
                    .votedAt(LocalDateTime.now())
                    .build());
        }

        // Aggregate counts only — no voter identities leave this server.
        Map<String, Object> event = new HashMap<>();
        event.put("type", "POLL_RESULT");
        event.put("pollId", pollId.toString());
        Map<String, Integer> counts = pollCounts(poll);
        event.put("results", counts);
        event.put("totalVotes", counts.values().stream().mapToInt(Integer::intValue).sum());
        event.put("timestamp", LocalDateTime.now().toString());
        webSocketHandler.publishToClass(poll.getLiveClassId(), event);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Vote recorded");
        data.put("myVote", optionIndex);
        return ok(data);
    }

    public ResponseEntity<ApiResponse<String>> closePoll(UUID userId, UUID pollId) {
        LiveClassPoll poll = loadPoll(pollId);
        if (poll == null) return err(404, "Poll not found");
        LiveClass lc = loadClass(poll.getLiveClassId());
        if (lc == null) return err(404, "Live class not found");
        if (!isClassTeacher(lc, userId)) return err(403, "Only the teacher can close this poll");

        if (!"CLOSED".equals(poll.getStatus())) {
            poll.setStatus("CLOSED");
            poll.setClosedAt(LocalDateTime.now());
            pollRepository.save(poll);
        }

        // Final tally rides the close event so every client can render the closed
        // state with results without a second request.
        Map<String, Object> event = new HashMap<>();
        event.put("type", "POLL_CLOSED");
        event.put("pollId", pollId.toString());
        event.put("question", poll.getQuestion());
        event.put("results", pollCounts(poll));
        event.put("timestamp", LocalDateTime.now().toString());
        webSocketHandler.publishToClass(poll.getLiveClassId(), event);

        return ResponseEntity.ok(ApiResponse.success("Poll closed", null));
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> getPollResults(UUID userId, UUID pollId) {
        LiveClassPoll poll = loadPoll(pollId);
        if (poll == null) return err(404, "Poll not found");
        LiveClass lc = loadClass(poll.getLiveClassId());
        if (lc == null) return err(404, "Live class not found");
        if (!canReadClass(lc, userId)) return err(403, "Access denied");

        Map<String, Integer> counts = pollCounts(poll);
        Map<String, Object> results = new HashMap<>();
        results.put("pollId", pollId.toString());
        results.put("question", poll.getQuestion());
        results.put("options", canonicalOptionsText(poll.getOptions()));
        results.put("status", poll.getStatus());
        results.put("totalVotes", counts.values().stream().mapToInt(Integer::intValue).sum());
        results.put("results", counts);
        Integer myVote = pollVoteRepository.findByPollIdAndIsDeletedFalse(pollId).stream()
                .filter(v -> userId.equals(v.getUserId()))
                .map(LiveClassPollVote::getOptionIndex)
                .findFirst()
                .orElse(null);
        results.put("myVote", myVote);
        return ok(results);
    }

    // ==================== BREAKOUT ROOMS ====================

    @Transactional
    public ResponseEntity<ApiResponse<LiveClassBreakoutRoom>> createBreakoutRoom(
            UUID institutionId, UUID userId, UUID classId, Map<String, Object> body) {
        LiveClass lc = loadClass(classId);
        if (lc == null) return err(404, "Live class not found");
        if (!lc.getInstitutionId().equals(institutionId)) return err(403, "Access denied");
        if (!isClassTeacher(lc, userId)) return err(403, "Only the teacher can create breakout rooms");

        String name = body.get("name") != null ? String.valueOf(body.get("name")).trim() : "";
        if (name.isEmpty()) return err(400, "Room name is required");

        int max = 10;
        if (body.get("maxParticipants") instanceof Number n) max = n.intValue();
        if (max < 1) return err(400, "Max participants must be at least 1");

        LiveClassBreakoutRoom saved = breakoutRoomRepository.save(LiveClassBreakoutRoom.builder()
                .liveClassId(classId)
                .name(name)
                .maxParticipants(max)
                .status("WAITING")
                .build());

        // Built inside the transaction (auto-flush sees the new room), broadcast after
        // commit so every client that reacts by fetching state reads committed rows.
        Map<String, Object> event = breakoutEvent(classId, userId);
        publishAfterCommit(classId, event);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(saved));
    }

    /** Every breakout event carries the full room state so clients render server truth. */
    private Map<String, Object> breakoutEvent(UUID classId, UUID viewerUserId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "BREAKOUT_ROOMS_UPDATED");
        event.put("rooms", breakoutRoomViews(classId, viewerUserId));
        event.put("timestamp", LocalDateTime.now().toString());
        return event;
    }

    public ResponseEntity<ApiResponse<String>> assignToBreakoutRoom(
            UUID userId, UUID roomId, Map<String, String> body) {
        LiveClassBreakoutRoom room = loadRoom(roomId);
        if (room == null) return err(404, "Breakout room not found");
        LiveClass lc = loadClass(room.getLiveClassId());
        if (lc == null) return err(404, "Live class not found");
        if (!isClassTeacher(lc, userId)) return err(403, "Only the teacher can assign participants");

        UUID targetId;
        try {
            targetId = UUID.fromString(body.get("userId"));
        } catch (Exception e) {
            return err(400, "userId is required");
        }
        if (!isParticipant(lc.getId(), targetId)) {
            return err(400, "User is not a participant of this live class");
        }

        List<LiveClassBreakoutAssignment> targetAssignments =
                breakoutAssignmentRepository.findByLiveClassIdAndIsDeletedFalse(lc.getId()).stream()
                        .filter(a -> a.getUserId().equals(targetId))
                        .toList();
        boolean inThisRoom = targetAssignments.stream()
                .anyMatch(a -> a.getBreakoutRoomId().equals(roomId));
        if (inThisRoom) return ResponseEntity.ok(ApiResponse.success("Already assigned", null));

        int assignedHere = (int) breakoutAssignmentRepository.findByBreakoutRoomIdAndIsDeletedFalse(roomId).size();
        if (assignedHere >= (room.getMaxParticipants() != null ? room.getMaxParticipants() : 10)) {
            return err(409, "Breakout room is full");
        }

        // One active room per participant per class: moving them soft-deletes the old
        // assignment (records are preserved, never physically deleted).
        for (LiveClassBreakoutAssignment old : targetAssignments) {
            old.setIsDeleted(true);
            breakoutAssignmentRepository.save(old);
        }
        breakoutAssignmentRepository.save(LiveClassBreakoutAssignment.builder()
                .breakoutRoomId(roomId)
                .userId(targetId)
                .assignedAt(LocalDateTime.now())
                .build());

        publishBreakoutUpdate(lc.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Assigned", null));
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> joinBreakoutRoom(UUID userId, UUID roomId) {
        LiveClassBreakoutRoom room = loadRoom(roomId);
        if (room == null) return err(404, "Breakout room not found");
        LiveClass lc = loadClass(room.getLiveClassId());
        if (lc == null) return err(404, "Live class not found");
        if (!isParticipant(lc.getId(), userId)) {
            return err(403, "Join the live class session first");
        }
        if (!"ACTIVE".equals(room.getStatus())) return err(409, "Breakout room is not open");

        List<LiveClassBreakoutAssignment> mine =
                breakoutAssignmentRepository.findByLiveClassIdAndIsDeletedFalse(lc.getId()).stream()
                        .filter(a -> a.getUserId().equals(userId))
                        .toList();
        UUID myRoomId = mine.stream().map(LiveClassBreakoutAssignment::getBreakoutRoomId).findFirst().orElse(null);

        if (myRoomId != null && !myRoomId.equals(roomId)) {
            // §33: a participant assigned elsewhere can never enter this room.
            return err(409, "You are assigned to a different breakout room");
        }

        boolean created = false;
        if (myRoomId == null) {
            int assignedHere = (int) breakoutAssignmentRepository.findByBreakoutRoomIdAndIsDeletedFalse(roomId).size();
            if (assignedHere >= (room.getMaxParticipants() != null ? room.getMaxParticipants() : 10)) {
                return err(409, "Breakout room is full");
            }
            breakoutAssignmentRepository.save(LiveClassBreakoutAssignment.builder()
                    .breakoutRoomId(roomId)
                    .userId(userId)
                    .assignedAt(LocalDateTime.now())
                    .build());
            created = true;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("message", created ? "Joined breakout room" : "Rejoined breakout room");
        data.put("roomId", roomId.toString());
        data.put("roomName", room.getName());
        data.put("breakoutLiveKitRoom", liveKitService.generateBreakoutRoomName(lc.getId(), roomId));
        boolean available = false;
        if (liveKitService.isAvailable()) {
            String token = liveKitService.generateBreakoutToken(lc.getId(), roomId, userId, userId.toString(), false);
            if (token != null) {
                available = true;
                data.put("liveKitToken", token);
                data.put("liveKitUrl", liveKitService.getServerUrl());
            }
        }
        data.put("liveKitAvailable", available);

        if (created) publishBreakoutUpdate(lc.getId(), userId);
        return ok(data);
    }

    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBreakoutRooms(UUID userId, UUID classId) {
        LiveClass lc = loadClass(classId);
        if (lc == null) return err(404, "Live class not found");
        if (!canReadClass(lc, userId)) return err(403, "Access denied");
        return ok(breakoutRoomViews(classId, userId));
    }

    public ResponseEntity<ApiResponse<String>> startBreakoutRoom(UUID userId, UUID roomId) {
        LiveClassBreakoutRoom room = loadRoom(roomId);
        if (room == null) return err(404, "Breakout room not found");
        LiveClass lc = loadClass(room.getLiveClassId());
        if (lc == null) return err(404, "Live class not found");
        if (!isClassTeacher(lc, userId)) return err(403, "Only the teacher can open breakout rooms");

        if ("ENDED".equals(room.getStatus())) return err(409, "This breakout room has already ended");
        if (!"ACTIVE".equals(room.getStatus())) {
            room.setStatus("ACTIVE");
            breakoutRoomRepository.save(room);
            publishBreakoutUpdate(lc.getId(), userId);
        }
        return ResponseEntity.ok(ApiResponse.success("Breakout room started", null));
    }

    public ResponseEntity<ApiResponse<String>> endBreakoutRoom(UUID userId, UUID roomId) {
        LiveClassBreakoutRoom room = loadRoom(roomId);
        if (room == null) return err(404, "Breakout room not found");
        LiveClass lc = loadClass(room.getLiveClassId());
        if (lc == null) return err(404, "Live class not found");
        if (!isClassTeacher(lc, userId)) return err(403, "Only the teacher can close breakout rooms");

        if (!"ENDED".equals(room.getStatus())) {
            room.setStatus("ENDED");
            breakoutRoomRepository.save(room);
            publishBreakoutUpdate(lc.getId(), userId);
        }
        return ResponseEntity.ok(ApiResponse.success("Breakout room ended", null));
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
