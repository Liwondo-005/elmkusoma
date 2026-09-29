package tz.elmkusoma.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.assessment.domain.*;
import tz.elmkusoma.assessment.dto.request.AssessmentRequest;
import tz.elmkusoma.assessment.dto.request.QuestionRequest;
import tz.elmkusoma.assessment.dto.request.SaveAnswerRequest;
import tz.elmkusoma.assessment.dto.request.SubmitAssessmentRequest;
import tz.elmkusoma.assessment.dto.response.*;
import tz.elmkusoma.assessment.repository.*;
import tz.elmkusoma.assessment.service.AssessmentService;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentServiceImpl implements AssessmentService {

    /** Grace minutes past ends_at / time limit during which a submit is still accepted. */
    private static final int SUBMIT_GRACE_MINUTES = 5;

    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final AssessmentResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ClassAccessGuard classAccessGuard;

    // ── Assessments ───────────────────────────────────────────────────────────

    @Override
    public AssessmentResponse createAssessment(UUID institutionId, AssessmentRequest request,
                                               String userEmail, String userRole) {
        boolean published = request.getIsPublished() != null && request.getIsPublished();
        Assessment assessment = Assessment.builder()
                .institutionId(institutionId)
                .subjectId(request.getSubjectId())
                .classGroupId(request.getClassGroupId())
                .title(request.getTitle())
                .description(request.getDescription())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .totalMarks(request.getTotalMarks())
                .passMarks(request.getPassMarks())
                .isPublished(published)
                .status(published ? "PUBLISHED" : "DRAFT")
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .lessonId(request.getLessonId())
                .maxAttempts(request.getMaxAttempts())
                .build();

        Assessment saved = assessmentRepository.save(assessment);

        if (published) {
            notifyAssessmentPublished(saved, resolveActorUserId(userEmail));
        }
        auditSafely(institutionId, userEmail, userRole, "Assessment", saved.getId(), saved.getTitle(),
                AuditLog.AuditAction.CREATE, null,
                Map.of("title", saved.getTitle(),
                        "classGroupId", String.valueOf(saved.getClassGroupId()),
                        "published", String.valueOf(published)));
        return toAssessmentResponse(saved);
    }

    @Override
    public AssessmentResponse updateAssessment(UUID assessmentId, AssessmentRequest request,
                                               UUID institutionId, String userEmail, String userRole) {
        Assessment assessment = loadScoped(assessmentId, institutionId);
        boolean wasPublished = Boolean.TRUE.equals(assessment.getIsPublished());

        Map<String, Object> oldValues = Map.of(
                "title", String.valueOf(assessment.getTitle()),
                "isPublished", String.valueOf(wasPublished),
                "startsAt", String.valueOf(assessment.getStartsAt()),
                "endsAt", String.valueOf(assessment.getEndsAt()));

        if (request.getSubjectId() != null) assessment.setSubjectId(request.getSubjectId());
        if (request.getClassGroupId() != null) assessment.setClassGroupId(request.getClassGroupId());
        if (request.getTitle() != null) assessment.setTitle(request.getTitle());
        if (request.getDescription() != null) assessment.setDescription(request.getDescription());
        if (request.getTimeLimitMinutes() != null) assessment.setTimeLimitMinutes(request.getTimeLimitMinutes());
        if (request.getTotalMarks() != null) assessment.setTotalMarks(request.getTotalMarks());
        if (request.getPassMarks() != null) assessment.setPassMarks(request.getPassMarks());
        if (request.getStartsAt() != null) assessment.setStartsAt(request.getStartsAt());
        if (request.getEndsAt() != null) assessment.setEndsAt(request.getEndsAt());
        if (request.getLessonId() != null) assessment.setLessonId(request.getLessonId());
        if (request.getMaxAttempts() != null) assessment.setMaxAttempts(request.getMaxAttempts());
        if (request.getIsPublished() != null) {
            assessment.setIsPublished(request.getIsPublished());
            assessment.setStatus(request.getIsPublished() ? "PUBLISHED" : "DRAFT");
        }

        Assessment saved = assessmentRepository.save(assessment);

        if (!wasPublished && Boolean.TRUE.equals(saved.getIsPublished())) {
            notifyAssessmentPublished(saved, resolveActorUserId(userEmail));
        }
        auditSafely(institutionId, userEmail, userRole, "Assessment", saved.getId(), saved.getTitle(),
                AuditLog.AuditAction.UPDATE, oldValues,
                Map.of("title", saved.getTitle(),
                        "isPublished", String.valueOf(saved.getIsPublished())));
        return toAssessmentResponse(saved);
    }

    @Override
    public void deleteAssessment(UUID assessmentId, UUID institutionId, String userEmail, String userRole) {
        Assessment assessment = loadScoped(assessmentId, institutionId);
        assessment.setIsDeleted(true);
        assessmentRepository.save(assessment);
        auditSafely(institutionId, userEmail, userRole, "Assessment", assessmentId, assessment.getTitle(),
                AuditLog.AuditAction.DELETE, Map.of("title", assessment.getTitle()), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAssessmentsByClass(UUID classGroupId, UUID institutionId,
                                                          String userEmail, String userRole) {
        boolean isLearner = isLearnerRole(userRole);
        if (isLearner) {
            classAccessGuard.assertLearnerCanAccessClass(userEmail, classGroupId);
        }
        return assessmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)
                .stream()
                .filter(a -> institutionId == null || institutionId.equals(a.getInstitutionId()))
                .filter(a -> !isLearner || Boolean.TRUE.equals(a.getIsPublished()))
                .map(this::toAssessmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAssessmentsByClasses(List<UUID> classGroupIds, UUID institutionId) {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return List.of();
        }
        return assessmentRepository.findByClassGroupIdInAndIsDeletedFalse(classGroupIds)
                .stream()
                .filter(a -> institutionId == null || institutionId.equals(a.getInstitutionId()))
                .map(this::toAssessmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAssessmentsBySubject(UUID subjectId, UUID institutionId) {
        return assessmentRepository.findBySubjectIdAndIsDeletedFalse(subjectId)
                .stream()
                .filter(a -> institutionId == null || institutionId.equals(a.getInstitutionId()))
                .map(this::toAssessmentResponse)
                .toList();
    }

    // ── Questions ─────────────────────────────────────────────────────────────

    @Override
    public QuestionResponse addQuestion(UUID assessmentId, UUID institutionId, QuestionRequest request,
                                        String userEmail, String userRole) {
        Assessment assessment = loadScoped(assessmentId, institutionId);

        Question question = Question.builder()
                .institutionId(assessment.getInstitutionId())
                .assessmentId(assessmentId)
                .questionType(request.getQuestionType())
                .questionText(request.getQuestionText())
                .marks(request.getMarks())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        Question savedQuestion = questionRepository.save(question);

        List<OptionResponse> optionResponses = new ArrayList<>();
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            for (int i = 0; i < request.getOptions().size(); i++) {
                var optReq = request.getOptions().get(i);
                Option option = Option.builder()
                        .institutionId(assessment.getInstitutionId())
                        .questionId(savedQuestion.getId())
                        .optionText(optReq.getOptionText())
                        .isCorrect(optReq.getIsCorrect() != null ? optReq.getIsCorrect() : false)
                        .sortOrder(optReq.getSortOrder() != null ? optReq.getSortOrder() : i)
                        .build();
                Option savedOption = optionRepository.save(option);
                optionResponses.add(toOptionResponse(savedOption));
            }
        }

        auditSafely(assessment.getInstitutionId(), userEmail, userRole, "Question", savedQuestion.getId(),
                assessment.getTitle(), AuditLog.AuditAction.CREATE, null,
                Map.of("assessmentId", String.valueOf(assessmentId),
                        "questionType", savedQuestion.getQuestionType().name(),
                        "marks", String.valueOf(savedQuestion.getMarks())));

        QuestionResponse response = toQuestionResponse(savedQuestion);
        response.setOptions(optionResponses);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions(UUID assessmentId, UUID institutionId, boolean includeAnswerKey) {
        Assessment assessment = loadScoped(assessmentId, institutionId);
        if (!includeAnswerKey && !Boolean.TRUE.equals(assessment.getIsPublished())) {
            // Learners must never read questions of an unpublished assessment.
            throw new ResourceNotFoundException("Assessment not found with id: " + assessmentId);
        }

        List<Question> questions = questionRepository.findByAssessmentIdAndIsDeletedFalseOrderBySortOrder(assessmentId);
        return questions.stream().map(q -> {
            QuestionResponse response = toQuestionResponse(q);
            response.setOptions(
                    optionRepository.findByQuestionIdAndIsDeletedFalseOrderBySortOrder(q.getId())
                            .stream()
                            .map(opt -> {
                                OptionResponse optResponse = toOptionResponse(opt);
                                if (!includeAnswerKey) {
                                    // Strip the answer key for learners; marks/options stay readable.
                                    optResponse.setIsCorrect(null);
                                }
                                return optResponse;
                            })
                            .toList()
            );
            return response;
        }).toList();
    }

    @Override
    public void deleteQuestion(UUID assessmentId, UUID questionId, UUID institutionId) {
        Assessment assessment = loadScoped(assessmentId, institutionId);
        Question question = questionRepository.findById(questionId)
                .filter(q -> !q.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));
        if (!assessmentId.equals(question.getAssessmentId())) {
            throw new ResourceNotFoundException("Question", "id", questionId);
        }
        question.setIsDeleted(true);
        questionRepository.save(question);
        auditSafely(assessment.getInstitutionId(), null, null, "Question", questionId,
                assessment.getTitle(), AuditLog.AuditAction.DELETE,
                Map.of("questionText", String.valueOf(question.getQuestionText())), null);
    }

    // ── Attempts ──────────────────────────────────────────────────────────────

    @Override
    public AttemptResponse startAttempt(UUID assessmentId, UUID callerUserId, UUID institutionId) {
        Assessment assessment = loadScoped(assessmentId, institutionId);

        Student student = classAccessGuard.findStudentByUserId(callerUserId);
        if (student == null) {
            throw new SecurityException("A student profile is required to start this assessment");
        }

        if (!Boolean.TRUE.equals(assessment.getIsPublished())) {
            throw new IllegalArgumentException("This assessment is not available");
        }
        validateAvailability(assessment, LocalDateTime.now());

        boolean hasIncompleteAttempt = attemptRepository
                .findByAssessmentIdAndStudentIdAndIsCompletedAndIsDeletedFalse(
                        assessmentId, student.getId(), false)
                .isPresent();
        if (hasIncompleteAttempt) {
            throw new IllegalArgumentException("Student already has an incomplete attempt for this assessment");
        }

        if (assessment.getMaxAttempts() != null) {
            long completed = attemptRepository.countByAssessmentIdAndStudentIdAndIsCompletedTrueAndIsDeletedFalse(
                    assessmentId, student.getId());
            if (completed >= assessment.getMaxAttempts()) {
                throw new IllegalArgumentException(
                        "Attempt limit reached: you have used all " + assessment.getMaxAttempts()
                                + " attempt(s) for this assessment");
            }
        }

        Attempt attempt = Attempt.builder()
                .institutionId(assessment.getInstitutionId())
                .assessmentId(assessmentId)
                .studentId(student.getId())
                .startedAt(LocalDateTime.now())
                .isCompleted(false)
                .build();
        Attempt saved = attemptRepository.save(attempt);

        auditSafely(assessment.getInstitutionId(), null, null, "Attempt", saved.getId(),
                assessment.getTitle(), AuditLog.AuditAction.CREATE, null,
                Map.of("assessmentId", String.valueOf(assessmentId)));
        return toAttemptResponse(saved);
    }

    @Override
    public AttemptResponse saveAnswer(UUID attemptId, UUID callerUserId, SaveAnswerRequest request) {
        Attempt attempt = loadActiveAttempt(attemptId, callerUserId);
        Assessment assessment = assessmentRepository.findById(attempt.getAssessmentId())
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        assertAttemptStillOpen(attempt, assessment);

        Question question = questionRepository.findById(request.getQuestionId())
                .filter(q -> !q.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", request.getQuestionId()));
        if (!attempt.getAssessmentId().equals(question.getAssessmentId())) {
            throw new ResourceNotFoundException("Question", "id", request.getQuestionId());
        }

        Answer answer = answerRepository
                .findByAttemptIdAndQuestionIdAndIsDeletedFalse(attemptId, request.getQuestionId())
                .orElse(null);
        if (answer == null) {
            answer = Answer.builder()
                    .institutionId(attempt.getInstitutionId())
                    .attemptId(attemptId)
                    .questionId(request.getQuestionId())
                    .build();
        }
        if (request.getSelectedOptionId() != null) {
            answer.setSelectedOptionId(request.getSelectedOptionId());
        }
        if (request.getTextAnswer() != null) {
            answer.setTextAnswer(request.getTextAnswer());
        }
        answerRepository.save(answer);
        // Autosave never grades — grading happens once at submit.

        return buildAttemptWithAnswers(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResponse getMyAttempt(UUID assessmentId, UUID callerUserId, UUID institutionId) {
        loadScoped(assessmentId, institutionId);
        Student student = classAccessGuard.findStudentByUserId(callerUserId);
        if (student == null) {
            return null;
        }
        Attempt attempt = attemptRepository
                .findByAssessmentIdAndStudentIdAndIsCompletedAndIsDeletedFalse(
                        assessmentId, student.getId(), false)
                .orElse(null);
        if (attempt == null) {
            return null;
        }
        return buildAttemptWithAnswers(attempt);
    }

    @Override
    public AttemptResponse submitAttempt(UUID attemptId, UUID callerUserId, SubmitAssessmentRequest request) {
        Student student = classAccessGuard.findStudentByUserId(callerUserId);
        if (student == null) {
            throw new SecurityException("A student profile is required to submit this assessment");
        }

        Attempt attempt = attemptRepository.findById(attemptId)
                .filter(a -> !a.getIsDeleted() && !Boolean.TRUE.equals(a.getIsCompleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Active attempt not found with id: " + attemptId));

        if (!attempt.getStudentId().equals(student.getId())) {
            throw new IllegalArgumentException("Attempt does not belong to this student");
        }

        Assessment assessment = assessmentRepository.findById(attempt.getAssessmentId())
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        assertAttemptStillOpen(attempt, assessment);

        // Merge payload answers over autosaved answers (upsert by question, no duplicates).
        Map<UUID, Answer> answersByQuestion = new LinkedHashMap<>();
        for (Answer existing : answerRepository.findByAttemptIdAndIsDeletedFalse(attemptId)) {
            answersByQuestion.put(existing.getQuestionId(), existing);
        }
        if (request != null && request.getAnswers() != null) {
            for (SubmitAssessmentRequest.AnswerSubmission ansReq : request.getAnswers()) {
                if (ansReq.getQuestionId() == null) {
                    continue;
                }
                Question question = questionRepository.findById(ansReq.getQuestionId()).orElse(null);
                if (question == null || !attempt.getAssessmentId().equals(question.getAssessmentId())) {
                    continue;
                }
                Answer answer = answersByQuestion.get(ansReq.getQuestionId());
                if (answer == null) {
                    answer = Answer.builder()
                            .institutionId(attempt.getInstitutionId())
                            .attemptId(attemptId)
                            .questionId(ansReq.getQuestionId())
                            .build();
                }
                if (ansReq.getSelectedOptionId() != null) {
                    answer.setSelectedOptionId(ansReq.getSelectedOptionId());
                }
                if (ansReq.getTextAnswer() != null) {
                    answer.setTextAnswer(ansReq.getTextAnswer());
                }
                answersByQuestion.put(ansReq.getQuestionId(), answer);
            }
        }

        // Grade objectively-answerable questions once, at submit time.
        List<Answer> savedAnswers = new ArrayList<>();
        int totalScore = 0;
        for (Answer answer : answersByQuestion.values()) {
            Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
            if (question == null) {
                continue;
            }
            if (question.getQuestionType() == QuestionType.MCQ ||
                question.getQuestionType() == QuestionType.TRUE_FALSE) {
                answer.setIsCorrect(false);
                answer.setMarksObtained(0);
                if (answer.getSelectedOptionId() != null) {
                    Option selectedOption = optionRepository.findById(answer.getSelectedOptionId()).orElse(null);
                    // The selected option must belong to this very question (anti-tampering).
                    if (selectedOption != null && question.getId().equals(selectedOption.getQuestionId())) {
                        answer.setIsCorrect(Boolean.TRUE.equals(selectedOption.getIsCorrect()));
                        if (Boolean.TRUE.equals(selectedOption.getIsCorrect())) {
                            answer.setMarksObtained(question.getMarks());
                            totalScore += question.getMarks();
                        }
                    }
                }
            }
            // SHORT_ANSWER / ESSAY stay ungraded until manual grading (marksObtained null).
            savedAnswers.add(answerRepository.save(answer));
        }

        attempt.setIsCompleted(true);
        attempt.setSubmittedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        AssessmentResult result = resultRepository
                .findByAssessmentIdAndStudentIdAndIsDeletedFalse(assessment.getId(), student.getId())
                .orElse(null);
        if (result == null) {
            result = AssessmentResult.builder()
                    .institutionId(attempt.getInstitutionId())
                    .assessmentId(attempt.getAssessmentId())
                    .studentId(student.getId())
                    .attemptId(attemptId)
                    .build();
        }
        result.setAttemptId(attemptId);
        result.setTotalScore(totalScore);
        result.setIsPassed(assessment.getPassMarks() != null && totalScore >= assessment.getPassMarks());
        result.setGradedAt(LocalDateTime.now());

        AssessmentResult savedResult = resultRepository.save(result);

        notifyTeacherOfSubmission(assessment);
        notifyStudentOfResult(assessment, student.getId());
        auditSafely(assessment.getInstitutionId(), null, null, "Attempt", attemptId,
                assessment.getTitle(), AuditLog.AuditAction.UPDATE,
                Map.of("isCompleted", "false"),
                Map.of("totalScore", String.valueOf(totalScore),
                        "isPassed", String.valueOf(savedResult.getIsPassed()),
                        "source", "ASSESSMENT_SUBMIT"));

        AttemptResponse response = toAttemptResponse(attempt);
        response.setAnswers(savedAnswers.stream().map(this::toAnswerResponse).toList());
        response.setResult(toResultResponse(savedResult));
        return response;
    }

    // ── Results ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentResultResponse> getResults(UUID assessmentId, UUID institutionId) {
        loadScoped(assessmentId, institutionId);
        return resultRepository.findByAssessmentIdAndIsDeletedFalse(assessmentId)
                .stream().map(this::toResultResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentResultResponse getResult(UUID assessmentId, UUID studentId, UUID callerUserId, String userRole) {
        if (isLearnerRole(userRole)) {
            Student caller = classAccessGuard.findStudentByUserId(callerUserId);
            if (caller == null || !caller.getId().equals(studentId)) {
                throw new SecurityException("You can only view your own result");
            }
        }
        AssessmentResult result = resultRepository
                .findByAssessmentIdAndStudentIdAndIsDeletedFalse(assessmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));
        return toResultResponse(result);
    }

    @Override
    public AnswerResponse gradeEssay(UUID answerId, int marksObtained, String feedback, UUID gradedBy,
                                     UUID institutionId) {
        Answer answer = answerRepository.findById(answerId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        Question question = questionRepository.findById(answer.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (question.getQuestionType() != QuestionType.SHORT_ANSWER &&
            question.getQuestionType() != QuestionType.ESSAY) {
            throw new IllegalArgumentException("Only SHORT_ANSWER and ESSAY questions can be manually graded");
        }

        if (marksObtained < 0 || marksObtained > question.getMarks()) {
            throw new IllegalArgumentException(
                    "Marks obtained must be between 0 and " + question.getMarks());
        }

        // Institution enforcement: answer -> attempt -> assessment.
        Attempt attempt = attemptRepository.findById(answer.getAttemptId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
        Assessment assessment = assessmentRepository.findById(attempt.getAssessmentId())
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        if (institutionId != null && !institutionId.equals(assessment.getInstitutionId())) {
            throw new ResourceNotFoundException("Answer not found with id: " + answerId);
        }

        Integer oldMarks = answer.getMarksObtained();
        answer.setMarksObtained(marksObtained);
        answer.setFeedback(feedback);
        answer.setGradedBy(gradedBy);
        answer.setGradedAt(LocalDateTime.now());
        answer.setIsCorrect(marksObtained > 0);

        Answer savedAnswer = answerRepository.save(answer);

        recalculateResultScore(answer.getAttemptId());
        notifyStudentOfResult(assessment, attempt.getStudentId());
        auditSafely(assessment.getInstitutionId(), null, null, "Answer", savedAnswer.getId(),
                assessment.getTitle(), AuditLog.AuditAction.UPDATE,
                Map.of("marksObtained", String.valueOf(oldMarks)),
                Map.of("marksObtained", String.valueOf(marksObtained),
                        "feedback", String.valueOf(feedback),
                        "source", "ASSESSMENT_ESSAY"));

        return toAnswerResponse(savedAnswer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissions(UUID assessmentId, UUID institutionId) {
        loadScoped(assessmentId, institutionId);

        List<Attempt> attempts = attemptRepository.findByAssessmentIdAndIsDeletedFalse(assessmentId);

        return attempts.stream().map(attempt -> {
            List<Answer> answers = answerRepository.findByAttemptIdAndIsDeletedFalse(attempt.getId());

            Optional<AssessmentResult> resultOpt = resultRepository
                    .findByAssessmentIdAndStudentIdAndIsDeletedFalse(assessmentId, attempt.getStudentId());

            AssessmentResult result = resultOpt.orElse(null);

            return SubmissionResponse.builder()
                    .attemptId(attempt.getId())
                    .studentId(attempt.getStudentId())
                    .assessmentId(attempt.getAssessmentId())
                    .startedAt(attempt.getStartedAt())
                    .submittedAt(attempt.getSubmittedAt())
                    .isCompleted(attempt.getIsCompleted())
                    .answers(answers.stream().map(this::toAnswerResponse).toList())
                    .totalScore(result != null ? result.getTotalScore() : null)
                    .isPassed(result != null ? result.getIsPassed() : null)
                    .gradedBy(result != null ? result.getGradedBy() : null)
                    .gradedAt(result != null ? result.getGradedAt() : null)
                    .feedback(result != null ? result.getFeedback() : null)
                    .build();
        }).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Institution-scoped load; a cross-institution id looks like "not found". */
    private Assessment loadScoped(UUID assessmentId, UUID institutionId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment not found with id: " + assessmentId));
        if (institutionId != null && !institutionId.equals(assessment.getInstitutionId())) {
            throw new ResourceNotFoundException("Assessment not found with id: " + assessmentId);
        }
        return assessment;
    }

    /** Ownership + liveness checks shared by autosave paths. */
    private Attempt loadActiveAttempt(UUID attemptId, UUID callerUserId) {
        Student student = classAccessGuard.findStudentByUserId(callerUserId);
        if (student == null) {
            throw new SecurityException("A student profile is required for this attempt");
        }
        Attempt attempt = attemptRepository.findById(attemptId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Active attempt not found with id: " + attemptId));
        if (!attempt.getStudentId().equals(student.getId())) {
            throw new IllegalArgumentException("Attempt does not belong to this student");
        }
        if (Boolean.TRUE.equals(attempt.getIsCompleted())) {
            throw new IllegalStateException("This attempt has already been submitted");
        }
        return attempt;
    }

    /** Strict start-window enforcement (no grace). */
    private void validateAvailability(Assessment assessment, LocalDateTime now) {
        if (assessment.getStartsAt() != null && now.isBefore(assessment.getStartsAt())) {
            throw new IllegalArgumentException("This assessment has not opened yet");
        }
        if (assessment.getEndsAt() != null && now.isAfter(assessment.getEndsAt())) {
            throw new IllegalArgumentException("The window for this assessment has closed");
        }
    }

    /**
     * Submit/autosave window enforcement with a small network grace:
     * ends_at and the per-student time limit each get {@link #SUBMIT_GRACE_MINUTES}.
     */
    private void assertAttemptStillOpen(Attempt attempt, Assessment assessment) {
        LocalDateTime now = LocalDateTime.now();
        if (assessment.getEndsAt() != null && now.isAfter(assessment.getEndsAt().plusMinutes(SUBMIT_GRACE_MINUTES))) {
            throw new IllegalStateException("The window for this assessment has closed");
        }
        if (assessment.getTimeLimitMinutes() != null && attempt.getStartedAt() != null
                && now.isAfter(attempt.getStartedAt()
                        .plusMinutes(assessment.getTimeLimitMinutes() + SUBMIT_GRACE_MINUTES))) {
            throw new IllegalStateException("The time limit for this attempt has expired");
        }
    }

    private AttemptResponse buildAttemptWithAnswers(Attempt attempt) {
        AttemptResponse response = toAttemptResponse(attempt);
        response.setAnswers(answerRepository.findByAttemptIdAndIsDeletedFalse(attempt.getId())
                .stream().map(this::toAnswerResponse).toList());
        return response;
    }

    private void recalculateResultScore(UUID attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));

        if (!attempt.getIsCompleted()) {
            return;
        }

        Assessment assessment = assessmentRepository.findById(attempt.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        List<Answer> answers = answerRepository.findByAttemptIdAndIsDeletedFalse(attemptId);
        int totalScore = answers.stream()
                .filter(a -> a.getMarksObtained() != null)
                .mapToInt(Answer::getMarksObtained)
                .sum();

        Optional<AssessmentResult> resultOpt = resultRepository
                .findByAssessmentIdAndStudentIdAndIsDeletedFalse(
                        attempt.getAssessmentId(), attempt.getStudentId());

        resultOpt.ifPresent(result -> {
            result.setTotalScore(totalScore);
            result.setIsPassed(totalScore >= assessment.getPassMarks());
            result.setGradedAt(LocalDateTime.now());
            resultRepository.save(result);
        });
    }

    private static boolean isLearnerRole(String role) {
        return "STUDENT".equals(role) || "OTHER_LEARNER".equals(role);
    }

    /** Notifies the class learners when an assessment becomes published. */
    private void notifyAssessmentPublished(Assessment assessment, UUID publisherId) {
        try {
            String title = "New assessment: " + assessment.getTitle();
            String message = "A new assessment \"" + assessment.getTitle() + "\" is available for your class.";
            List<UUID> studentUserIds = classAccessGuard.resolveClassStudentUserIds(assessment.getClassGroupId());
            if (studentUserIds.isEmpty()) {
                notificationService.notifyInstitutionStudentsExcluding(
                        assessment.getInstitutionId(), publisherId, title, message,
                        "ASSESSMENT_PUBLISHED", "assessment", assessment.getId());
                return;
            }
            for (UUID studentUserId : studentUserIds) {
                if (studentUserId.equals(publisherId)) {
                    continue;
                }
                notificationService.notifyUser(studentUserId, title, message,
                        "ASSESSMENT_PUBLISHED", "assessment", assessment.getId());
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(AssessmentServiceImpl.class)
                    .warn("Failed to send ASSESSMENT_PUBLISHED for assessment {}: {}",
                            assessment.getId(), ex.getMessage());
        }
    }

    /** Tells the assessment's teacher that a learner submitted an attempt. */
    private void notifyTeacherOfSubmission(Assessment assessment) {
        try {
            if (assessment.getCreatedBy() == null || "system".equals(assessment.getCreatedBy())) {
                return;
            }
            UUID teacherUserId = userRepository.findByEmailAndIsDeletedFalse(assessment.getCreatedBy())
                    .map(User::getId).orElse(null);
            if (teacherUserId == null) {
                return;
            }
            notificationService.notifyUser(teacherUserId,
                    "Assessment submitted: " + assessment.getTitle(),
                    "A learner submitted an attempt for \"" + assessment.getTitle() + "\".",
                    "ASSESSMENT_SUBMITTED", "assessment", assessment.getId());
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(AssessmentServiceImpl.class)
                    .warn("Failed to send ASSESSMENT_SUBMITTED for assessment {}: {}",
                            assessment.getId(), ex.getMessage());
        }
    }

    /** Tells the learner their result/grade is ready. */
    private void notifyStudentOfResult(Assessment assessment, UUID studentId) {
        try {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null || student.getUserId() == null) {
                return;
            }
            notificationService.notifyUser(student.getUserId(),
                    "Assessment graded: " + assessment.getTitle(),
                    "Your attempt for \"" + assessment.getTitle() + "\" has been graded. Check your result.",
                    "ASSESSMENT_GRADED", "assessment", assessment.getId());
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(AssessmentServiceImpl.class)
                    .warn("Failed to send ASSESSMENT_GRADED for assessment {}: {}",
                            assessment.getId(), ex.getMessage());
        }
    }

    private UUID resolveActorUserId(String userEmail) {
        if (userEmail == null) {
            return null;
        }
        return userRepository.findByEmailAndIsDeletedFalse(userEmail).map(User::getId).orElse(null);
    }

    /** Audit write that can never fail the business transaction (null-safe in unit tests). */
    private void auditSafely(UUID institutionId, String userEmail, String userRole,
                             String entityType, UUID entityId, String entityName,
                             AuditLog.AuditAction action,
                             Map<String, Object> oldValues, Map<String, Object> newValues) {
        try {
            if (auditService == null) {
                return;
            }
            UUID actorId = resolveActorUserId(userEmail);
            auditService.recordAuditLog(institutionId, actorId, userEmail, userRole,
                    entityType, entityId, entityName, action, oldValues, newValues);
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(AssessmentServiceImpl.class)
                    .warn("Audit write failed for {} {}: {}", entityType, entityId, ex.getMessage());
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private AssessmentResponse toAssessmentResponse(Assessment a) {
        return AssessmentResponse.builder()
                .id(a.getId())
                .subjectId(a.getSubjectId())
                .classGroupId(a.getClassGroupId())
                .title(a.getTitle())
                .description(a.getDescription())
                .timeLimitMinutes(a.getTimeLimitMinutes())
                .totalMarks(a.getTotalMarks())
                .passMarks(a.getPassMarks())
                .isPublished(a.getIsPublished())
                .startsAt(a.getStartsAt())
                .endsAt(a.getEndsAt())
                .createdAt(a.getCreatedAt())
                .status(a.getStatus())
                .scheduledAt(a.getScheduledAt())
                .lessonId(a.getLessonId())
                .maxAttempts(a.getMaxAttempts())
                .build();
    }

    private QuestionResponse toQuestionResponse(Question q) {
        return QuestionResponse.builder()
                .id(q.getId())
                .assessmentId(q.getAssessmentId())
                .questionType(q.getQuestionType().name())
                .questionText(q.getQuestionText())
                .marks(q.getMarks())
                .sortOrder(q.getSortOrder())
                .build();
    }

    private OptionResponse toOptionResponse(Option o) {
        return OptionResponse.builder()
                .id(o.getId())
                .questionId(o.getQuestionId())
                .optionText(o.getOptionText())
                .isCorrect(o.getIsCorrect())
                .sortOrder(o.getSortOrder())
                .build();
    }

    private AttemptResponse toAttemptResponse(Attempt a) {
        return AttemptResponse.builder()
                .id(a.getId())
                .assessmentId(a.getAssessmentId())
                .studentId(a.getStudentId())
                .startedAt(a.getStartedAt())
                .submittedAt(a.getSubmittedAt())
                .isCompleted(a.getIsCompleted())
                .build();
    }

    private AnswerResponse toAnswerResponse(Answer a) {
        return AnswerResponse.builder()
                .id(a.getId())
                .attemptId(a.getAttemptId())
                .questionId(a.getQuestionId())
                .selectedOptionId(a.getSelectedOptionId())
                .textAnswer(a.getTextAnswer())
                .isCorrect(a.getIsCorrect())
                .marksObtained(a.getMarksObtained())
                .feedback(a.getFeedback())
                .gradedBy(a.getGradedBy())
                .gradedAt(a.getGradedAt())
                .build();
    }

    private AssessmentResultResponse toResultResponse(AssessmentResult r) {
        return AssessmentResultResponse.builder()
                .id(r.getId())
                .assessmentId(r.getAssessmentId())
                .studentId(r.getStudentId())
                .attemptId(r.getAttemptId())
                .totalScore(r.getTotalScore())
                .isPassed(r.getIsPassed())
                .gradedBy(r.getGradedBy())
                .gradedAt(r.getGradedAt())
                .feedback(r.getFeedback())
                .build();
    }
}
