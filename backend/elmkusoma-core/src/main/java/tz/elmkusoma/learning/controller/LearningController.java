package tz.elmkusoma.learning.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.request.ProgressRequest;
import tz.elmkusoma.learning.dto.response.*;
import tz.elmkusoma.learning.service.LearningService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/learning")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
@Tag(name = "Learning", description = "Lessons, assignments, and progress management")
public class LearningController {

    private final LearningService learningService;

    @PostMapping("/lessons")
    @Operation(summary = "Create a new lesson")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @RequestAttribute("institutionId") UUID institutionId,
            @Valid @RequestBody LessonRequest request) {
        LessonResponse response = learningService.createLesson(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lesson created", response));
    }

    @GetMapping("/lessons/subject/{subjectId}/class/{classGroupId}")
    @Operation(summary = "Get lessons by subject and class")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessons(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID subjectId, @PathVariable UUID classGroupId) {
        List<LessonResponse> response = learningService.getLessonsBySubjectAndClass(subjectId, classGroupId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/lessons/class/{classGroupId}")
    @Operation(summary = "Get all lessons for a class")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessonsByClass(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID classGroupId) {
        List<LessonResponse> response = learningService.getLessonsByClass(classGroupId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/progress")
    @Operation(summary = "Update lesson progress for a student")
    public ResponseEntity<ApiResponse<ProgressResponse>> updateProgress(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID studentId,
            @Valid @RequestBody ProgressRequest request) {
        ProgressResponse response = learningService.updateProgress(institutionId, studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Progress updated", response));
    }

    @GetMapping("/progress/student/{studentId}")
    @Operation(summary = "Get all lesson progress for a student")
    public ResponseEntity<ApiResponse<List<ProgressResponse>>> getStudentProgress(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID studentId) {
        List<ProgressResponse> response = learningService.getStudentProgress(studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/progress/student/{studentId}/average")
    @Operation(summary = "Get average completion for a student")
    public ResponseEntity<ApiResponse<Double>> getAverageCompletion(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID studentId) {
        Double average = learningService.getStudentAverageCompletion(studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(average));
    }

    @PostMapping("/assignments")
    @Operation(summary = "Create a new assignment")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @RequestAttribute("institutionId") UUID institutionId,
            @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse response = learningService.createAssignment(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment created", response));
    }

    @GetMapping("/assignments/class/{classGroupId}")
    @Operation(summary = "Get assignments by class")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getAssignments(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID classGroupId) {
        List<AssignmentResponse> response = learningService.getAssignmentsByClass(classGroupId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/assignments/{id}/submit")
    @Operation(summary = "Submit an assignment")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitAssignment(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID studentId,
            @RequestAttribute("institutionId") UUID institutionId) {
        SubmissionResponse response = learningService.submitAssignment(id, studentId, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment submitted", response));
    }

    @GetMapping("/assignments/{id}/submissions")
    @Operation(summary = "Get all submissions for an assignment")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissions(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        List<SubmissionResponse> response = learningService.getSubmissionsByAssignment(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/submissions/{id}/grade")
    @Operation(summary = "Grade a submission")
    public ResponseEntity<ApiResponse<SubmissionResponse>> gradeSubmission(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id,
            @RequestParam Integer grade,
            @RequestParam(required = false) String feedback,
            @RequestAttribute("userId") UUID gradedBy) {
        SubmissionResponse response = learningService.gradeSubmission(id, grade, feedback, gradedBy, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Submission graded", response));
    }
}
