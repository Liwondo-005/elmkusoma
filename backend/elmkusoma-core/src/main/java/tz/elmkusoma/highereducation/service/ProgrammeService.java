package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.Programme;
import tz.elmkusoma.highereducation.domain.ProgrammeType;
import tz.elmkusoma.highereducation.dto.ProgrammeDTO;
import tz.elmkusoma.highereducation.repository.ProgrammeRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProgrammeService {

    private final ProgrammeRepository programmeRepository;

    public ProgrammeDTO create(UUID institutionId, ProgrammeDTO request) {
        Programme programme = Programme.builder()
                .institutionId(institutionId)
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .programmeType(request.getProgrammeType())
                .educationLevel(request.getEducationLevel())
                .durationMonths(request.getDurationMonths())
                .creditHours(request.getCreditHours())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Programme saved = programmeRepository.save(programme);
        log.info("Programme created: {} for institution {}", saved.getId(), institutionId);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public ProgrammeDTO getById(UUID id) {
        Programme programme = programmeRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Programme", "id", id));
        return mapToDTO(programme);
    }

    @Transactional(readOnly = true)
    public List<ProgrammeDTO> listByInstitution(UUID institutionId) {
        return programmeRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProgrammeDTO> listByEducationLevel(UUID institutionId, EducationLevel educationLevel) {
        return programmeRepository.findByInstitutionIdAndEducationLevelAndIsDeletedFalse(institutionId, educationLevel)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProgrammeDTO> listByProgrammeType(UUID institutionId, ProgrammeType programmeType) {
        return programmeRepository.findByInstitutionIdAndProgrammeTypeAndIsDeletedFalse(institutionId, programmeType)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countByInstitution(UUID institutionId) {
        return programmeRepository.countByInstitutionIdAndIsDeletedFalse(institutionId);
    }

    public ProgrammeDTO update(UUID id, ProgrammeDTO request) {
        Programme programme = programmeRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Programme", "id", id));

        programme.setName(request.getName());
        programme.setCode(request.getCode());
        programme.setDescription(request.getDescription());
        programme.setProgrammeType(request.getProgrammeType());
        programme.setEducationLevel(request.getEducationLevel());
        programme.setDurationMonths(request.getDurationMonths());
        programme.setCreditHours(request.getCreditHours());
        if (request.getIsActive() != null) {
            programme.setIsActive(request.getIsActive());
        }

        Programme saved = programmeRepository.save(programme);
        log.info("Programme updated: {}", id);
        return mapToDTO(saved);
    }

    public void delete(UUID id) {
        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Programme", "id", id));
        programme.setIsDeleted(true);
        programmeRepository.save(programme);
        log.info("Programme soft-deleted: {}", id);
    }

    private ProgrammeDTO mapToDTO(Programme programme) {
        return ProgrammeDTO.builder()
                .id(programme.getId())
                .name(programme.getName())
                .code(programme.getCode())
                .description(programme.getDescription())
                .programmeType(programme.getProgrammeType())
                .educationLevel(programme.getEducationLevel())
                .durationMonths(programme.getDurationMonths())
                .creditHours(programme.getCreditHours())
                .isActive(programme.getIsActive())
                .build();
    }
}
