package tz.elmkusoma.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.assessment.dto.request.AssessmentRequest;
import tz.elmkusoma.assessment.dto.request.QuestionRequest;
import tz.elmkusoma.assessment.dto.request.SaveAnswerRequest;
import tz.elmkusoma.assessment.dto.request.SubmitAssessmentRequest;
import tz.elmkusoma.assessment.dto.response.*;
import tz.elmkusoma.assessment.service.AssessmentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/assessments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
@Tag(name = "Assessment", description = "Assessment, quiz, and exam management")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping
    @Operation(summary = "Create a new assessment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> createAssessment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole,
            @Valid @RequestBody AssessmentRequest request) {
        AssessmentResponse response = assessmentService.createAssessment(institutionId, request, userEmail, userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assessment created", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an assessment (including publish/unpublish)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> updateAssessment(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole,
            @Valid @RequestBody AssessmentRequest request) {
        AssessmentResponse response = assessmentService.updateAssessment(id, request, institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Assessment updated", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an assessment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteAssessment(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        assessmentService.deleteAssessment(id, institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Assessment deleted", null));
    }

    @GetMapping("/class/{classGroupId}")
    @Operation(summary = "Get assessments by class")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getByClass(
            @PathVariable UUID classGroupId,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        List<AssessmentResponse> response =
                assessmentService.getAssessmentsByClass(classGroupId, institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/classes")
    @Operation(summary = "Get assessments across multiple classes in one request (teacher workspace)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getByClasses(
            @RequestParam("ids") List<UUID> ids,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<AssessmentResponse> response = assessmentService.getAssessmentsByClasses(ids, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(summary = "Get assessments by subject")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getBySubject(
            @PathVariable UUID subjectId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<AssessmentResponse> response = assessmentService.getAssessmentsBySubject(subjectId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/questions")
    @Operation(summary = "Add a question to an assessment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionResponse>> addQuestion(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole,
            @Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = assessmentService.addQuestion(id, institutionId, request, userEmail, userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question added", response));
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @Operation(summary = "Remove a question from an assessment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        assessmentService.deleteQuestion(id, questionId, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Question removed", null));
    }

    @GetMapping("/{id}/questions")
    @Operation(summary = "Get all questions for an assessment (answer key stripped for learners)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuestions(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userRole") String userRole) {
        boolean includeAnswerKey = !isLearnerRole(userRole);
        List<QuestionResponse> response = assessmentService.getQuestions(id, institutionId, includeAnswerKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start an assessment attempt")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<AttemptResponse>> startAttempt(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        AttemptResponse response = assessmentService.startAttempt(id, callerUserId, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attempt started", response));
    }

    @GetMapping("/{id}/my-attempt")
    @Operation(summary = "The caller's active attempt with saved answers (resume)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<AttemptResponse>> getMyAttempt(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        AttemptResponse response = assessmentService.getMyAttempt(id, callerUserId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/attempts/{attemptId}/answers")
    @Operation(summary = "Autosave one answer for an active attempt")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<AttemptResponse>> saveAnswer(
            @PathVariable UUID attemptId,
            @RequestAttribute("userId") UUID callerUserId,
            @Valid @RequestBody SaveAnswerRequest request) {
        AttemptResponse response = assessmentService.saveAnswer(attemptId, callerUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Answer saved", response));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(summary = "Submit an assessment attempt")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<AttemptResponse>> submitAttempt(
            @PathVariable UUID attemptId,
            @RequestAttribute("userId") UUID callerUserId,
            @Valid @RequestBody SubmitAssessmentRequest request) {
        AttemptResponse response = assessmentService.submitAttempt(attemptId, callerUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Assessment submitted", response));
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get all results for an assessment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentResultResponse>>> getResults(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<AssessmentResultResponse> response = assessmentService.getResults(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/results/student/{studentId}")
    @Operation(summary = "Get a student's result for an assessment (learners: own result only)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<AssessmentResultResponse>> getResult(
            @PathVariable UUID id,
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole) {
        AssessmentResultResponse response =
                assessmentService.getResult(id, studentId, callerUserId, userRole, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/answers/{answerId}/grade")
    @Operation(summary = "Grade a SHORT_ANSWER or ESSAY answer manually")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AnswerResponse>> gradeEssay(
            @PathVariable UUID answerId,
            @RequestParam int marksObtained,
            @RequestParam(required = false) String feedback,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID gradedBy) {
        AnswerResponse response =
                assessmentService.gradeEssay(answerId, marksObtained, feedback, gradedBy, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Answer graded", response));
    }

    @GetMapping("/{id}/submissions")
    @Operation(summary = "Get all submissions for an assessment with student info")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissions(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<SubmissionResponse> response = assessmentService.getSubmissions(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private static boolean isLearnerRole(String role) {
        return "STUDENT".equals(role) || "OTHER_LEARNER".equals(role) || "PARENT".equals(role);
    }
}
