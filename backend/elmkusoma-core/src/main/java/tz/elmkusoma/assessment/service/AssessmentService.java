package tz.elmkusoma.assessment.service;

import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.dto.request.AssessmentRequest;
import tz.elmkusoma.assessment.dto.request.QuestionRequest;
import tz.elmkusoma.assessment.dto.request.SubmitAssessmentRequest;
import tz.elmkusoma.assessment.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface AssessmentService {

    AssessmentResponse createAssessment(UUID institutionId, AssessmentRequest request);

    List<AssessmentResponse> getAssessmentsByClass(UUID classGroupId, UUID institutionId);

    List<AssessmentResponse> getAssessmentsBySubject(UUID subjectId, UUID institutionId);

    QuestionResponse addQuestion(UUID assessmentId, UUID institutionId, QuestionRequest request);

    List<QuestionResponse> getQuestions(UUID assessmentId, UUID institutionId);

    AttemptResponse startAttempt(UUID assessmentId, UUID studentId, UUID institutionId);

    AttemptResponse submitAttempt(UUID attemptId, UUID studentId, SubmitAssessmentRequest request);

    List<AssessmentResultResponse> getResults(UUID assessmentId, UUID institutionId);

    AssessmentResultResponse getResult(UUID assessmentId, UUID studentId, UUID institutionId);
}
