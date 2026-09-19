package tz.elmkusoma.highereducation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.domain.ProjectStatus;
import tz.elmkusoma.highereducation.dto.ProjectDTO;
import tz.elmkusoma.highereducation.dto.ProjectMilestoneDTO;
import tz.elmkusoma.highereducation.dto.ProjectSubmissionDTO;
import tz.elmkusoma.highereducation.service.ProjectService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> listProjects(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(required = false) ProjectStatus status) {
        List<ProjectDTO> projects = status != null
                ? projectService.getProjectsByStatus(institutionId, status)
                : projectService.getProjects(institutionId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<ProjectDTO>> getProject(@PathVariable UUID id) {
        ProjectDTO project = projectService.getProject(id);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProjectDTO>> createProject(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody ProjectDTO dto) {
        dto.setInstitutionId(institutionId);
        ProjectDTO created = projectService.createProject(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProjectDTO>> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectDTO dto) {
        ProjectDTO updated = projectService.updateProject(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Project updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success("Project deleted", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getStudentProjects(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<ProjectDTO> projects = projectService.getStudentProjects(studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @GetMapping("/instructor/{instructorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getInstructorProjects(
            @PathVariable UUID instructorId) {
        List<ProjectDTO> projects = projectService.getProjectsByInstructor(instructorId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    // ── Milestones ───────────────────────────────────────────────

    @PostMapping("/{id}/milestones")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProjectMilestoneDTO>> addMilestone(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectMilestoneDTO dto) {
        ProjectMilestoneDTO created = projectService.addMilestone(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Milestone added", created));
    }

    @GetMapping("/{id}/milestones")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<ProjectMilestoneDTO>>> getMilestones(
            @PathVariable UUID id) {
        List<ProjectMilestoneDTO> milestones = projectService.getMilestones(id);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @PutMapping("/milestones/{milestoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProjectMilestoneDTO>> updateMilestone(
            @PathVariable UUID milestoneId,
            @Valid @RequestBody ProjectMilestoneDTO dto) {
        ProjectMilestoneDTO updated = projectService.updateMilestone(milestoneId, dto);
        return ResponseEntity.ok(ApiResponse.success("Milestone updated", updated));
    }

    @PutMapping("/milestones/{milestoneId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> completeMilestone(@PathVariable UUID milestoneId) {
        projectService.completeMilestone(milestoneId);
        return ResponseEntity.ok(ApiResponse.success("Milestone completed", null));
    }

    // ── Submissions ──────────────────────────────────────────────

    @PostMapping("/{id}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<ProjectSubmissionDTO>> addSubmission(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectSubmissionDTO dto) {
        ProjectSubmissionDTO created = projectService.addSubmission(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Submission added", created));
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<ProjectSubmissionDTO>>> getSubmissions(
            @PathVariable UUID id) {
        List<ProjectSubmissionDTO> submissions = projectService.getSubmissions(id);
        return ResponseEntity.ok(ApiResponse.success(submissions));
    }

    @GetMapping("/milestones/{milestoneId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<ProjectSubmissionDTO>>> getMilestoneSubmissions(
            @PathVariable UUID milestoneId) {
        List<ProjectSubmissionDTO> submissions = projectService.getMilestoneSubmissions(milestoneId);
        return ResponseEntity.ok(ApiResponse.success(submissions));
    }

    @PutMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<ProjectSubmissionDTO>> gradeSubmission(
            @PathVariable UUID submissionId,
            @RequestParam(required = false) String feedback,
            @RequestParam(required = false) String grade) {
        ProjectSubmissionDTO graded = projectService.gradeSubmission(submissionId, feedback, grade);
        return ResponseEntity.ok(ApiResponse.success("Submission graded", graded));
    }
}
