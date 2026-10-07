package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.Department;
import tz.elmkusoma.highereducation.domain.Programme;
import tz.elmkusoma.highereducation.domain.ProgrammeType;
import tz.elmkusoma.highereducation.dto.ProgrammeDTO;
import tz.elmkusoma.highereducation.repository.ProgrammeRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProgrammeService {

    private final ProgrammeRepository programmeRepository;
    private final tz.elmkusoma.highereducation.repository.DepartmentRepository departmentRepository;

    public ProgrammeDTO create(UUID institutionId, ProgrammeDTO request) {
        assertDepartmentInInstitution(request.getDepartmentId(), institutionId);
        Programme programme = Programme.builder()
                .institutionId(institutionId)
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .programmeType(request.getProgrammeType())
                .educationLevel(request.getEducationLevel())
                .durationMonths(request.getDurationMonths())
                .departmentId(request.getDepartmentId())
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

    /** Department -> Programme: the real relationship, filtered inside the institution. */
    @Transactional(readOnly = true)
    public List<ProgrammeDTO> listByDepartment(UUID institutionId, UUID departmentId) {
        assertDepartmentInInstitution(departmentId, institutionId);
        return programmeRepository.findByInstitutionIdAndDepartmentIdAndIsDeletedFalse(institutionId, departmentId)
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
        if (request.getDepartmentId() != null
                || !Objects.equals(programme.getDepartmentId(), request.getDepartmentId())) {
            assertDepartmentInInstitution(request.getDepartmentId(), programme.getInstitutionId());
            programme.setDepartmentId(request.getDepartmentId());
        }
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
                .durationDisplay(formatDuration(programme.getDurationMonths()))
                .departmentId(programme.getDepartmentId())
                .departmentName(programme.getDepartmentId() == null ? null
                        : departmentRepository.findById(programme.getDepartmentId())
                        .map(tz.elmkusoma.highereducation.domain.Department::getName)
                        .orElse(null))
                .isActive(programme.getIsActive())
                .build();
    }

    /**
     * Programme duration is expressed in months; present it the way an applicant reads it:
     * 12 -> "1 year", 18 -> "1.5 years", 24 -> "2 years", 36 -> "3 years". Durations under a
     * year stay in months ("9 months") rather than becoming a confusing fraction of a year.
     */
    public static String formatDuration(Integer durationMonths) {
        if (durationMonths == null || durationMonths <= 0) {
            return null;
        }
        if (durationMonths < 12) {
            return durationMonths + (durationMonths == 1 ? " month" : " months");
        }
        if (durationMonths % 12 == 0) {
            int years = durationMonths / 12;
            return years + (years == 1 ? " year" : " years");
        }
        double years = durationMonths / 12.0;
        String formatted = String.valueOf(Math.round(years * 10) / 10.0);
        return formatted + " years";
    }

    /**
     * A programme may only be attached to a department in its own institution. Without this
     * an institution admin could attach their programme to another institution's department.
     */
    private void assertDepartmentInInstitution(UUID departmentId, UUID institutionId) {
        if (departmentId == null) {
            return;
        }
        var department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
        UUID departmentInstitution = department.getInstitutionId();
        if (departmentInstitution != null && institutionId != null
                && !departmentInstitution.equals(institutionId)) {
            throw new ForbiddenException("Department", "access");
        }
    }
}
