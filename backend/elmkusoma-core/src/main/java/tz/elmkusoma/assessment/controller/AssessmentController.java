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
    public ResponseEntity<ApiResponse<AssessmentResponse>> createAssessment(
            @RequestAttribute("institutionId") UUID institutionId,
            @Valid @RequestBody AssessmentRequest request) {
        AssessmentResponse response = assessmentService.createAssessment(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assessment created", response));
    }

    @GetMapping("/class/{classGroupId}")
    @Operation(summary = "Get assessments by class")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getByClass(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID classGroupId) {
        List<AssessmentResponse> response = assessmentService.getAssessmentsByClass(classGroupId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(summary = "Get assessments by subject")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getBySubject(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID subjectId) {
        List<AssessmentResponse> response = assessmentService.getAssessmentsBySubject(subjectId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/questions")
    @Operation(summary = "Add a question to an assessment")
    public ResponseEntity<ApiResponse<QuestionResponse>> addQuestion(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID institutionId,
            @Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = assessmentService.addQuestion(id, institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question added", response));
    }

    @GetMapping("/{id}/questions")
    @Operation(summary = "Get all questions for an assessment")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuestions(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        List<QuestionResponse> response = assessmentService.getQuestions(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start an assessment attempt")
    public ResponseEntity<ApiResponse<AttemptResponse>> startAttempt(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID studentId,
            @RequestAttribute("institutionId") UUID institutionId) {
        AttemptResponse response = assessmentService.startAttempt(id, studentId, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attempt started", response));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(summary = "Submit an assessment attempt")
    public ResponseEntity<ApiResponse<AttemptResponse>> submitAttempt(
            @PathVariable UUID attemptId,
            @RequestAttribute("userId") UUID studentId,
            @Valid @RequestBody SubmitAssessmentRequest request) {
        AttemptResponse response = assessmentService.submitAttempt(attemptId, studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Assessment submitted", response));
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get all results for an assessment")
    public ResponseEntity<ApiResponse<List<AssessmentResultResponse>>> getResults(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        List<AssessmentResultResponse> response = assessmentService.getResults(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/results/student/{studentId}")
    @Operation(summary = "Get a student's result for an assessment")
    public ResponseEntity<ApiResponse<AssessmentResultResponse>> getResult(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id, @PathVariable UUID studentId) {
        AssessmentResultResponse response = assessmentService.getResult(id, studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
