package tz.elmkusoma.learning.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.request.ProgressRequest;
import tz.elmkusoma.learning.dto.response.*;
import tz.elmkusoma.learning.service.LearningService;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/learning")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
@Tag(name = "Learning", description = "Lessons, assignments, and progress management")
public class LearningController {

    private final LearningService learningService;
    private final StudentRepository studentRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;

    @PostMapping("/lessons")
    @Operation(summary = "Create a new lesson")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody LessonRequest request) {
        LessonResponse response = learningService.createLesson(institutionId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lesson created", response));
    }

    @PutMapping("/lessons/{id}")
    @Operation(summary = "Update a lesson")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable UUID id,
            @Valid @RequestBody LessonRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("userId") UUID userId) {
        LessonResponse response = learningService.updateLesson(id, request, institutionId, userEmail, userRole, userId);
        return ResponseEntity.ok(ApiResponse.success("Lesson updated", response));
    }

    @PutMapping("/lessons/{id}/status")
    @Operation(summary = "Transition a lesson publication status")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> setLessonStatus(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole,
            @RequestBody Map<String, String> body) {
        learningService.setLessonStatus(id, body.get("status"), institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Lesson status updated", null));
    }

    @DeleteMapping("/lessons/{id}")
    @Operation(summary = "Delete a lesson")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        learningService.deleteLesson(id, institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Lesson deleted", null));
    }

    @PostMapping("/lessons/{id}/publish")
    @Operation(summary = "Publish a lesson (DRAFT -> PUBLISHED)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LessonResponse>> publishLesson(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        LessonResponse response = learningService.setLessonStatus(id, "PUBLISHED", institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Lesson published", response));
    }

    @PostMapping("/lessons/{id}/unpublish")
    @Operation(summary = "Unpublish a lesson (PUBLISHED -> DRAFT)")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LessonResponse>> unpublishLesson(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        LessonResponse response = learningService.setLessonStatus(id, "DRAFT", institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Lesson unpublished", response));
    }

    @PostMapping("/lessons/{id}/archive")
    @Operation(summary = "Archive a lesson")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LessonResponse>> archiveLesson(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        LessonResponse response = learningService.setLessonStatus(id, "ARCHIVED", institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Lesson archived", response));
    }

    @GetMapping("/lessons/subject/{subjectId}/class/{classGroupId}")
    @Operation(summary = "Get lessons by subject and class")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessons(
            @PathVariable UUID subjectId,
            @PathVariable UUID classGroupId,
            @RequestAttribute("userRole") String userRole) {
        List<LessonResponse> response = learningService.getLessonsBySubjectAndClass(
                subjectId, classGroupId, canSeeUnpublished(userRole));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/lessons/class/{classGroupId}")
    @Operation(summary = "Get all lessons for a class")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessonsByClass(
            @PathVariable UUID classGroupId,
            @RequestAttribute("userRole") String userRole) {
        List<LessonResponse> response = learningService.getLessonsByClass(
                classGroupId, canSeeUnpublished(userRole));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** Learners only ever see PUBLISHED, non-archived lessons. */
    private static boolean canSeeUnpublished(String userRole) {
        return !"STUDENT".equals(userRole) && !"OTHER_LEARNER".equals(userRole) && !"PARENT".equals(userRole);
    }

    @PostMapping("/progress")
    @Operation(summary = "Update lesson progress for a student")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<ProgressResponse>> updateProgress(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID studentId,
            @Valid @RequestBody ProgressRequest request) {
        ProgressResponse response = learningService.updateProgress(institutionId, studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Progress updated", response));
    }

    @GetMapping("/progress/student/{studentId}")
    @Operation(summary = "Get all lesson progress for a student")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<List<ProgressResponse>>> getStudentProgress(
            @PathVariable UUID studentId, HttpServletRequest request) {
        verifyStudentAccess(studentId, request);
        List<ProgressResponse> response = learningService.getStudentProgress(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/progress/student/{studentId}/average")
    @Operation(summary = "Get average completion for a student")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<Double>> getAverageCompletion(
            @PathVariable UUID studentId, HttpServletRequest request) {
        verifyStudentAccess(studentId, request);
        Double average = learningService.getStudentAverageCompletion(studentId);
        return ResponseEntity.ok(ApiResponse.success(average));
    }

    private void verifyStudentAccess(UUID studentId, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if ("STUDENT".equals(role)) {
            UUID userId = (UUID) request.getAttribute("userId");
            var student = studentRepository.findByUserIdAndIsDeletedFalse(userId).orElse(null);
            if (student == null || !student.getId().equals(studentId)) {
                throw new AccessDeniedException("You can only access your own progress");
            }
        } else if ("PARENT".equals(role)) {
            UUID userId = (UUID) request.getAttribute("userId");
            var parent = parentStudentLinkRepository.findAllByParentId(userId);
            boolean isChild = parent.stream().anyMatch(link -> link.getStudentId().equals(studentId));
            if (!isChild) {
                throw new AccessDeniedException("You can only access your child's progress");
            }
        }
    }

    @PostMapping("/assignments")
    @Operation(summary = "Create a new assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse response = learningService.createAssignment(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment created", response));
    }

    @PutMapping("/assignments/{id}")
    @Operation(summary = "Update an assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            @PathVariable UUID id,
            @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse response = learningService.updateAssignment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Assignment updated", response));
    }

    @DeleteMapping("/assignments/{id}")
    @Operation(summary = "Delete an assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(@PathVariable UUID id) {
        learningService.deleteAssignment(id);
        return ResponseEntity.ok(ApiResponse.success("Assignment deleted", null));
    }

    @GetMapping("/assignments/class/{classGroupId}")
    @Operation(summary = "Get assignments by class")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getAssignments(@PathVariable UUID classGroupId) {
        List<AssignmentResponse> response = learningService.getAssignmentsByClass(classGroupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/assignments/{id}/submit")
    @Operation(summary = "Submit an assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitAssignment(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        SubmissionResponse response = learningService.submitAssignment(id, studentId, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment submitted", response));
    }

    @GetMapping("/assignments/{id}/submissions")
    @Operation(summary = "Get all submissions for an assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissions(@PathVariable UUID id) {
        List<SubmissionResponse> response = learningService.getSubmissionsByAssignment(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/submissions/{id}/grade")
    @Operation(summary = "Grade a submission")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> gradeSubmission(
            @PathVariable UUID id,
            @RequestParam Integer grade,
            @RequestParam(required = false) String feedback,
            @RequestAttribute("userId") UUID gradedBy) {
        SubmissionResponse response = learningService.gradeSubmission(id, grade, feedback, gradedBy);
        return ResponseEntity.ok(ApiResponse.success("Submission graded", response));
    }
}
