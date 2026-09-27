package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.ProfessionalDevelopmentGoal;
import tz.elmkusoma.highereducation.domain.DevGoalType;
import tz.elmkusoma.highereducation.domain.DevGoalStatus;
import tz.elmkusoma.highereducation.dto.ProfessionalDevelopmentGoalDTO;
import tz.elmkusoma.highereducation.repository.ProfessionalDevelopmentGoalRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfessionalDevelopmentGoalService {

    private final ProfessionalDevelopmentGoalRepository goalRepository;

    public ProfessionalDevelopmentGoalDTO createGoal(ProfessionalDevelopmentGoalDTO dto) {
        ProfessionalDevelopmentGoal goal = ProfessionalDevelopmentGoal.builder()
                .studentId(dto.getStudentId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .goalType(DevGoalType.valueOf(
                        dto.getGoalType() != null ? dto.getGoalType() : "SKILL_DEVELOPMENT"))
                .targetDate(dto.getTargetDate())
                .status(DevGoalStatus.NOT_STARTED)
                .progressPercent(0)
                .evidenceUrl(dto.getEvidenceUrl())
                .notes(dto.getNotes())
                .category(dto.getCategory())
                .build();
        goal.setInstitutionId(dto.getInstitutionId() != null ? dto.getInstitutionId() : UUID.randomUUID());
        return toDTO(goalRepository.save(goal));
    }

    public ProfessionalDevelopmentGoalDTO getGoal(UUID id) {
        ProfessionalDevelopmentGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        return toDTO(goal);
    }

    public List<ProfessionalDevelopmentGoalDTO> getStudentGoals(UUID studentId) {
        return goalRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ProfessionalDevelopmentGoalDTO> getStudentGoalsByStatus(UUID studentId, String status) {
        DevGoalStatus enumStatus = DevGoalStatus.valueOf(status); return goalRepository.findByStudentIdAndStatus(studentId, enumStatus)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ProfessionalDevelopmentGoalDTO updateGoal(UUID id, ProfessionalDevelopmentGoalDTO dto) {
        ProfessionalDevelopmentGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        if (dto.getTitle() != null) goal.setTitle(dto.getTitle());
        if (dto.getDescription() != null) goal.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            goal.setStatus(DevGoalStatus.valueOf(dto.getStatus()));
            if ("COMPLETED".equals(dto.getStatus())) goal.setCompletedDate(LocalDate.now());
        }
        if (dto.getProgressPercent() != null) goal.setProgressPercent(dto.getProgressPercent());
        if (dto.getNotes() != null) goal.setNotes(dto.getNotes());
        if (dto.getCategory() != null) goal.setCategory(dto.getCategory());
        if (dto.getTargetDate() != null) goal.setTargetDate(dto.getTargetDate());
        return toDTO(goalRepository.save(goal));
    }

    public void deleteGoal(UUID id) {
        if (!goalRepository.existsById(id))
            throw new ResourceNotFoundException("Goal not found");
        goalRepository.deleteById(id);
    }

    private ProfessionalDevelopmentGoalDTO toDTO(ProfessionalDevelopmentGoal g) {
        return ProfessionalDevelopmentGoalDTO.builder()
                .id(g.getId()).studentId(g.getStudentId()).title(g.getTitle())
                .description(g.getDescription()).goalType(g.getGoalType().name())
                .targetDate(g.getTargetDate()).completedDate(g.getCompletedDate())
                .status(g.getStatus().name()).progressPercent(g.getProgressPercent())
                .evidenceUrl(g.getEvidenceUrl()).notes(g.getNotes()).category(g.getCategory())
                .build();
    }
}
