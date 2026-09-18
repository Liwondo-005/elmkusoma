package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.*;
import tz.elmkusoma.highereducation.dto.ProjectDTO;
import tz.elmkusoma.highereducation.dto.ProjectMilestoneDTO;
import tz.elmkusoma.highereducation.dto.ProjectSubmissionDTO;
import tz.elmkusoma.highereducation.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectSubmissionRepository projectSubmissionRepository;

    // ── Project CRUD ─────────────────────────────────────────────

    public ProjectDTO createProject(ProjectDTO dto) {
        Project project = Project.builder()
                .institutionId(dto.getInstitutionId())
                .studentId(dto.getStudentId())
                .subjectId(dto.getSubjectId())
                .title(dto.getTitle())
                .objective(dto.getObjective())
                .description(dto.getDescription())
                .status(dto.getStatus() != null ? dto.getStatus() : ProjectStatus.IDEATION)
                .instructorId(dto.getInstructorId())
                .startDate(dto.getStartDate())
                .dueDate(dto.getDueDate())
                .completedDate(dto.getCompletedDate())
                .build();
        Project saved = projectRepository.save(project);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjects(UUID institutionId) {
        return projectRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProject(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        return toDTO(project);
    }

    public ProjectDTO updateProject(UUID id, ProjectDTO dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        project.setStudentId(dto.getStudentId());
        project.setSubjectId(dto.getSubjectId());
        project.setTitle(dto.getTitle());
        project.setObjective(dto.getObjective());
        project.setDescription(dto.getDescription());
        project.setStatus(dto.getStatus());
        project.setInstructorId(dto.getInstructorId());
        project.setStartDate(dto.getStartDate());
        project.setDueDate(dto.getDueDate());
        project.setCompletedDate(dto.getCompletedDate());
        Project saved = projectRepository.save(project);
        return toDTO(saved);
    }

    public void deleteProject(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        project.setIsDeleted(true);
        projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getStudentProjects(UUID studentId, UUID institutionId) {
        return projectRepository.findByStudentIdAndInstitutionIdAndIsDeletedFalse(studentId, institutionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByStatus(UUID institutionId, ProjectStatus status) {
        return projectRepository.findByStatusAndIsDeletedFalse(status)
                .stream()
                .filter(p -> institutionId.equals(p.getInstitutionId()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByInstructor(UUID instructorId) {
        return projectRepository.findByInstructorIdAndIsDeletedFalse(instructorId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Milestones ───────────────────────────────────────────────

    public ProjectMilestoneDTO addMilestone(UUID projectId, ProjectMilestoneDTO dto) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        ProjectMilestone milestone = ProjectMilestone.builder()
                .projectId(projectId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .isCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();
        ProjectMilestone saved = projectMilestoneRepository.save(milestone);
        return toMilestoneDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectMilestoneDTO> getMilestones(UUID projectId) {
        return projectMilestoneRepository.findByProjectIdAndIsDeletedFalse(projectId)
                .stream()
                .map(this::toMilestoneDTO)
                .collect(Collectors.toList());
    }

    public ProjectMilestoneDTO updateMilestone(UUID milestoneId, ProjectMilestoneDTO dto) {
        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", "id", milestoneId));
        milestone.setTitle(dto.getTitle());
        milestone.setDescription(dto.getDescription());
        milestone.setDueDate(dto.getDueDate());
        milestone.setIsCompleted(dto.getIsCompleted());
        if (Boolean.TRUE.equals(dto.getIsCompleted()) && milestone.getCompletedDate() == null) {
            milestone.setCompletedDate(LocalDateTime.now());
        }
        milestone.setSortOrder(dto.getSortOrder());
        ProjectMilestone saved = projectMilestoneRepository.save(milestone);
        return toMilestoneDTO(saved);
    }

    public void completeMilestone(UUID milestoneId) {
        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", "id", milestoneId));
        milestone.setIsCompleted(true);
        milestone.setCompletedDate(LocalDateTime.now());
        projectMilestoneRepository.save(milestone);
    }

    // ── Submissions ──────────────────────────────────────────────

    public ProjectSubmissionDTO addSubmission(UUID projectId, ProjectSubmissionDTO dto) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        if (dto.getMilestoneId() != null) {
            projectMilestoneRepository.findById(dto.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", "id", dto.getMilestoneId()));
        }
        ProjectSubmission submission = ProjectSubmission.builder()
                .projectId(projectId)
                .milestoneId(dto.getMilestoneId())
                .submissionType(dto.getSubmissionType())
                .title(dto.getTitle())
                .fileUrl(dto.getFileUrl())
                .description(dto.getDescription())
                .feedback(dto.getFeedback())
                .grade(dto.getGrade())
                .submittedAt(LocalDateTime.now())
                .build();
        ProjectSubmission saved = projectSubmissionRepository.save(submission);
        return toSubmissionDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectSubmissionDTO> getSubmissions(UUID projectId) {
        return projectSubmissionRepository.findByProjectIdAndIsDeletedFalse(projectId)
                .stream()
                .map(this::toSubmissionDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectSubmissionDTO> getMilestoneSubmissions(UUID milestoneId) {
        return projectSubmissionRepository.findByMilestoneIdAndIsDeletedFalse(milestoneId)
                .stream()
                .map(this::toSubmissionDTO)
                .collect(Collectors.toList());
    }

    public ProjectSubmissionDTO gradeSubmission(UUID submissionId, String feedback, String grade) {
        ProjectSubmission submission = projectSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectSubmission", "id", submissionId));
        submission.setFeedback(feedback);
        submission.setGrade(grade);
        ProjectSubmission saved = projectSubmissionRepository.save(submission);
        return toSubmissionDTO(saved);
    }

    // ── Mapping helpers ──────────────────────────────────────────

    private ProjectDTO toDTO(Project project) {
        return ProjectDTO.builder()
                .id(project.getId())
                .institutionId(project.getInstitutionId())
                .studentId(project.getStudentId())
                .subjectId(project.getSubjectId())
                .title(project.getTitle())
                .objective(project.getObjective())
                .description(project.getDescription())
                .status(project.getStatus())
                .instructorId(project.getInstructorId())
                .startDate(project.getStartDate())
                .dueDate(project.getDueDate())
                .completedDate(project.getCompletedDate())
                .build();
    }

    private ProjectMilestoneDTO toMilestoneDTO(ProjectMilestone milestone) {
        return ProjectMilestoneDTO.builder()
                .id(milestone.getId())
                .projectId(milestone.getProjectId())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .dueDate(milestone.getDueDate())
                .isCompleted(milestone.getIsCompleted())
                .completedDate(milestone.getCompletedDate())
                .sortOrder(milestone.getSortOrder())
                .build();
    }

    private ProjectSubmissionDTO toSubmissionDTO(ProjectSubmission submission) {
        return ProjectSubmissionDTO.builder()
                .id(submission.getId())
                .projectId(submission.getProjectId())
                .milestoneId(submission.getMilestoneId())
                .submissionType(submission.getSubmissionType())
                .title(submission.getTitle())
                .fileUrl(submission.getFileUrl())
                .description(submission.getDescription())
                .feedback(submission.getFeedback())
                .grade(submission.getGrade())
                .submittedAt(submission.getSubmittedAt())
                .build();
    }
}
