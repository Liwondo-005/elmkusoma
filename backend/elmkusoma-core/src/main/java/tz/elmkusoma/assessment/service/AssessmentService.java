package tz.elmkusoma.assessment.service;

import tz.elmkusoma.assessment.dto.request.AssessmentRequest;
import tz.elmkusoma.assessment.dto.request.QuestionRequest;
import tz.elmkusoma.assessment.dto.request.SaveAnswerRequest;
import tz.elmkusoma.assessment.dto.request.SubmitAssessmentRequest;
import tz.elmkusoma.assessment.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface AssessmentService {

    /** Create with actor context (audit + publish notifications). */
    AssessmentResponse createAssessment(UUID institutionId, AssessmentRequest request, String userEmail, String userRole);

    /** Update with institution enforcement; notifies learners when it becomes published. */
    AssessmentResponse updateAssessment(UUID assessmentId, AssessmentRequest request, UUID institutionId, String userEmail, String userRole);

    /** Soft delete with institution enforcement + audit. */
    void deleteAssessment(UUID assessmentId, UUID institutionId, String userEmail, String userRole);

    /** Class-scoped list; learners only receive published items and must be class members. */
    List<AssessmentResponse> getAssessmentsByClass(UUID classGroupId, UUID institutionId, String userEmail, String userRole);

    /** Batch list across several classes in one query (teacher/admin workspace). Institution-scoped. */
    List<AssessmentResponse> getAssessmentsByClasses(List<UUID> classGroupIds, UUID institutionId);

    /** Subject-scoped list, institution-filtered. */
    List<AssessmentResponse> getAssessmentsBySubject(UUID subjectId, UUID institutionId);

    QuestionResponse addQuestion(UUID assessmentId, UUID institutionId, QuestionRequest request, String userEmail, String userRole);

    /**
     * Questions for an assessment. When {@code includeAnswerKey} is false (learners),
     * option correctness is stripped so the answer key never reaches students.
     */
    List<QuestionResponse> getQuestions(UUID assessmentId, UUID institutionId, boolean includeAnswerKey);

    /** Remove a question (institution-enforced, audited). */
    void deleteQuestion(UUID assessmentId, UUID questionId, UUID institutionId);

    /**
     * Start an attempt. Resolves the caller's student profile (users.id -> students.id)
     * and enforces published state, availability window and max attempts.
     *
     * @param callerUserId the authenticated user id (users.id) from the request context
     */
    AttemptResponse startAttempt(UUID assessmentId, UUID callerUserId, UUID institutionId);

    /** Autosave one answer for the caller's active attempt (upsert, not graded until submit). */
    AttemptResponse saveAnswer(UUID attemptId, UUID callerUserId, SaveAnswerRequest request);

    /** The caller's active (incomplete) attempt with saved answers, or null when none exists. */
    AttemptResponse getMyAttempt(UUID assessmentId, UUID callerUserId, UUID institutionId);

    /** Submit the caller's active attempt; merges autosaved answers and grades objectively. */
    AttemptResponse submitAttempt(UUID attemptId, UUID callerUserId, SubmitAssessmentRequest request);

    List<AssessmentResultResponse> getResults(UUID assessmentId, UUID institutionId);

    /** Result read; learners may only read their own result. */
    AssessmentResultResponse getResult(UUID assessmentId, UUID studentId, UUID callerUserId, String userRole);

    AnswerResponse gradeEssay(UUID answerId, int marksObtained, String feedback, UUID gradedBy, UUID institutionId);

    List<SubmissionResponse> getSubmissions(UUID assessmentId, UUID institutionId);
}
