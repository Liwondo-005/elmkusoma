package tz.elmkusoma.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.assessment.domain.*;
import tz.elmkusoma.assessment.dto.request.AssessmentRequest;
import tz.elmkusoma.assessment.dto.request.QuestionRequest;
import tz.elmkusoma.assessment.dto.request.SubmitAssessmentRequest;
import tz.elmkusoma.assessment.dto.response.*;
import tz.elmkusoma.assessment.repository.*;
import tz.elmkusoma.assessment.service.AssessmentService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final AssessmentResultRepository resultRepository;

    @Override
    public AssessmentResponse createAssessment(UUID institutionId, AssessmentRequest request) {
        Assessment assessment = Assessment.builder()
                .institutionId(institutionId)
                .subjectId(request.getSubjectId())
                .classGroupId(request.getClassGroupId())
                .title(request.getTitle())
                .description(request.getDescription())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .totalMarks(request.getTotalMarks())
                .passMarks(request.getPassMarks())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .build();

        return toAssessmentResponse(assessmentRepository.save(assessment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAssessmentsByClass(UUID classGroupId) {
        return assessmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)
                .stream().map(this::toAssessmentResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAssessmentsBySubject(UUID subjectId) {
        return assessmentRepository.findBySubjectIdAndIsDeletedFalse(subjectId)
                .stream().map(this::toAssessmentResponse).toList();
    }

    @Override
    public QuestionResponse addQuestion(UUID assessmentId, UUID institutionId, QuestionRequest request) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with id: " + assessmentId));

        Question question = Question.builder()
                .institutionId(institutionId)
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
                        .institutionId(institutionId)
                        .questionId(savedQuestion.getId())
                        .optionText(optReq.getOptionText())
                        .isCorrect(optReq.getIsCorrect() != null ? optReq.getIsCorrect() : false)
                        .sortOrder(optReq.getSortOrder() != null ? optReq.getSortOrder() : i)
                        .build();
                Option savedOption = optionRepository.save(option);
                optionResponses.add(toOptionResponse(savedOption));
            }
        }

        QuestionResponse response = toQuestionResponse(savedQuestion);
        response.setOptions(optionResponses);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions(UUID assessmentId) {
        List<Question> questions = questionRepository.findByAssessmentIdAndIsDeletedFalseOrderBySortOrder(assessmentId);
        return questions.stream().map(q -> {
            QuestionResponse response = toQuestionResponse(q);
            response.setOptions(
                    optionRepository.findByQuestionIdAndIsDeletedFalseOrderBySortOrder(q.getId())
                            .stream().map(this::toOptionResponse).toList()
            );
            return response;
        }).toList();
    }

    @Override
    public AttemptResponse startAttempt(UUID assessmentId, UUID studentId, UUID institutionId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with id: " + assessmentId));

        boolean hasIncompleteAttempt = attemptRepository
                .findByAssessmentIdAndStudentIdAndIsCompletedAndIsDeletedFalse(
                        assessmentId, studentId, false)
                .isPresent();

        if (hasIncompleteAttempt) {
            throw new IllegalArgumentException("Student already has an incomplete attempt for this assessment");
        }

        Attempt attempt = Attempt.builder()
                .institutionId(institutionId)
                .assessmentId(assessmentId)
                .studentId(studentId)
                .startedAt(LocalDateTime.now())
                .isCompleted(false)
                .build();

        return toAttemptResponse(attemptRepository.save(attempt));
    }

    @Override
    public AttemptResponse submitAttempt(UUID attemptId, UUID studentId, SubmitAssessmentRequest request) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .filter(a -> !a.getIsDeleted() && !a.getIsCompleted())
                .orElseThrow(() -> new ResourceNotFoundException("Active attempt not found with id: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Attempt does not belong to this student");
        }

        Assessment assessment = assessmentRepository.findById(attempt.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        attempt.setIsCompleted(true);
        attempt.setSubmittedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        List<Answer> savedAnswers = new ArrayList<>();
        int totalScore = 0;

        if (request.getAnswers() != null) {
            for (SubmitAssessmentRequest.AnswerSubmission ansReq : request.getAnswers()) {
                Question question = questionRepository.findById(ansReq.getQuestionId()).orElse(null);
                if (question == null) continue;

                Answer answer = Answer.builder()
                        .institutionId(attempt.getInstitutionId())
                        .attemptId(attemptId)
                        .questionId(ansReq.getQuestionId())
                        .selectedOptionId(ansReq.getSelectedOptionId())
                        .textAnswer(ansReq.getTextAnswer())
                        .build();

                if (question.getQuestionType() == QuestionType.MCQ ||
                    question.getQuestionType() == QuestionType.TRUE_FALSE) {
                    if (ansReq.getSelectedOptionId() != null) {
                        Option selectedOption = optionRepository.findById(ansReq.getSelectedOptionId()).orElse(null);
                        if (selectedOption != null) {
                            answer.setIsCorrect(selectedOption.getIsCorrect());
                            if (selectedOption.getIsCorrect()) {
                                answer.setMarksObtained(question.getMarks());
                                totalScore += question.getMarks();
                            } else {
                                answer.setMarksObtained(0);
                            }
                        }
                    }
                }

                savedAnswers.add(answerRepository.save(answer));
            }
        }

        AssessmentResult result = AssessmentResult.builder()
                .institutionId(attempt.getInstitutionId())
                .assessmentId(attempt.getAssessmentId())
                .studentId(studentId)
                .attemptId(attemptId)
                .totalScore(totalScore)
                .isPassed(totalScore >= assessment.getPassMarks())
                .gradedAt(LocalDateTime.now())
                .build();

        resultRepository.save(result);

        AttemptResponse response = toAttemptResponse(attempt);
        response.setAnswers(savedAnswers.stream().map(this::toAnswerResponse).toList());
        response.setResult(toResultResponse(result));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentResultResponse> getResults(UUID assessmentId) {
        return resultRepository.findByAssessmentIdAndIsDeletedFalse(assessmentId)
                .stream().map(this::toResultResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentResultResponse getResult(UUID assessmentId, UUID studentId) {
        AssessmentResult result = resultRepository
                .findByAssessmentIdAndStudentIdAndIsDeletedFalse(assessmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));
        return toResultResponse(result);
    }

    @Override
    public AnswerResponse gradeEssay(UUID answerId, int marksObtained, String feedback, UUID gradedBy) {
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

        answer.setMarksObtained(marksObtained);
        answer.setFeedback(feedback);
        answer.setGradedBy(gradedBy);
        answer.setGradedAt(LocalDateTime.now());
        answer.setIsCorrect(marksObtained > 0);

        Answer savedAnswer = answerRepository.save(answer);

        recalculateResultScore(answer.getAttemptId());

        return toAnswerResponse(savedAnswer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissions(UUID assessmentId) {
        assessmentRepository.findById(assessmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with id: " + assessmentId));

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
