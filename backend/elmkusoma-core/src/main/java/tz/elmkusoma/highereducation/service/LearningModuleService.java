package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.LearningModule;
import tz.elmkusoma.highereducation.dto.LearningModuleDTO;
import tz.elmkusoma.highereducation.repository.LearningModuleRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningModuleService {

    private final LearningModuleRepository learningModuleRepository;

    public LearningModuleDTO createModule(LearningModuleDTO dto) {
        LearningModule module = LearningModule.builder()
                .studentId(dto.getStudentId())
                .courseId(dto.getCourseId())
                .moduleTitle(dto.getModuleTitle())
                .moduleCode(dto.getModuleCode())
                .description(dto.getDescription())
                .creditHours(dto.getCreditHours())
                .instructorId(dto.getInstructorId())
                .semester(dto.getSemester())
                .academicYear(dto.getAcademicYear())
                .status(tz.elmkusoma.highereducation.domain.ModuleStatus.valueOf(
                        dto.getStatus() != null ? dto.getStatus() : "NOT_STARTED"))
                .progressPercent(dto.getProgressPercent() != null ? dto.getProgressPercent() : 0)
                .grade(dto.getGrade())
                .totalLessons(dto.getTotalLessons())
                .completedLessons(dto.getCompletedLessons() != null ? dto.getCompletedLessons() : 0)
                .totalAssignments(dto.getTotalAssignments())
                .completedAssignments(dto.getCompletedAssignments() != null ? dto.getCompletedAssignments() : 0)
                .totalAssessments(dto.getTotalAssessments())
                .completedAssessments(dto.getCompletedAssessments() != null ? dto.getCompletedAssessments() : 0)
                .institutionId(dto.getInstitutionId() != null ? dto.getInstitutionId() : UUID.randomUUID())
                .build();
        return toDTO(learningModuleRepository.save(module));
    }

    public LearningModuleDTO getModule(UUID id) {
        LearningModule module = learningModuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
        return toDTO(module);
    }

    public List<LearningModuleDTO> getStudentModules(UUID studentId) {
        return learningModuleRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<LearningModuleDTO> getStudentCourseModules(UUID studentId, UUID courseId) {
        return learningModuleRepository.findByStudentIdAndCourseId(studentId, courseId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<LearningModuleDTO> getInstitutionModules(UUID institutionId) {
        return learningModuleRepository.findByInstitutionId(institutionId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public LearningModuleDTO updateModule(UUID id, LearningModuleDTO dto) {
        LearningModule module = learningModuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
        if (dto.getModuleTitle() != null) module.setModuleTitle(dto.getModuleTitle());
        if (dto.getDescription() != null) module.setDescription(dto.getDescription());
        if (dto.getStatus() != null) module.setStatus(
                tz.elmkusoma.highereducation.domain.ModuleStatus.valueOf(dto.getStatus()));
        if (dto.getProgressPercent() != null) module.setProgressPercent(dto.getProgressPercent());
        if (dto.getGrade() != null) module.setGrade(dto.getGrade());
        if (dto.getCompletedLessons() != null) module.setCompletedLessons(dto.getCompletedLessons());
        if (dto.getCompletedAssignments() != null) module.setCompletedAssignments(dto.getCompletedAssignments());
        if (dto.getCompletedAssessments() != null) module.setCompletedAssessments(dto.getCompletedAssessments());
        return toDTO(learningModuleRepository.save(module));
    }

    public void deleteModule(UUID id) {
        if (!learningModuleRepository.existsById(id))
            throw new ResourceNotFoundException("Module not found");
        learningModuleRepository.deleteById(id);
    }

    private LearningModuleDTO toDTO(LearningModule m) {
        return LearningModuleDTO.builder()
                .id(m.getId()).studentId(m.getStudentId()).courseId(m.getCourseId())
                .moduleTitle(m.getModuleTitle()).moduleCode(m.getModuleCode())
                .description(m.getDescription()).creditHours(m.getCreditHours())
                .instructorId(m.getInstructorId()).semester(m.getSemester())
                .academicYear(m.getAcademicYear()).status(m.getStatus().name())
                .progressPercent(m.getProgressPercent()).grade(m.getGrade())
                .totalLessons(m.getTotalLessons()).completedLessons(m.getCompletedLessons())
                .totalAssignments(m.getTotalAssignments()).completedAssignments(m.getCompletedAssignments())
                .totalAssessments(m.getTotalAssessments()).completedAssessments(m.getCompletedAssessments())
                .createdAt(m.getCreatedAt()).updatedAt(m.getUpdatedAt())
                .build();
    }
}
