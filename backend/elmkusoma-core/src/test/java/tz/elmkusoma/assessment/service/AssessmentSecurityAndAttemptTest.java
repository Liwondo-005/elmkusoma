package tz.elmkusoma.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.assessment.domain.*;
import tz.elmkusoma.assessment.dto.request.SaveAnswerRequest;
import tz.elmkusoma.assessment.dto.request.SubmitAssessmentRequest;
import tz.elmkusoma.assessment.dto.response.AnswerResponse;
import tz.elmkusoma.assessment.dto.response.AssessmentResponse;
import tz.elmkusoma.assessment.dto.response.AssessmentResultResponse;
import tz.elmkusoma.assessment.dto.response.AttemptResponse;
import tz.elmkusoma.assessment.dto.response.QuestionResponse;
import tz.elmkusoma.assessment.repository.*;
import tz.elmkusoma.assessment.service.impl.AssessmentServiceImpl;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Answer-key sanitization, availability / max-attempt enforcement, users.id →
 * students.id resolution, ownership checks, autosave upsert and submit-time
 * grading (including the cross-question option anti-tampering rule).
 */
@ExtendWith(MockitoExtension.class)
class AssessmentSecurityAndAttemptTest {

    /** The single generic availability message used by every start-attempt rejection. */
    private static final String UNAVAILABLE = "This assessment is not available";

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private OptionRepository optionRepository;
    @Mock private AttemptRepository attemptRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private AssessmentResultRepository resultRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private ClassAccessGuard classAccessGuard;

    @InjectMocks
    private AssessmentServiceImpl service;

    private UUID assessmentId;
    private UUID institutionId;
    private UUID callerUserId;
    private UUID studentId;
    private UUID attemptId;

    @BeforeEach
    void setUp() {
        assessmentId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        callerUserId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        attemptId = UUID.randomUUID();
    }

    private Assessment assessment(boolean published, LocalDateTime startsAt, LocalDateTime endsAt) {
        return Assessment.builder()
                .id(assessmentId)
                .institutionId(institutionId)
                .subjectId(UUID.randomUUID())
                .classGroupId(UUID.randomUUID())
                .title("Algebra quiz")
                .totalMarks(50)
                .passMarks(30)
                .isPublished(published)
                .status(published ? "PUBLISHED" : "DRAFT")
                .startsAt(startsAt)
                .endsAt(endsAt)
                .isDeleted(false)
                .build();
    }

    private Student student() {
        return Student.builder()
                .id(studentId)
                .userId(callerUserId)
                .status(StudentStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    // ── Answer-key sanitization ───────────────────────────────────────────────

    @Test
    void getQuestions_stripsAnswerKeyForLearners() {
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, null, null)));
        when(questionRepository.findByAssessmentIdAndIsDeletedFalseOrderBySortOrder(assessmentId))
                .thenReturn(List.of(Question.builder()
                        .id(questionId).assessmentId(assessmentId)
                        .questionType(QuestionType.MCQ).questionText("2+2?")
                        .marks(4).sortOrder(0).isDeleted(false).build()));
        when(optionRepository.findByQuestionIdAndIsDeletedFalseOrderBySortOrder(questionId))
                .thenReturn(List.of(Option.builder()
                        .id(optionId).questionId(questionId)
                        .optionText("4").isCorrect(true).sortOrder(0).isDeleted(false).build()));

        List<QuestionResponse> questions = service.getQuestions(assessmentId, institutionId, false);

        assertEquals(1, questions.size());
        assertNotNull(questions.get(0).getOptions());
        assertNull(questions.get(0).getOptions().get(0).getIsCorrect(),
                "learners must never receive the answer key");
        assertEquals("4", questions.get(0).getOptions().get(0).getOptionText());
        assertNotNull(questions.get(0).getOptions().get(0).getId());
    }

    @Test
    void getQuestions_keepsAnswerKeyForTeachers() {
        UUID questionId = UUID.randomUUID();
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, null, null)));
        when(questionRepository.findByAssessmentIdAndIsDeletedFalseOrderBySortOrder(assessmentId))
                .thenReturn(List.of(Question.builder()
                        .id(questionId).assessmentId(assessmentId)
                        .questionType(QuestionType.MCQ).questionText("2+2?")
                        .marks(4).sortOrder(0).isDeleted(false).build()));
        when(optionRepository.findByQuestionIdAndIsDeletedFalseOrderBySortOrder(questionId))
                .thenReturn(List.of(Option.builder()
                        .id(UUID.randomUUID()).questionId(questionId)
                        .optionText("4").isCorrect(true).sortOrder(0).isDeleted(false).build()));

        List<QuestionResponse> questions = service.getQuestions(assessmentId, institutionId, true);

        assertEquals(Boolean.TRUE, questions.get(0).getOptions().get(0).getIsCorrect());
    }

    @Test
    void getQuestions_unpublishedAssessmentHiddenFromLearners() {
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(false, null, null)));

        assertThrows(ResourceNotFoundException.class,
                () -> service.getQuestions(assessmentId, institutionId, false));
    }

    // ── Availability & attempt rules ──────────────────────────────────────────

    @Test
    void startAttempt_rejectsUnpublishedAssessment() {
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(false, null, null)));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());

        assertThrows(IllegalArgumentException.class,
                () -> service.startAttempt(assessmentId, callerUserId, institutionId));
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void startAttempt_rejectsBeforeWindowOpens() {
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, LocalDateTime.now().plusHours(2), null)));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startAttempt(assessmentId, callerUserId, institutionId));
        assertEquals(UNAVAILABLE, ex.getMessage());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void startAttempt_rejectsAfterWindowCloses() {
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, null, LocalDateTime.now().minusMinutes(1))));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startAttempt(assessmentId, callerUserId, institutionId));
        assertEquals(UNAVAILABLE, ex.getMessage());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void startAttempt_missingAssessmentIsIndistinguishableFromClosedWindow() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startAttempt(assessmentId, callerUserId, institutionId));
        assertEquals(UNAVAILABLE, ex.getMessage());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void startAttempt_crossInstitutionAssessmentIsIndistinguishableFromClosedWindow() {
        Assessment foreign = assessment(true, null, null);
        foreign.setInstitutionId(UUID.randomUUID());
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(foreign));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startAttempt(assessmentId, callerUserId, institutionId));
        assertEquals(UNAVAILABLE, ex.getMessage());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void startAttempt_enforcesMaxAttempts() {
        Assessment assessment = assessment(true, null, null);
        assessment.setMaxAttempts(1);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(attemptRepository.findByAssessmentIdAndStudentIdAndIsCompletedAndIsDeletedFalse(
                assessmentId, studentId, false)).thenReturn(Optional.empty());
        when(attemptRepository.countByAssessmentIdAndStudentIdAndIsCompletedTrueAndIsDeletedFalse(
                assessmentId, studentId)).thenReturn(1L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startAttempt(assessmentId, callerUserId, institutionId));
        assertTrue(ex.getMessage().contains("Attempt limit"));
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void startAttempt_requiresStudentProfile() {
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, null, null)));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(null);

        assertThrows(SecurityException.class,
                () -> service.startAttempt(assessmentId, callerUserId, institutionId));
    }

    @Test
    void startAttempt_persistsResolvedStudentProfileId() {
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, null, null)));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(attemptRepository.findByAssessmentIdAndStudentIdAndIsCompletedAndIsDeletedFalse(
                assessmentId, studentId, false)).thenReturn(Optional.empty());
        when(attemptRepository.save(any(Attempt.class))).thenAnswer(inv -> inv.getArgument(0));

        AttemptResponse response = service.startAttempt(assessmentId, callerUserId, institutionId);

        ArgumentCaptor<Attempt> captor = ArgumentCaptor.forClass(Attempt.class);
        verify(attemptRepository).save(captor.capture());
        // attempts.student_id has a hard FK to students(id) — must be the profile id.
        assertEquals(studentId, captor.getValue().getStudentId());
        assertEquals(assessmentId, captor.getValue().getAssessmentId());
        assertFalse(captor.getValue().getIsCompleted());
        assertNotNull(captor.getValue().getStartedAt());
        assertEquals(studentId, response.getStudentId());
        verify(auditService, atLeastOnce()).recordAuditLog(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ── Autosave (resume) ─────────────────────────────────────────────────────

    @Test
    void saveAnswer_rejectsCompletedAttempt() {
        Attempt completed = Attempt.builder()
                .id(attemptId).assessmentId(assessmentId).studentId(studentId)
                .institutionId(institutionId).isCompleted(true)
                .startedAt(LocalDateTime.now().minusHours(1)).isDeleted(false).build();
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(completed));

        assertThrows(IllegalStateException.class,
                () -> service.saveAnswer(attemptId, callerUserId, new SaveAnswerRequest()));
        verify(answerRepository, never()).save(any());
    }

    @Test
    void saveAnswer_rejectsForeignAttempt() {
        Attempt foreign = Attempt.builder()
                .id(attemptId).assessmentId(assessmentId).studentId(UUID.randomUUID())
                .institutionId(institutionId).isCompleted(false)
                .startedAt(LocalDateTime.now()).isDeleted(false).build();
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(foreign));

        assertThrows(IllegalArgumentException.class,
                () -> service.saveAnswer(attemptId, callerUserId, new SaveAnswerRequest()));
    }

    @Test
    void saveAnswer_upsertsExistingAnswerInsteadOfDuplicating() {
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        Attempt attempt = Attempt.builder()
                .id(attemptId).assessmentId(assessmentId).studentId(studentId)
                .institutionId(institutionId).isCompleted(false)
                .startedAt(LocalDateTime.now()).isDeleted(false).build();
        Answer existing = Answer.builder()
                .id(UUID.randomUUID()).attemptId(attemptId).questionId(questionId)
                .institutionId(institutionId).isDeleted(false).build();
        existing.setSelectedOptionId(optionId);
        existing.setTextAnswer("old");

        SaveAnswerRequest request = new SaveAnswerRequest();
        request.setQuestionId(questionId);
        request.setSelectedOptionId(optionId);
        request.setTextAnswer("new answer");

        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, null, null)));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(Question.builder()
                .id(questionId).assessmentId(assessmentId)
                .questionType(QuestionType.MCQ).questionText("q")
                .marks(4).sortOrder(0).isDeleted(false).build()));
        when(answerRepository.findByAttemptIdAndQuestionIdAndIsDeletedFalse(attemptId, questionId))
                .thenReturn(Optional.of(existing));
        when(answerRepository.save(any(Answer.class))).thenAnswer(inv -> inv.getArgument(0));

        AttemptResponse response = service.saveAnswer(attemptId, callerUserId, request);

        ArgumentCaptor<Answer> captor = ArgumentCaptor.forClass(Answer.class);
        verify(answerRepository).save(captor.capture());
        assertEquals(existing.getId(), captor.getValue().getId());
        assertEquals("new answer", captor.getValue().getTextAnswer());
        // Autosave never grades — grading happens once at submit.
        assertNull(captor.getValue().getIsCorrect());
        assertNull(captor.getValue().getMarksObtained());
        assertNotNull(response);
        assertFalse(response.getIsCompleted());
    }

    // ── Submit-time grading ───────────────────────────────────────────────────

    private Question mcq(UUID questionId, int marks) {
        return Question.builder()
                .id(questionId).assessmentId(assessmentId)
                .questionType(QuestionType.MCQ).questionText("q")
                .marks(marks).sortOrder(0).isDeleted(false).build();
    }

    private SubmitAssessmentRequest payload(UUID questionId, UUID optionId) {
        SubmitAssessmentRequest.AnswerSubmission answer = new SubmitAssessmentRequest.AnswerSubmission();
        answer.setQuestionId(questionId);
        answer.setSelectedOptionId(optionId);
        SubmitAssessmentRequest request = new SubmitAssessmentRequest();
        request.setAnswers(List.of(answer));
        return request;
    }

    @Test
    void submitAttempt_rejectsForeignAttempt() {
        Attempt foreign = Attempt.builder()
                .id(attemptId).assessmentId(assessmentId).studentId(UUID.randomUUID())
                .institutionId(institutionId).isCompleted(false)
                .startedAt(LocalDateTime.now()).isDeleted(false).build();
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(foreign));

        assertThrows(IllegalArgumentException.class,
                () -> service.submitAttempt(attemptId, callerUserId, payload(UUID.randomUUID(), null)));
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void submitAttempt_rejectsExpiredTimeLimit() {
        Assessment assessment = assessment(true, null, null);
        assessment.setTimeLimitMinutes(60);
        Attempt attempt = Attempt.builder()
                .id(attemptId).assessmentId(assessmentId).studentId(studentId)
                .institutionId(institutionId).isCompleted(false)
                .startedAt(LocalDateTime.now().minusHours(3)).isDeleted(false).build();
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.submitAttempt(attemptId, callerUserId, payload(UUID.randomUUID(), null)));
        assertTrue(ex.getMessage().toLowerCase().contains("time limit"));
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void submitAttempt_mergesAutosavedAnswersAndScoresOnce() {
        UUID question1 = UUID.randomUUID();
        UUID question2 = UUID.randomUUID();
        UUID correctOption = UUID.randomUUID();
        UUID wrongOption = UUID.randomUUID();

        Assessment assessment = assessment(true, null, null);
        assessment.setPassMarks(3);
        Attempt attempt = Attempt.builder()
                .id(attemptId).assessmentId(assessmentId).studentId(studentId)
                .institutionId(institutionId).isCompleted(false)
                .startedAt(LocalDateTime.now()).isDeleted(false).build();

        // Q2 was autosaved earlier; the payload carries Q1 only.
        Answer autosaved = Answer.builder()
                .id(UUID.randomUUID()).attemptId(attemptId).questionId(question2)
                .institutionId(institutionId).isDeleted(false).build();
        autosaved.setSelectedOptionId(wrongOption);

        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(answerRepository.findByAttemptIdAndIsDeletedFalse(attemptId))
                .thenReturn(List.of(autosaved));
        when(questionRepository.findById(question1)).thenReturn(Optional.of(mcq(question1, 4)));
        when(questionRepository.findById(question2)).thenReturn(Optional.of(mcq(question2, 6)));
        when(optionRepository.findById(correctOption)).thenReturn(Optional.of(Option.builder()
                .id(correctOption).questionId(question1).optionText("4")
                .isCorrect(true).sortOrder(0).isDeleted(false).build()));
        when(optionRepository.findById(wrongOption)).thenReturn(Optional.of(Option.builder()
                .id(wrongOption).questionId(question2).optionText("5")
                .isCorrect(false).sortOrder(0).isDeleted(false).build()));
        when(answerRepository.save(any(Answer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resultRepository.findByAssessmentIdAndStudentIdAndIsDeletedFalse(assessmentId, studentId))
                .thenReturn(Optional.empty());
        when(resultRepository.save(any(AssessmentResult.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));

        AttemptResponse response = service.submitAttempt(attemptId, callerUserId,
                payload(question1, correctOption));

        // One row per question — the autosaved Q2 is not duplicated.
        ArgumentCaptor<Answer> answerCaptor = ArgumentCaptor.forClass(Answer.class);
        verify(answerRepository, times(2)).save(answerCaptor.capture());
        assertEquals(2, answerCaptor.getAllValues().size());
        assertTrue(answerCaptor.getAllValues().stream()
                .anyMatch(a -> question1.equals(a.getQuestionId())));
        assertTrue(answerCaptor.getAllValues().stream()
                .anyMatch(a -> question2.equals(a.getQuestionId())));

        ArgumentCaptor<AssessmentResult> resultCaptor =
                ArgumentCaptor.forClass(AssessmentResult.class);
        verify(resultRepository).save(resultCaptor.capture());
        // Q1 correct (4 marks) + Q2 wrong (0) = 4, pass mark 3 → passed.
        assertEquals(4, resultCaptor.getValue().getTotalScore());
        assertTrue(resultCaptor.getValue().getIsPassed());
        assertEquals(studentId, resultCaptor.getValue().getStudentId());

        assertTrue(attempt.getIsCompleted());
        assertNotNull(attempt.getSubmittedAt());
        assertNotNull(response.getAnswers());
        assertEquals(2, response.getAnswers().size());

        verify(notificationService).notifyUser(
                eq(callerUserId), anyString(), anyString(),
                eq("ASSESSMENT_GRADED"), eq("assessment"), eq(assessmentId));
    }

    @Test
    void submitAttempt_rejectsOptionBelongingToAnotherQuestion() {
        UUID questionId = UUID.randomUUID();
        UUID foreignOption = UUID.randomUUID();

        Attempt attempt = Attempt.builder()
                .id(attemptId).assessmentId(assessmentId).studentId(studentId)
                .institutionId(institutionId).isCompleted(false)
                .startedAt(LocalDateTime.now()).isDeleted(false).build();

        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, null, null)));
        when(answerRepository.findByAttemptIdAndIsDeletedFalse(attemptId)).thenReturn(List.of());
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(mcq(questionId, 10)));
        // The "correct" option actually belongs to a different question — tampering attempt.
        when(optionRepository.findById(foreignOption)).thenReturn(Optional.of(Option.builder()
                .id(foreignOption).questionId(UUID.randomUUID()).optionText("a")
                .isCorrect(true).sortOrder(0).isDeleted(false).build()));
        when(answerRepository.save(any(Answer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resultRepository.findByAssessmentIdAndStudentIdAndIsDeletedFalse(assessmentId, studentId))
                .thenReturn(Optional.empty());
        when(resultRepository.save(any(AssessmentResult.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));

        service.submitAttempt(attemptId, callerUserId, payload(questionId, foreignOption));

        ArgumentCaptor<AssessmentResult> resultCaptor =
                ArgumentCaptor.forClass(AssessmentResult.class);
        verify(resultRepository).save(resultCaptor.capture());
        assertEquals(0, resultCaptor.getValue().getTotalScore(),
                "an option from another question must not earn marks");
        assertFalse(resultCaptor.getValue().getIsPassed());
    }

    // ── Essay grading ownership ──────────────────────────────────────────────

    private Answer essayAnswer(UUID questionId) {
        return Answer.builder()
                .id(UUID.randomUUID()).attemptId(attemptId).questionId(questionId)
                .institutionId(institutionId).isDeleted(false).build();
    }

    private Question essayQuestion(UUID questionId) {
        return Question.builder()
                .id(questionId).assessmentId(assessmentId)
                .questionType(QuestionType.ESSAY).questionText("Explain photosynthesis")
                .marks(10).sortOrder(0).isDeleted(false).build();
    }

    private Attempt completedAttempt() {
        return Attempt.builder()
                .id(attemptId).assessmentId(assessmentId).studentId(studentId)
                .institutionId(institutionId).isCompleted(true)
                .startedAt(LocalDateTime.now().minusHours(2)).isDeleted(false).build();
    }

    @Test
    void gradeEssay_rejectsAnswerFromAnotherInstitution() {
        UUID questionId = UUID.randomUUID();
        Answer answer = essayAnswer(questionId);
        Assessment foreign = assessment(true, null, null);
        foreign.setInstitutionId(UUID.randomUUID());

        when(answerRepository.findById(answer.getId())).thenReturn(Optional.of(answer));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(essayQuestion(questionId)));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(completedAttempt()));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(foreign));

        assertThrows(ResourceNotFoundException.class,
                () -> service.gradeEssay(answer.getId(), 5, "good effort", callerUserId, institutionId));
        verify(answerRepository, never()).save(any());
    }

    @Test
    void gradeEssay_requiresInstitutionContext() {
        UUID questionId = UUID.randomUUID();
        Answer answer = essayAnswer(questionId);

        when(answerRepository.findById(answer.getId())).thenReturn(Optional.of(answer));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(essayQuestion(questionId)));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(completedAttempt()));
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, null, null)));

        assertThrows(ResourceNotFoundException.class,
                () -> service.gradeEssay(answer.getId(), 5, "good effort", callerUserId, null));
        verify(answerRepository, never()).save(any());
    }

    @Test
    void gradeEssay_gradesOwnInstitutionAnswerAndRecalculates() {
        UUID questionId = UUID.randomUUID();
        Answer answer = essayAnswer(questionId);
        AssessmentResult existingResult = AssessmentResult.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .assessmentId(assessmentId)
                .studentId(studentId)
                .attemptId(attemptId)
                .totalScore(0)
                .isPassed(false)
                .isDeleted(false)
                .build();

        when(answerRepository.findById(answer.getId())).thenReturn(Optional.of(answer));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(essayQuestion(questionId)));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(completedAttempt()));
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, null, null)));
        when(answerRepository.save(any(Answer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(answerRepository.findByAttemptIdAndIsDeletedFalse(attemptId)).thenReturn(List.of(answer));
        when(resultRepository.findByAssessmentIdAndStudentIdAndIsDeletedFalse(assessmentId, studentId))
                .thenReturn(Optional.of(existingResult));
        when(resultRepository.save(any(AssessmentResult.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));

        AnswerResponse graded =
                service.gradeEssay(answer.getId(), 7, "solid work", callerUserId, institutionId);

        assertEquals(7, graded.getMarksObtained());
        assertEquals(callerUserId, graded.getGradedBy());
        assertNotNull(graded.getGradedAt());
        assertEquals(7, existingResult.getTotalScore(),
                "manual grade must be rolled into the attempt's result");
        verify(auditService, atLeastOnce()).recordAuditLog(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ── Result read + class scoping ───────────────────────────────────────────

    @Test
    void getResult_learnerCannotReadAnotherStudentsResult() {
        UUID otherStudentId = UUID.randomUUID();
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());

        assertThrows(SecurityException.class,
                () -> service.getResult(assessmentId, otherStudentId, callerUserId, "STUDENT", institutionId));
        verify(resultRepository, never()).findByAssessmentIdAndStudentIdAndIsDeletedFalse(any(), any());
    }

    @Test
    void getResult_parentCannotReadAnotherStudentsResult() {
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(null);

        assertThrows(SecurityException.class,
                () -> service.getResult(assessmentId, studentId, callerUserId, "PARENT", institutionId));
        verify(resultRepository, never()).findByAssessmentIdAndStudentIdAndIsDeletedFalse(any(), any());
    }

    @Test
    void getResult_learnerReadsOwnResultWithinInstitution() {
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment(true, null, null)));
        when(resultRepository.findByAssessmentIdAndStudentIdAndIsDeletedFalse(assessmentId, studentId))
                .thenReturn(Optional.of(AssessmentResult.builder()
                        .id(UUID.randomUUID())
                        .institutionId(institutionId)
                        .assessmentId(assessmentId)
                        .studentId(studentId)
                        .attemptId(attemptId)
                        .totalScore(40)
                        .isPassed(true)
                        .isDeleted(false)
                        .build()));

        AssessmentResultResponse result =
                service.getResult(assessmentId, studentId, callerUserId, "STUDENT", institutionId);

        assertEquals(40, result.getTotalScore());
        assertEquals(studentId, result.getStudentId());
    }

    @Test
    void getResult_teacherCannotReadAnotherInstitutionsResult() {
        Assessment foreign = assessment(true, null, null);
        foreign.setInstitutionId(UUID.randomUUID());
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(foreign));

        assertThrows(ResourceNotFoundException.class,
                () -> service.getResult(assessmentId, studentId, callerUserId, "TEACHER", institutionId));
        verify(resultRepository, never()).findByAssessmentIdAndStudentIdAndIsDeletedFalse(any(), any());
    }

    @Test
    void getAssessmentsByClass_learnersOnlyReceivePublished() {
        UUID classGroupId = UUID.randomUUID();
        Assessment published = assessment(true, null, null);
        Assessment draft = assessment(false, null, null);
        when(assessmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of(published, draft));

        List<AssessmentResponse> visible = service.getAssessmentsByClass(
                classGroupId, institutionId, "learner@test.com", "STUDENT");

        assertEquals(1, visible.size());
        assertEquals("Algebra quiz", visible.get(0).getTitle());
        verify(classAccessGuard).assertLearnerCanAccessClass("learner@test.com", classGroupId);
    }

    @Test
    void getAssessmentsByClass_filtersOtherInstitutions() {
        UUID classGroupId = UUID.randomUUID();
        Assessment foreign = assessment(true, null, null);
        foreign.setInstitutionId(UUID.randomUUID());
        when(assessmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of(foreign));

        List<AssessmentResponse> visible = service.getAssessmentsByClass(
                classGroupId, institutionId, "teacher@test.com", "TEACHER");

        assertTrue(visible.isEmpty());
        verify(classAccessGuard, never()).assertLearnerCanAccessClass(any(), any());
    }
}
