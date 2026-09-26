package tz.elmkusoma.liveclass.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.domain.*;
import tz.elmkusoma.liveclass.handler.LiveClassWebSocketHandler;
import tz.elmkusoma.liveclass.repository.*;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Live Quiz / Poll / Breakout lifecycle: validation, session-scoped
 * access control, server-side scoring, duplicate guards, and typed realtime events.
 */
@ExtendWith(MockitoExtension.class)
class LiveClassInteractionServiceTest {

    @Mock private LiveClassRepository liveClassRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private UserRepository userRepository;
    @Mock private LiveClassQuizRepository quizRepository;
    @Mock private LiveClassQuizQuestionRepository quizQuestionRepository;
    @Mock private LiveClassQuizResponseRepository quizResponseRepository;
    @Mock private LiveClassPollRepository pollRepository;
    @Mock private LiveClassPollVoteRepository pollVoteRepository;
    @Mock private LiveClassBreakoutRoomRepository breakoutRoomRepository;
    @Mock private LiveClassBreakoutAssignmentRepository breakoutAssignmentRepository;
    @Mock private LiveClassParticipantRepository participantRepository;
    @Mock private LiveClassWebSocketHandler webSocketHandler;
    @Mock private LiveKitService liveKitService;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private LiveClassInteractionService service;

    private UUID institutionId;
    private UUID teacherUserId;
    private UUID teacherProfileId;
    private UUID classId;
    private UUID studentId;

    private LiveClass lc;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        teacherUserId = UUID.randomUUID();
        teacherProfileId = UUID.randomUUID();
        classId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        lc = LiveClass.builder().institutionId(institutionId).teacherId(teacherProfileId).build();
        lc.setId(classId);
        teacher = teacherWith(teacherProfileId, teacherUserId);
    }

    /** Teacher's @Builder (not @SuperBuilder) omits BaseEntity#id — set it explicitly. */
    private static Teacher teacherWith(UUID profileId, UUID userId) {
        Teacher t = Teacher.builder().userId(userId).build();
        t.setId(profileId);
        return t;
    }

    private void asClassTeacher() {
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacher));
    }

    // ==================== CREATE QUIZ ====================

    @Test
    void createQuiz_blankTitle_rejected400() {
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        asClassTeacher();

        ResponseEntity<ApiResponse<LiveClassQuiz>> res = service.createQuiz(
                institutionId, teacherUserId, classId, Map.of("title", "  ", "questions", List.of()));

        assertEquals(400, res.getStatusCode().value());
        assertFalse(res.getBody().isSuccess());
        verify(quizRepository, never()).save(any());
    }

    @Test
    void createQuiz_noQuestions_rejected400() {
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        asClassTeacher();

        ResponseEntity<ApiResponse<LiveClassQuiz>> res = service.createQuiz(
                institutionId, teacherUserId, classId, Map.of("title", "Quiz", "questions", List.of()));

        assertEquals(400, res.getStatusCode().value());
        verify(quizRepository, never()).save(any());
    }

    @Test
    void createQuiz_wrongTeacher_rejected403() {
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacherWith(UUID.randomUUID(), teacherUserId)));

        ResponseEntity<ApiResponse<LiveClassQuiz>> res = service.createQuiz(
                institutionId, teacherUserId, classId,
                Map.of("title", "Quiz", "questions", List.of(Map.of(
                        "questionText", "Q1", "options", "A,B", "correctAnswer", "A"))));

        assertEquals(403, res.getStatusCode().value());
        verify(quizRepository, never()).save(any());
    }

    @Test
    void createQuiz_happyPath_persistsAndBroadcastsQuizStarted_withoutAnswerKey() {
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        asClassTeacher();
        when(quizRepository.save(any(LiveClassQuiz.class))).thenAnswer(inv -> {
            LiveClassQuiz q = inv.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        ResponseEntity<ApiResponse<LiveClassQuiz>> res = service.createQuiz(
                institutionId, teacherUserId, classId,
                Map.of("title", " capitals ", "questions", List.of(Map.of(
                        "questionText", "Tanzania capital?", "options", "Dar es Salaam,Dodoma", "correctAnswer", "Dodoma"))));

        assertEquals(201, res.getStatusCode().value());
        assertEquals("capitals", res.getBody().getData().getTitle());
        assertEquals("ACTIVE", res.getBody().getData().getStatus());

        ArgumentCaptor<LiveClassQuizQuestion> qCap = ArgumentCaptor.forClass(LiveClassQuizQuestion.class);
        verify(quizQuestionRepository).save(qCap.capture());
        assertEquals("Dodoma", qCap.getValue().getCorrectAnswer());

        verify(webSocketHandler).publishToClass(eq(classId), argThat(m ->
                "QUIZ_STARTED".equals(m.get("type"))
                        && m.get("quizId") != null
                        && !m.containsKey("correctAnswer")));
    }

    // ==================== READ ACCESS ====================

    @Test
    void getQuizzes_outsider_rejected403() {
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(teacherRepository.findByUserIdAndInstitutionId(any(), any())).thenReturn(Optional.empty());
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(false);

        ResponseEntity<ApiResponse<List<LiveClassQuiz>>> res = service.getQuizzes(studentId, classId);

        assertEquals(403, res.getStatusCode().value());
        verify(quizRepository, never()).findByLiveClassIdAndIsDeletedFalse(any());
    }

    @Test
    void getQuizzes_participant_ok() {
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(teacherRepository.findByUserIdAndInstitutionId(any(), any())).thenReturn(Optional.empty());
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(quizRepository.findByLiveClassIdAndIsDeletedFalse(classId)).thenReturn(List.of());

        ResponseEntity<ApiResponse<List<LiveClassQuiz>>> res = service.getQuizzes(studentId, classId);

        assertEquals(200, res.getStatusCode().value());
    }

    // ==================== QUESTION SANITIZATION ====================

    @Test
    void getQuizQuestions_student_neverSeesCorrectAnswer() {
        UUID quizId = UUID.randomUUID();
        LiveClassQuiz quiz = LiveClassQuiz.builder().liveClassId(classId).teacherId(teacherUserId)
                .title("Q").status("ACTIVE").build();
        quiz.setId(quizId);
        LiveClassQuizQuestion question = LiveClassQuizQuestion.builder()
                .quizId(quizId).questionText("Capital?").options("[\"Dodoma\",\"Dar\"]")
                .correctAnswer("Dodoma").displayOrder(0).build();
        question.setId(UUID.randomUUID());

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(teacherRepository.findByUserIdAndInstitutionId(any(), any())).thenReturn(Optional.empty());
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(quizQuestionRepository.findByQuizIdAndIsDeletedFalseOrderByDisplayOrderAsc(quizId))
                .thenReturn(List.of(question));
        when(quizResponseRepository.findByUserIdAndQuizIdAndIsDeletedFalse(studentId, quizId))
                .thenReturn(List.of());

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> res =
                service.getQuizQuestions(studentId, quizId, false);

        assertEquals(200, res.getStatusCode().value());
        Map<String, Object> m = res.getBody().getData().get(0);
        assertFalse(m.containsKey("correctAnswer"), "correct answer leaked to student");
        assertEquals("Capital?", m.get("questionText"));
        assertTrue(String.valueOf(m.get("options")).contains("Dodoma"));
    }

    @Test
    void getQuizQuestions_teacher_seesCorrectAnswer() {
        UUID quizId = UUID.randomUUID();
        LiveClassQuiz quiz = LiveClassQuiz.builder().liveClassId(classId).teacherId(teacherProfileId)
                .title("Q").status("ACTIVE").build();
        quiz.setId(quizId);
        LiveClassQuizQuestion question = LiveClassQuizQuestion.builder()
                .quizId(quizId).questionText("Capital?").options("[\"Dodoma\"]")
                .correctAnswer("Dodoma").displayOrder(0).build();
        question.setId(UUID.randomUUID());

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        asClassTeacher();
        when(quizQuestionRepository.findByQuizIdAndIsDeletedFalseOrderByDisplayOrderAsc(quizId))
                .thenReturn(List.of(question));

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> res =
                service.getQuizQuestions(teacherUserId, quizId, true);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("Dodoma", res.getBody().getData().get(0).get("correctAnswer"));
    }

    // ==================== SUBMIT RESPONSES ====================

    private LiveClassQuiz activeQuiz() {
        UUID quizId = UUID.randomUUID();
        LiveClassQuiz quiz = LiveClassQuiz.builder().liveClassId(classId).teacherId(teacherProfileId)
                .title("Quiz").status("ACTIVE").build();
        quiz.setId(quizId);
        return quiz;
    }

    @Test
    void submitResponse_notParticipant_rejected403() {
        LiveClassQuiz quiz = activeQuiz();
        when(quizRepository.findById(quiz.getId())).thenReturn(Optional.of(quiz));
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(false);

        ResponseEntity<ApiResponse<Map<String, Object>>> res = service.submitQuizResponse(
                studentId, quiz.getId(), List.of(Map.of("questionId", UUID.randomUUID().toString(), "answer", "A")));

        assertEquals(403, res.getStatusCode().value());
        verify(quizResponseRepository, never()).save(any());
    }

    @Test
    void submitResponse_closedQuiz_rejected409() {
        LiveClassQuiz quiz = activeQuiz();
        quiz.setStatus("CLOSED");
        when(quizRepository.findById(quiz.getId())).thenReturn(Optional.of(quiz));
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);

        ResponseEntity<ApiResponse<Map<String, Object>>> res = service.submitQuizResponse(
                studentId, quiz.getId(), List.of(Map.of("questionId", UUID.randomUUID().toString(), "answer", "A")));

        assertEquals(409, res.getStatusCode().value());
        verify(quizResponseRepository, never()).save(any());
    }

    @Test
    void submitResponse_duplicateSubmission_rejected409() {
        LiveClassQuiz quiz = activeQuiz();
        when(quizRepository.findById(quiz.getId())).thenReturn(Optional.of(quiz));
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(quizResponseRepository.findByUserIdAndQuizIdAndIsDeletedFalse(studentId, quiz.getId()))
                .thenReturn(List.of(LiveClassQuizResponse.builder().build()));

        ResponseEntity<ApiResponse<Map<String, Object>>> res = service.submitQuizResponse(
                studentId, quiz.getId(), List.of(Map.of("questionId", UUID.randomUUID().toString(), "answer", "A")));

        assertEquals(409, res.getStatusCode().value());
        verify(quizResponseRepository, never()).save(any());
    }

    @Test
    void submitResponse_questionFromAnotherQuiz_rejected400() {
        LiveClassQuiz quiz = activeQuiz();
        UUID foreignQuestionId = UUID.randomUUID();
        LiveClassQuizQuestion foreign = LiveClassQuizQuestion.builder()
                .quizId(UUID.randomUUID()).questionText("foreign").correctAnswer("x").build();
        foreign.setId(foreignQuestionId);

        when(quizRepository.findById(quiz.getId())).thenReturn(Optional.of(quiz));
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(quizResponseRepository.findByUserIdAndQuizIdAndIsDeletedFalse(studentId, quiz.getId()))
                .thenReturn(List.of());
        when(quizQuestionRepository.findById(foreignQuestionId)).thenReturn(Optional.of(foreign));

        ResponseEntity<ApiResponse<Map<String, Object>>> res = service.submitQuizResponse(
                studentId, quiz.getId(), List.of(Map.of("questionId", foreignQuestionId.toString(), "answer", "A")));

        assertEquals(400, res.getStatusCode().value());
        verify(quizResponseRepository, never()).save(any());
    }

    @Test
    void submitResponse_scoresServerSide_andBroadcastsAggregate() {
        LiveClassQuiz quiz = activeQuiz();
        UUID questionId = UUID.randomUUID();
        LiveClassQuizQuestion question = LiveClassQuizQuestion.builder()
                .quizId(quiz.getId()).questionText("Capital?").correctAnswer("Dodoma").build();
        question.setId(questionId);

        LiveClassParticipant learner = LiveClassParticipant.builder()
                .liveClassId(classId).userId(studentId).role("LEARNER").build();

        when(quizRepository.findById(quiz.getId())).thenReturn(Optional.of(quiz));
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(quizResponseRepository.findByUserIdAndQuizIdAndIsDeletedFalse(studentId, quiz.getId()))
                .thenReturn(List.of());
        when(quizQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(quizResponseRepository.findByQuizIdAndIsDeletedFalse(quiz.getId()))
                .thenReturn(List.of(LiveClassQuizResponse.builder()
                        .quizId(quiz.getId()).questionId(questionId).userId(studentId)
                        .answerText("  dodoma  ").isCorrect(true).build()));
        when(participantRepository.findByLiveClassIdAndIsDeletedFalse(classId)).thenReturn(List.of(learner));

        ResponseEntity<ApiResponse<Map<String, Object>>> res = service.submitQuizResponse(
                studentId, quiz.getId(), List.of(Map.of("questionId", questionId.toString(), "answer", "  dodoma  ")));

        assertEquals(200, res.getStatusCode().value());

        ArgumentCaptor<LiveClassQuizResponse> cap = ArgumentCaptor.forClass(LiveClassQuizResponse.class);
        verify(quizResponseRepository).save(cap.capture());
        assertTrue(cap.getValue().getIsCorrect(), "server-side scoring failed for normalized answer");

        verify(webSocketHandler).publishToClass(eq(quiz.getLiveClassId()), argThat(m ->
                "QUIZ_RESULT".equals(m.get("type"))
                        && Long.valueOf(1L).equals(asLong(m.get("answeredCount")))
                        && !m.containsKey("responses")));
    }

    // ==================== RESULTS / CLOSE ====================

    @Test
    void getQuizResults_nonOwnerTeacher_rejected403() {
        UUID quizId = UUID.randomUUID();
        LiveClassQuiz quiz = LiveClassQuiz.builder().liveClassId(classId).teacherId(teacherProfileId)
                .title("Q").status("ACTIVE").build();
        quiz.setId(quizId);
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacherWith(UUID.randomUUID(), teacherUserId)));

        ResponseEntity<ApiResponse<Map<String, Object>>> res = service.getQuizResults(teacherUserId, quizId);

        assertEquals(403, res.getStatusCode().value());
    }

    @Test
    void getQuizResults_countsAnsweredNotAnsweredCorrectIncorrect() {
        UUID quizId = UUID.randomUUID();
        LiveClassQuiz quiz = LiveClassQuiz.builder().liveClassId(classId).teacherId(teacherProfileId)
                .title("Q").status("ACTIVE").build();
        quiz.setId(quizId);

        UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID(), s3 = UUID.randomUUID();
        List<LiveClassParticipant> participants = List.of(
                LiveClassParticipant.builder().liveClassId(classId).userId(s1).role("LEARNER").build(),
                LiveClassParticipant.builder().liveClassId(classId).userId(s2).role("LEARNER").build(),
                LiveClassParticipant.builder().liveClassId(classId).userId(s3).role("LEARNER").build(),
                LiveClassParticipant.builder().liveClassId(classId).userId(teacherUserId).role("TEACHER").build());

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        asClassTeacher();
        when(quizResponseRepository.findByQuizIdAndIsDeletedFalse(quizId)).thenReturn(List.of(
                LiveClassQuizResponse.builder().quizId(quizId).userId(s1).isCorrect(true).build(),
                LiveClassQuizResponse.builder().quizId(quizId).userId(s1).isCorrect(false).build(),
                LiveClassQuizResponse.builder().quizId(quizId).userId(s2).isCorrect(false).build()));
        when(participantRepository.findByLiveClassIdAndIsDeletedFalse(classId)).thenReturn(participants);

        ResponseEntity<ApiResponse<Map<String, Object>>> res = service.getQuizResults(teacherUserId, quizId);

        assertEquals(200, res.getStatusCode().value());
        Map<String, Object> d = res.getBody().getData();
        assertEquals(3, ((Number) d.get("totalResponses")).intValue());
        assertEquals(1, ((Number) d.get("totalCorrect")).intValue());
        assertEquals(2, ((Number) d.get("totalIncorrect")).intValue());
        assertEquals(3, ((Number) d.get("participantCount")).intValue());
        assertEquals(2, ((Number) d.get("answeredCount")).intValue());
        assertEquals(1, ((Number) d.get("notAnsweredCount")).intValue());
    }

    @Test
    void closeQuiz_owner_setsClosedAndBroadcasts() {
        UUID quizId = UUID.randomUUID();
        LiveClassQuiz quiz = LiveClassQuiz.builder().liveClassId(classId).teacherId(teacherProfileId)
                .title("Quiz").status("ACTIVE").build();
        quiz.setId(quizId);
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        asClassTeacher();

        ResponseEntity<ApiResponse<LiveClassQuiz>> res = service.closeQuiz(teacherUserId, quizId);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("CLOSED", quiz.getStatus());
        verify(quizRepository).save(quiz);
        verify(webSocketHandler).publishToClass(eq(classId), argThat(m ->
                "QUIZ_CLOSED".equals(m.get("type")) && quizId.toString().equals(m.get("quizId"))));
    }

    @Test
    void myResponses_scoreHiddenUntilClosed() {
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        LiveClassQuiz quiz = LiveClassQuiz.builder().liveClassId(classId).teacherId(teacherProfileId)
                .title("Q").status("ACTIVE").build();
        quiz.setId(quizId);

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(teacherRepository.findByUserIdAndInstitutionId(any(), any())).thenReturn(Optional.empty());
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(quizResponseRepository.findByUserIdAndQuizIdAndIsDeletedFalse(studentId, quizId))
                .thenReturn(List.of(LiveClassQuizResponse.builder()
                        .quizId(quizId).questionId(questionId).userId(studentId)
                        .answerText("Dodoma").isCorrect(true).build()));

        ResponseEntity<ApiResponse<Map<String, Object>>> openRes = service.getMyQuizResponses(studentId, quizId);
        assertEquals(200, openRes.getStatusCode().value());
        assertTrue((Boolean) openRes.getBody().getData().get("submitted"));
        assertFalse(((Map<?, ?>) ((List<?>) openRes.getBody().getData().get("responses")).get(0))
                .containsKey("isCorrect"), "score must not be exposed before evaluation");

        quiz.setStatus("CLOSED");
        ResponseEntity<ApiResponse<Map<String, Object>>> closedRes = service.getMyQuizResponses(studentId, quizId);
        assertTrue((Boolean) ((Map<?, ?>) ((List<?>) closedRes.getBody().getData().get("responses")).get(0))
                .get("isCorrect"));
    }

    // ==================== POLLS ====================

    private LiveClassPoll activePoll() {
        UUID pollId = UUID.randomUUID();
        LiveClassPoll poll = LiveClassPoll.builder().liveClassId(classId).teacherId(teacherProfileId)
                .question("Like it?").options("[\"Yes\",\"No\"]").status("ACTIVE").build();
        poll.setId(pollId);
        return poll;
    }

    @Test
    void createPoll_tooFewOptions_rejected400() {
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        asClassTeacher();

        ResponseEntity<ApiResponse<LiveClassPoll>> res = service.createPoll(
                institutionId, teacherUserId, classId, Map.of("question", "Q?", "options", "OnlyOne"));

        assertEquals(400, res.getStatusCode().value());
        verify(pollRepository, never()).save(any());
    }

    @Test
    void votePoll_notParticipant_rejected403() {
        LiveClassPoll poll = activePoll();
        when(pollRepository.findById(poll.getId())).thenReturn(Optional.of(poll));
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(false);

        ResponseEntity<ApiResponse<Map<String, Object>>> res =
                service.votePoll(studentId, poll.getId(), Map.of("optionIndex", 0));

        assertEquals(403, res.getStatusCode().value());
        verify(pollVoteRepository, never()).save(any());
    }

    @Test
    void votePoll_indexOutOfRange_rejected400() {
        LiveClassPoll poll = activePoll();
        when(pollRepository.findById(poll.getId())).thenReturn(Optional.of(poll));
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);

        ResponseEntity<ApiResponse<Map<String, Object>>> res =
                service.votePoll(studentId, poll.getId(), Map.of("optionIndex", 7));

        assertEquals(400, res.getStatusCode().value());
        verify(pollVoteRepository, never()).save(any());
        verify(webSocketHandler, never()).publishToClass(any(), any());
    }

    @Test
    void votePoll_changesExistingVoteInPlace_withoutDuplicateRow() {
        LiveClassPoll poll = activePoll();
        LiveClassPollVote existing = LiveClassPollVote.builder()
                .pollId(poll.getId()).userId(studentId).optionIndex(0).build();
        when(pollRepository.findById(poll.getId())).thenReturn(Optional.of(poll));
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(pollVoteRepository.findByPollIdAndIsDeletedFalse(poll.getId()))
                .thenReturn(new ArrayList<>(List.of(existing)));

        ResponseEntity<ApiResponse<Map<String, Object>>> res =
                service.votePoll(studentId, poll.getId(), Map.of("optionIndex", 1));

        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, existing.getOptionIndex(), "vote should be updated in place");
        verify(pollVoteRepository, times(1)).save(any()); // only the update, no second row
        verify(webSocketHandler).publishToClass(eq(classId), argThat(m ->
                "POLL_RESULT".equals(m.get("type")) && !m.containsKey("userIds")));
    }

    @Test
    void closePoll_nonOwner_rejected403() {
        LiveClassPoll poll = activePoll();
        when(pollRepository.findById(poll.getId())).thenReturn(Optional.of(poll));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacherWith(UUID.randomUUID(), teacherUserId)));

        ResponseEntity<ApiResponse<String>> res = service.closePoll(teacherUserId, poll.getId());

        assertEquals(403, res.getStatusCode().value());
        assertEquals("ACTIVE", poll.getStatus());
        verify(pollRepository, never()).save(any());
    }

    @Test
    void pollResults_useRealOptionCount_notHardcodedTwenty() {
        LiveClassPoll poll = activePoll();
        when(pollRepository.findById(poll.getId())).thenReturn(Optional.of(poll));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(teacherRepository.findByUserIdAndInstitutionId(any(), any())).thenReturn(Optional.empty());
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(pollVoteRepository.findByPollIdAndIsDeletedFalse(poll.getId())).thenReturn(List.of(
                LiveClassPollVote.builder().pollId(poll.getId()).userId(UUID.randomUUID()).optionIndex(0).build(),
                LiveClassPollVote.builder().pollId(poll.getId()).userId(UUID.randomUUID()).optionIndex(0).build(),
                LiveClassPollVote.builder().pollId(poll.getId()).userId(UUID.randomUUID()).optionIndex(1).build()));

        ResponseEntity<ApiResponse<Map<String, Object>>> res = service.getPollResults(studentId, poll.getId());

        assertEquals(200, res.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Integer> counts = (Map<String, Integer>) res.getBody().getData().get("results");
        assertEquals(2, counts.size(), "results must have one key per real option, not 20");
        assertEquals(2, counts.get("0"));
        assertEquals(1, counts.get("1"));
        assertEquals(3, res.getBody().getData().get("totalVotes"));
    }

    // ==================== BREAKOUT ROOMS ====================

    private LiveClassBreakoutRoom room(UUID id, String status, int max) {
        LiveClassBreakoutRoom r = LiveClassBreakoutRoom.builder()
                .liveClassId(classId).name("Group A").maxParticipants(max).status(status).build();
        r.setId(id);
        return r;
    }

    @Test
    void createBreakoutRoom_nonOwner_rejected403() {
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacherWith(UUID.randomUUID(), teacherUserId)));

        ResponseEntity<ApiResponse<LiveClassBreakoutRoom>> res = service.createBreakoutRoom(
                institutionId, teacherUserId, classId, Map.of("name", "Room 1"));

        assertEquals(403, res.getStatusCode().value());
        verify(breakoutRoomRepository, never()).save(any());
    }

    @Test
    void assign_targetNotParticipant_rejected400() {
        UUID roomId = UUID.randomUUID();
        LiveClassBreakoutRoom r = room(roomId, "ACTIVE", 10);
        when(breakoutRoomRepository.findById(roomId)).thenReturn(Optional.of(r));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        asClassTeacher();
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(false);

        ResponseEntity<ApiResponse<String>> res = service.assignToBreakoutRoom(
                teacherUserId, roomId, Map.of("userId", studentId.toString()));

        assertEquals(400, res.getStatusCode().value());
        verify(breakoutAssignmentRepository, never()).save(any());
    }

    @Test
    void assign_roomFull_rejected409() {
        UUID roomId = UUID.randomUUID();
        LiveClassBreakoutRoom r = room(roomId, "ACTIVE", 1);
        when(breakoutRoomRepository.findById(roomId)).thenReturn(Optional.of(r));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        asClassTeacher();
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(breakoutAssignmentRepository.findByLiveClassIdAndIsDeletedFalse(classId)).thenReturn(List.of());
        when(breakoutAssignmentRepository.findByBreakoutRoomIdAndIsDeletedFalse(roomId))
                .thenReturn(List.of(LiveClassBreakoutAssignment.builder()
                        .breakoutRoomId(roomId).userId(UUID.randomUUID()).build()));

        ResponseEntity<ApiResponse<String>> res = service.assignToBreakoutRoom(
                teacherUserId, roomId, Map.of("userId", studentId.toString()));

        assertEquals(409, res.getStatusCode().value());
        verify(breakoutAssignmentRepository, never()).save(any());
    }

    @Test
    void join_assignedElsewhere_rejected409() {
        UUID roomId = UUID.randomUUID();
        UUID otherRoomId = UUID.randomUUID();
        LiveClassBreakoutRoom r = room(roomId, "ACTIVE", 10);
        when(breakoutRoomRepository.findById(roomId)).thenReturn(Optional.of(r));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(breakoutAssignmentRepository.findByLiveClassIdAndIsDeletedFalse(classId))
                .thenReturn(List.of(LiveClassBreakoutAssignment.builder()
                        .breakoutRoomId(otherRoomId).userId(studentId).build()));

        ResponseEntity<ApiResponse<Map<String, Object>>> res = service.joinBreakoutRoom(studentId, roomId);

        assertEquals(409, res.getStatusCode().value());
        verify(breakoutAssignmentRepository, never()).save(any());
    }

    @Test
    void join_unassignedIntoOpenRoom_persistsAndReturnsBreakoutRoomInfo() {
        UUID roomId = UUID.randomUUID();
        LiveClassBreakoutRoom r = room(roomId, "ACTIVE", 10);
        when(breakoutRoomRepository.findById(roomId)).thenReturn(Optional.of(r));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(participantRepository.existsByLiveClassIdAndUserIdAndIsDeletedFalse(classId, studentId)).thenReturn(true);
        when(breakoutAssignmentRepository.findByLiveClassIdAndIsDeletedFalse(classId)).thenReturn(List.of());
        when(breakoutAssignmentRepository.findByBreakoutRoomIdAndIsDeletedFalse(roomId)).thenReturn(List.of());
        when(liveKitService.isAvailable()).thenReturn(false);

        ResponseEntity<ApiResponse<Map<String, Object>>> res = service.joinBreakoutRoom(studentId, roomId);

        assertEquals(200, res.getStatusCode().value());
        Map<String, Object> d = res.getBody().getData();
        assertEquals(roomId.toString(), d.get("roomId"));
        assertEquals("Group A", d.get("roomName"));
        assertEquals(Boolean.FALSE, d.get("liveKitAvailable"));
        verify(breakoutAssignmentRepository).save(any(LiveClassBreakoutAssignment.class));
        verify(webSocketHandler).publishToClass(eq(classId), argThat(m ->
                "BREAKOUT_ROOMS_UPDATED".equals(m.get("type")) && m.get("rooms") != null));
    }

    @Test
    void startBreakoutRoom_nonOwner_rejected403() {
        UUID roomId = UUID.randomUUID();
        LiveClassBreakoutRoom r = room(roomId, "WAITING", 10);
        when(breakoutRoomRepository.findById(roomId)).thenReturn(Optional.of(r));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacherWith(UUID.randomUUID(), teacherUserId)));

        ResponseEntity<ApiResponse<String>> res = service.startBreakoutRoom(teacherUserId, roomId);

        assertEquals(403, res.getStatusCode().value());
        assertEquals("WAITING", r.getStatus());
        verify(breakoutRoomRepository, never()).save(any());
    }

    @Test
    void startBreakoutRoom_owner_opensAndBroadcasts() {
        UUID roomId = UUID.randomUUID();
        LiveClassBreakoutRoom r = room(roomId, "WAITING", 10);
        when(breakoutRoomRepository.findById(roomId)).thenReturn(Optional.of(r));
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(lc));
        asClassTeacher();
        when(breakoutRoomRepository.findByLiveClassIdAndIsDeletedFalse(classId)).thenReturn(List.of(r));
        when(breakoutAssignmentRepository.findByLiveClassIdAndIsDeletedFalse(classId)).thenReturn(List.of());

        ResponseEntity<ApiResponse<String>> res = service.startBreakoutRoom(teacherUserId, roomId);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("ACTIVE", r.getStatus());
        verify(breakoutRoomRepository).save(r);
        verify(webSocketHandler).publishToClass(eq(classId), argThat(m ->
                "BREAKOUT_ROOMS_UPDATED".equals(m.get("type"))));
    }

    private static Long asLong(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }
}
