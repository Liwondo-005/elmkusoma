package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.Department;
import tz.elmkusoma.highereducation.dto.DepartmentDTO;
import tz.elmkusoma.highereducation.repository.DepartmentRepository;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentDTO create(UUID institutionId, DepartmentDTO request) {
        Department department = Department.builder()
                .institutionId(institutionId)
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .programmeIds(request.getProgrammeIds() != null ? new HashSet<>(request.getProgrammeIds()) : new HashSet<>())
                .headOfDepartmentId(request.getHeadOfDepartmentId())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Department saved = departmentRepository.save(department);
        log.info("Department created: {} for institution {}", saved.getId(), institutionId);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public DepartmentDTO getById(UUID id) {
        Department department = departmentRepository.findById(id)
                .filter(d -> !d.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return mapToDTO(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentDTO> listByInstitution(UUID institutionId) {
        return departmentRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countByInstitution(UUID institutionId) {
        return departmentRepository.countByInstitutionIdAndIsDeletedFalse(institutionId);
    }

    public DepartmentDTO update(UUID id, DepartmentDTO request) {
        Department department = departmentRepository.findById(id)
                .filter(d -> !d.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setDescription(request.getDescription());
        if (request.getProgrammeIds() != null) {
            department.setProgrammeIds(new HashSet<>(request.getProgrammeIds()));
        }
        department.setHeadOfDepartmentId(request.getHeadOfDepartmentId());
        if (request.getIsActive() != null) {
            department.setIsActive(request.getIsActive());
        }

        Department saved = departmentRepository.save(department);
        log.info("Department updated: {}", id);
        return mapToDTO(saved);
    }

    public void delete(UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        department.setIsDeleted(true);
        departmentRepository.save(department);
        log.info("Department soft-deleted: {}", id);
    }

    private DepartmentDTO mapToDTO(Department department) {
        return DepartmentDTO.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .description(department.getDescription())
                .programmeIds(department.getProgrammeIds() != null ? new HashSet<>(department.getProgrammeIds()) : new HashSet<>())
                .headOfDepartmentId(department.getHeadOfDepartmentId())
                .isActive(department.getIsActive())
                .build();
    }
}
