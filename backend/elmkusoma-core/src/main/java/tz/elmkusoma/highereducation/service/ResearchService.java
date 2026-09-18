package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.*;
import tz.elmkusoma.highereducation.dto.ResearchMilestoneDTO;
import tz.elmkusoma.highereducation.dto.ResearchProjectDTO;
import tz.elmkusoma.highereducation.dto.ResearchResourceDTO;

import java.time.LocalDate;
import tz.elmkusoma.highereducation.repository.ResearchMilestoneRepository;
import tz.elmkusoma.highereducation.repository.ResearchProjectRepository;
import tz.elmkusoma.highereducation.repository.ResearchResourceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ResearchService {

    private final ResearchProjectRepository researchProjectRepository;
    private final ResearchMilestoneRepository researchMilestoneRepository;
    private final ResearchResourceRepository researchResourceRepository;

    // ── Research Project CRUD ────────────────────────────────────

    public ResearchProjectDTO createResearchProject(ResearchProjectDTO dto) {
        ResearchProject project = ResearchProject.builder()
                .institutionId(dto.getInstitutionId())
                .studentId(dto.getStudentId())
                .title(dto.getTitle())
                .researchQuestion(dto.getResearchQuestion())
                .objectives(dto.getObjectives())
                .supervisorId(dto.getSupervisorId())
                .status(dto.getStatus() != null ? dto.getStatus() : ResearchStatus.IDEA)
                .programmeId(dto.getProgrammeId())
                .subjectId(dto.getSubjectId())
                .methodology(dto.getMethodology())
                .startDate(dto.getStartDate())
                .dueDate(dto.getDueDate())
                .completedDate(dto.getCompletedDate())
                .abstractText(dto.getAbstractText())
                .keywords(dto.getKeywords())
                .build();
        ResearchProject saved = researchProjectRepository.save(project);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ResearchProjectDTO> getResearchProjects(UUID institutionId) {
        return researchProjectRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResearchProjectDTO getResearchProject(UUID id) {
        ResearchProject project = researchProjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchProject", "id", id));
        return toDTO(project);
    }

    public ResearchProjectDTO updateResearchProject(UUID id, ResearchProjectDTO dto) {
        ResearchProject project = researchProjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchProject", "id", id));
        project.setStudentId(dto.getStudentId());
        project.setTitle(dto.getTitle());
        project.setResearchQuestion(dto.getResearchQuestion());
        project.setObjectives(dto.getObjectives());
        project.setSupervisorId(dto.getSupervisorId());
        project.setStatus(dto.getStatus());
        project.setProgrammeId(dto.getProgrammeId());
        project.setSubjectId(dto.getSubjectId());
        project.setMethodology(dto.getMethodology());
        project.setStartDate(dto.getStartDate());
        project.setDueDate(dto.getDueDate());
        project.setCompletedDate(dto.getCompletedDate());
        project.setAbstractText(dto.getAbstractText());
        project.setKeywords(dto.getKeywords());
        ResearchProject saved = researchProjectRepository.save(project);
        return toDTO(saved);
    }

    public void deleteResearchProject(UUID id) {
        ResearchProject project = researchProjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchProject", "id", id));
        project.setIsDeleted(true);
        researchProjectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<ResearchProjectDTO> getStudentResearchProjects(UUID studentId, UUID institutionId) {
        return researchProjectRepository.findByStudentIdAndInstitutionIdAndIsDeletedFalse(studentId, institutionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResearchProjectDTO> getResearchProjectsByStatus(UUID institutionId, ResearchStatus status) {
        return researchProjectRepository.findByStatusAndIsDeletedFalse(status)
                .stream()
                .filter(p -> institutionId.equals(p.getInstitutionId()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResearchProjectDTO> getResearchProjectsBySupervisor(UUID supervisorId) {
        return researchProjectRepository.findBySupervisorIdAndIsDeletedFalse(supervisorId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Milestones ───────────────────────────────────────────────

    public ResearchMilestoneDTO addMilestone(UUID researchProjectId, ResearchMilestoneDTO dto) {
        researchProjectRepository.findById(researchProjectId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchProject", "id", researchProjectId));
        ResearchMilestone milestone = ResearchMilestone.builder()
                .researchProjectId(researchProjectId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .isCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();
        ResearchMilestone saved = researchMilestoneRepository.save(milestone);
        return toMilestoneDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ResearchMilestoneDTO> getMilestones(UUID researchProjectId) {
        return researchMilestoneRepository.findByResearchProjectIdAndIsDeletedFalse(researchProjectId)
                .stream()
                .map(this::toMilestoneDTO)
                .collect(Collectors.toList());
    }

    public ResearchMilestoneDTO updateMilestone(UUID milestoneId, ResearchMilestoneDTO dto) {
        ResearchMilestone milestone = researchMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchMilestone", "id", milestoneId));
        milestone.setTitle(dto.getTitle());
        milestone.setDescription(dto.getDescription());
        milestone.setDueDate(dto.getDueDate());
        milestone.setIsCompleted(dto.getIsCompleted());
        if (Boolean.TRUE.equals(dto.getIsCompleted()) && milestone.getCompletedDate() == null) {
        milestone.setCompletedDate(LocalDate.now());
        }
        milestone.setSortOrder(dto.getSortOrder());
        ResearchMilestone saved = researchMilestoneRepository.save(milestone);
        return toMilestoneDTO(saved);
    }

    public void completeMilestone(UUID milestoneId) {
        ResearchMilestone milestone = researchMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchMilestone", "id", milestoneId));
        milestone.setIsCompleted(true);
        milestone.setCompletedDate(LocalDate.now());
        researchMilestoneRepository.save(milestone);
    }

    // ── Resources ────────────────────────────────────────────────

    public ResearchResourceDTO addResource(UUID researchProjectId, ResearchResourceDTO dto) {
        researchProjectRepository.findById(researchProjectId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchProject", "id", researchProjectId));
        ResearchResource resource = ResearchResource.builder()
                .researchProjectId(researchProjectId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .resourceType(dto.getResourceType())
                .fileUrl(dto.getFileUrl())
                .citation(dto.getCitation())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();
        ResearchResource saved = researchResourceRepository.save(resource);
        return toResourceDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ResearchResourceDTO> getResources(UUID researchProjectId) {
        return researchResourceRepository.findByResearchProjectIdAndIsDeletedFalse(researchProjectId)
                .stream()
                .map(this::toResourceDTO)
                .collect(Collectors.toList());
    }

    public ResearchResourceDTO updateResource(UUID resourceId, ResearchResourceDTO dto) {
        ResearchResource resource = researchResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchResource", "id", resourceId));
        resource.setTitle(dto.getTitle());
        resource.setDescription(dto.getDescription());
        resource.setResourceType(dto.getResourceType());
        resource.setFileUrl(dto.getFileUrl());
        resource.setCitation(dto.getCitation());
        resource.setSortOrder(dto.getSortOrder());
        ResearchResource saved = researchResourceRepository.save(resource);
        return toResourceDTO(saved);
    }

    public void deleteResource(UUID resourceId) {
        ResearchResource resource = researchResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchResource", "id", resourceId));
        resource.setIsDeleted(true);
        researchResourceRepository.save(resource);
    }

    // ── Mapping helpers ──────────────────────────────────────────

    private ResearchProjectDTO toDTO(ResearchProject project) {
        return ResearchProjectDTO.builder()
                .id(project.getId())
                .institutionId(project.getInstitutionId())
                .studentId(project.getStudentId())
                .title(project.getTitle())
                .researchQuestion(project.getResearchQuestion())
                .objectives(project.getObjectives())
                .supervisorId(project.getSupervisorId())
                .status(project.getStatus())
                .programmeId(project.getProgrammeId())
                .subjectId(project.getSubjectId())
                .methodology(project.getMethodology())
                .startDate(project.getStartDate())
                .dueDate(project.getDueDate())
                .completedDate(project.getCompletedDate())
                .abstractText(project.getAbstractText())
                .keywords(project.getKeywords())
                .build();
    }

    private ResearchMilestoneDTO toMilestoneDTO(ResearchMilestone milestone) {
        return ResearchMilestoneDTO.builder()
                .id(milestone.getId())
                .researchProjectId(milestone.getResearchProjectId())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .dueDate(milestone.getDueDate())
                .isCompleted(milestone.getIsCompleted())
                .completedDate(milestone.getCompletedDate())
                .sortOrder(milestone.getSortOrder())
                .build();
    }

    private ResearchResourceDTO toResourceDTO(ResearchResource resource) {
        return ResearchResourceDTO.builder()
                .id(resource.getId())
                .researchProjectId(resource.getResearchProjectId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .resourceType(resource.getResourceType())
                .fileUrl(resource.getFileUrl())
                .citation(resource.getCitation())
                .sortOrder(resource.getSortOrder())
                .build();
    }
}
