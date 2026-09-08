package tz.elmkusoma.learning.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
@Tag(name = "Learning", description = "Lessons, assignments, and progress management")
public class LearningController {

    private final LearningService learningService;

    @PostMapping("/lessons")
    @Operation(summary = "Create a new lesson")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody LessonRequest request) {
        LessonResponse response = learningService.createLesson(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lesson created", response));
    }

    @GetMapping("/lessons/subject/{subjectId}/class/{classGroupId}")
    @Operation(summary = "Get lessons by subject and class")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessons(
            @PathVariable UUID subjectId, @PathVariable UUID classGroupId) {
        List<LessonResponse> response = learningService.getLessonsBySubjectAndClass(subjectId, classGroupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/lessons/class/{classGroupId}")
    @Operation(summary = "Get all lessons for a class")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessonsByClass(@PathVariable UUID classGroupId) {
        List<LessonResponse> response = learningService.getLessonsByClass(classGroupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/progress")
    @Operation(summary = "Update lesson progress for a student")
    public ResponseEntity<ApiResponse<ProgressResponse>> updateProgress(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID studentId,
            @Valid @RequestBody ProgressRequest request) {
        ProgressResponse response = learningService.updateProgress(institutionId, studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Progress updated", response));
    }

    @GetMapping("/progress/student/{studentId}")
    @Operation(summary = "Get all lesson progress for a student")
    public ResponseEntity<ApiResponse<List<ProgressResponse>>> getStudentProgress(@PathVariable UUID studentId) {
        List<ProgressResponse> response = learningService.getStudentProgress(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/progress/student/{studentId}/average")
    @Operation(summary = "Get average completion for a student")
    public ResponseEntity<ApiResponse<Double>> getAverageCompletion(@PathVariable UUID studentId) {
        Double average = learningService.getStudentAverageCompletion(studentId);
        return ResponseEntity.ok(ApiResponse.success(average));
    }

    @PostMapping("/assignments")
    @Operation(summary = "Create a new assignment")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse response = learningService.createAssignment(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment created", response));
    }

    @GetMapping("/assignments/class/{classGroupId}")
    @Operation(summary = "Get assignments by class")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getAssignments(@PathVariable UUID classGroupId) {
        List<AssignmentResponse> response = learningService.getAssignmentsByClass(classGroupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/assignments/{id}/submit")
    @Operation(summary = "Submit an assignment")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitAssignment(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        SubmissionResponse response = learningService.submitAssignment(id, studentId, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment submitted", response));
    }

    @GetMapping("/assignments/{id}/submissions")
    @Operation(summary = "Get all submissions for an assignment")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissions(@PathVariable UUID id) {
        List<SubmissionResponse> response = learningService.getSubmissionsByAssignment(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/submissions/{id}/grade")
    @Operation(summary = "Grade a submission")
    public ResponseEntity<ApiResponse<SubmissionResponse>> gradeSubmission(
            @PathVariable UUID id,
            @RequestParam Integer grade,
            @RequestParam(required = false) String feedback,
            @RequestHeader("X-User-Id") UUID gradedBy) {
        SubmissionResponse response = learningService.gradeSubmission(id, grade, feedback, gradedBy);
        return ResponseEntity.ok(ApiResponse.success("Submission graded", response));
    }
}
