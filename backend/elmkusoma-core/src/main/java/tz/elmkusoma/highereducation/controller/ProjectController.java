package tz.elmkusoma.highereducation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.ProjectMilestone;
import tz.elmkusoma.highereducation.domain.ProjectStatus;
import tz.elmkusoma.highereducation.domain.ProjectSubmission;
import tz.elmkusoma.highereducation.dto.ProjectDTO;
import tz.elmkusoma.highereducation.dto.ProjectMilestoneDTO;
import tz.elmkusoma.highereducation.dto.ProjectSubmissionDTO;
import tz.elmkusoma.highereducation.repository.ProjectMilestoneRepository;
import tz.elmkusoma.highereducation.repository.ProjectSubmissionRepository;
import tz.elmkusoma.highereducation.service.HighEdIdentity;
import tz.elmkusoma.highereducation.service.ProjectService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectSubmissionRepository projectSubmissionRepository;
    private final HighEdIdentity highEdIdentity;

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
    public ResponseEntity<ApiResponse<ProjectDTO>> getProject(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        ProjectDTO project = projectService.getProject(id);
        assertProjectReadable(project, callerUserId, userRole, serverInstitutionId, id);
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
            @Valid @RequestBody ProjectDTO dto,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertProjectTenant(projectService.getProject(id), serverInstitutionId, userRole);
        ProjectDTO updated = projectService.updateProject(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Project updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertProjectTenant(projectService.getProject(id), serverInstitutionId, userRole);
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success("Project deleted", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getStudentProjects(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        List<ProjectDTO> projects = projectService.getStudentProjects(learnerId, institutionId);
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
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        assertProjectReadable(projectService.getProject(id), callerUserId, userRole, serverInstitutionId, id);
        List<ProjectMilestoneDTO> milestones = projectService.getMilestones(id);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @PutMapping("/milestones/{milestoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProjectMilestoneDTO>> updateMilestone(
            @PathVariable UUID milestoneId,
            @Valid @RequestBody ProjectMilestoneDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        assertChildTenant(getProjectMilestone(milestoneId).getInstitutionId(),
                serverInstitutionId, userRole, "Project milestone");
        ProjectMilestoneDTO updated = projectService.updateMilestone(milestoneId, dto);
        return ResponseEntity.ok(ApiResponse.success("Milestone updated", updated));
    }

    @PutMapping("/milestones/{milestoneId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> completeMilestone(
            @PathVariable UUID milestoneId,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        assertChildTenant(getProjectMilestone(milestoneId).getInstitutionId(),
                serverInstitutionId, userRole, "Project milestone");
        projectService.completeMilestone(milestoneId);
        return ResponseEntity.ok(ApiResponse.success("Milestone completed", null));
    }

    // ── Submissions ──────────────────────────────────────────────

    @PostMapping("/{id}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<ProjectSubmissionDTO>> addSubmission(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectSubmissionDTO dto,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        ProjectDTO project = projectService.getProject(id);
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, project.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        ProjectSubmissionDTO created = projectService.addSubmission(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Submission added", created));
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<ProjectSubmissionDTO>>> getSubmissions(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        assertProjectReadable(projectService.getProject(id), callerUserId, userRole, serverInstitutionId, id);
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
            @RequestParam(required = false) String grade,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        ProjectSubmission submission = projectSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectSubmission", "id", submissionId));
        assertChildTenant(submission.getInstitutionId(), serverInstitutionId, userRole, "Project submission");
        ProjectSubmissionDTO graded = projectService.gradeSubmission(submissionId, feedback, grade);
        return ResponseEntity.ok(ApiResponse.success("Submission graded", graded));
    }

    private ProjectMilestone getProjectMilestone(UUID milestoneId) {
        return projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", "id", milestoneId));
    }

    private void assertProjectReadable(ProjectDTO project, UUID callerUserId, String userRole,
                                       UUID serverInstitutionId, UUID id) {
        if (HighEdIdentity.isLearner(userRole)) {
            if (project.getStudentId() != null) {
                try {
                    highEdIdentity.resolveStudentId(callerUserId, userRole, project.getStudentId());
                } catch (ForbiddenException e) {
                    throw new ResourceNotFoundException("Project", "id", id);
                }
            } else if (serverInstitutionId == null || !serverInstitutionId.equals(project.getInstitutionId())) {
                throw new ResourceNotFoundException("Project", "id", id);
            }
            return;
        }
        assertProjectTenant(project, serverInstitutionId, userRole);
    }

    private void assertChildTenant(UUID targetInstitutionId, UUID serverInstitutionId,
                                   String userRole, String resourceName) {
        if (targetInstitutionId == null || serverInstitutionId == null || "ADMIN".equals(userRole)) {
            return;
        }
        if (!serverInstitutionId.equals(targetInstitutionId)) {
            throw new ForbiddenException(resourceName, "access");
        }
    }

    private void assertProjectTenant(ProjectDTO project, UUID serverInstitutionId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return;
        }
        if (serverInstitutionId == null || !serverInstitutionId.equals(project.getInstitutionId())) {
            throw new ForbiddenException("Project", "access");
        }
    }
}
