package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.*;
import tz.elmkusoma.highereducation.dto.FieldworkPlacementDTO;
import tz.elmkusoma.highereducation.dto.LogbookEntryDTO;
import tz.elmkusoma.highereducation.repository.FieldworkPlacementRepository;
import tz.elmkusoma.highereducation.repository.LogbookEntryRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FieldworkService {

    private final FieldworkPlacementRepository fieldworkPlacementRepository;
    private final LogbookEntryRepository logbookEntryRepository;

    // ── Placement CRUD ──────────────────────────────────────────

    public FieldworkPlacementDTO createPlacement(FieldworkPlacementDTO dto) {
        FieldworkPlacement placement = FieldworkPlacement.builder()
                .institutionId(dto.getInstitutionId())
                .studentId(dto.getStudentId())
                .programmeId(dto.getProgrammeId())
                .organisationName(dto.getOrganisationName())
                .placementTitle(dto.getPlacementTitle())
                .supervisorName(dto.getSupervisorName())
                .supervisorEmail(dto.getSupervisorEmail())
                .supervisorPhone(dto.getSupervisorPhone())
                .institutionSupervisorId(dto.getInstitutionSupervisorId())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus() != null ? dto.getStatus() : PlacementStatus.PLANNING)
                .totalHoursRequired(dto.getTotalHoursRequired())
                .totalHoursCompleted(dto.getTotalHoursCompleted() != null ? dto.getTotalHoursCompleted() : 0)
                .objectives(dto.getObjectives())
                .remarks(dto.getRemarks())
                .build();
        FieldworkPlacement saved = fieldworkPlacementRepository.save(placement);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<FieldworkPlacementDTO> getPlacements(UUID institutionId) {
        return fieldworkPlacementRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FieldworkPlacementDTO getPlacement(UUID id) {
        FieldworkPlacement placement = fieldworkPlacementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FieldworkPlacement", "id", id));
        return toDTO(placement);
    }

    public FieldworkPlacementDTO updatePlacement(UUID id, FieldworkPlacementDTO dto) {
        FieldworkPlacement placement = fieldworkPlacementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FieldworkPlacement", "id", id));
        placement.setStudentId(dto.getStudentId());
        placement.setProgrammeId(dto.getProgrammeId());
        placement.setOrganisationName(dto.getOrganisationName());
        placement.setPlacementTitle(dto.getPlacementTitle());
        placement.setSupervisorName(dto.getSupervisorName());
        placement.setSupervisorEmail(dto.getSupervisorEmail());
        placement.setSupervisorPhone(dto.getSupervisorPhone());
        placement.setInstitutionSupervisorId(dto.getInstitutionSupervisorId());
        placement.setStartDate(dto.getStartDate());
        placement.setEndDate(dto.getEndDate());
        placement.setStatus(dto.getStatus());
        placement.setTotalHoursRequired(dto.getTotalHoursRequired());
        placement.setTotalHoursCompleted(dto.getTotalHoursCompleted());
        placement.setObjectives(dto.getObjectives());
        placement.setRemarks(dto.getRemarks());
        FieldworkPlacement saved = fieldworkPlacementRepository.save(placement);
        return toDTO(saved);
    }

    public void deletePlacement(UUID id) {
        FieldworkPlacement placement = fieldworkPlacementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FieldworkPlacement", "id", id));
        placement.setIsDeleted(true);
        fieldworkPlacementRepository.save(placement);
    }

    @Transactional(readOnly = true)
    public List<FieldworkPlacementDTO> getStudentPlacements(UUID studentId, UUID institutionId) {
        return fieldworkPlacementRepository.findByStudentIdAndInstitutionIdAndIsDeletedFalse(studentId, institutionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FieldworkPlacementDTO> getPlacementsByStatus(UUID institutionId, PlacementStatus status) {
        return fieldworkPlacementRepository.findByStatusAndIsDeletedFalse(status)
                .stream()
                .filter(p -> institutionId.equals(p.getInstitutionId()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FieldworkPlacementDTO> getPlacementsByInstructor(UUID institutionSupervisorId) {
        return fieldworkPlacementRepository.findByInstitutionSupervisorIdAndIsDeletedFalse(institutionSupervisorId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public FieldworkPlacementDTO completePlacement(UUID id) {
        FieldworkPlacement placement = fieldworkPlacementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FieldworkPlacement", "id", id));
        placement.setStatus(PlacementStatus.COMPLETED);
        placement.setEndDate(LocalDate.now());
        FieldworkPlacement saved = fieldworkPlacementRepository.save(placement);
        return toDTO(saved);
    }

    // ── Logbook Entries ─────────────────────────────────────────

    public LogbookEntryDTO addLogbookEntry(UUID placementId, LogbookEntryDTO dto) {
        fieldworkPlacementRepository.findById(placementId)
                .orElseThrow(() -> new ResourceNotFoundException("FieldworkPlacement", "id", placementId));
        LogbookEntry entry = LogbookEntry.builder()
                .placementId(placementId)
                .entryDate(dto.getEntryDate())
                .activities(dto.getActivities())
                .hoursWorked(dto.getHoursWorked())
                .skillsUsed(dto.getSkillsUsed())
                .challenges(dto.getChallenges())
                .learningOutcomes(dto.getLearningOutcomes())
                .supervisorComments(dto.getSupervisorComments())
                .isApproved(false)
                .build();
        LogbookEntry saved = logbookEntryRepository.save(entry);
        updatePlacementHours(placementId);
        return toLogbookDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<LogbookEntryDTO> getLogbookEntries(UUID placementId) {
        return logbookEntryRepository.findByPlacementIdAndIsDeletedFalse(placementId)
                .stream()
                .map(this::toLogbookDTO)
                .collect(Collectors.toList());
    }

    public LogbookEntryDTO updateLogbookEntry(UUID entryId, LogbookEntryDTO dto) {
        LogbookEntry entry = logbookEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("LogbookEntry", "id", entryId));
        entry.setEntryDate(dto.getEntryDate());
        entry.setActivities(dto.getActivities());
        entry.setHoursWorked(dto.getHoursWorked());
        entry.setSkillsUsed(dto.getSkillsUsed());
        entry.setChallenges(dto.getChallenges());
        entry.setLearningOutcomes(dto.getLearningOutcomes());
        entry.setSupervisorComments(dto.getSupervisorComments());
        LogbookEntry saved = logbookEntryRepository.save(entry);
        updatePlacementHours(entry.getPlacementId());
        return toLogbookDTO(saved);
    }

    public LogbookEntryDTO approveLogbookEntry(UUID entryId, UUID approvedBy) {
        LogbookEntry entry = logbookEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("LogbookEntry", "id", entryId));
        entry.setIsApproved(true);
        entry.setApprovedBy(approvedBy);
        entry.setApprovedAt(LocalDateTime.now());
        LogbookEntry saved = logbookEntryRepository.save(entry);
        return toLogbookDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<LogbookEntryDTO> getPendingLogbookEntries(UUID placementId) {
        return logbookEntryRepository.findByPlacementIdAndIsApprovedFalseAndIsDeletedFalse(placementId)
                .stream()
                .map(this::toLogbookDTO)
                .collect(Collectors.toList());
    }

    private void updatePlacementHours(UUID placementId) {
        FieldworkPlacement placement = fieldworkPlacementRepository.findById(placementId)
                .orElseThrow(() -> new ResourceNotFoundException("FieldworkPlacement", "id", placementId));
        List<LogbookEntry> entries = logbookEntryRepository.findByPlacementIdAndIsDeletedFalse(placementId);
        double totalHours = entries.stream()
                .filter(e -> e.getHoursWorked() != null)
                .mapToDouble(LogbookEntry::getHoursWorked)
                .sum();
        placement.setTotalHoursCompleted((int) Math.round(totalHours));
        fieldworkPlacementRepository.save(placement);
    }

    // ── Mapping helpers ──────────────────────────────────────────

    private FieldworkPlacementDTO toDTO(FieldworkPlacement placement) {
        return FieldworkPlacementDTO.builder()
                .id(placement.getId())
                .institutionId(placement.getInstitutionId())
                .studentId(placement.getStudentId())
                .programmeId(placement.getProgrammeId())
                .organisationName(placement.getOrganisationName())
                .placementTitle(placement.getPlacementTitle())
                .supervisorName(placement.getSupervisorName())
                .supervisorEmail(placement.getSupervisorEmail())
                .supervisorPhone(placement.getSupervisorPhone())
                .institutionSupervisorId(placement.getInstitutionSupervisorId())
                .startDate(placement.getStartDate())
                .endDate(placement.getEndDate())
                .status(placement.getStatus())
                .totalHoursRequired(placement.getTotalHoursRequired())
                .totalHoursCompleted(placement.getTotalHoursCompleted())
                .objectives(placement.getObjectives())
                .remarks(placement.getRemarks())
                .build();
    }

    private LogbookEntryDTO toLogbookDTO(LogbookEntry entry) {
        return LogbookEntryDTO.builder()
                .id(entry.getId())
                .placementId(entry.getPlacementId())
                .entryDate(entry.getEntryDate())
                .activities(entry.getActivities())
                .hoursWorked(entry.getHoursWorked())
                .skillsUsed(entry.getSkillsUsed())
                .challenges(entry.getChallenges())
                .learningOutcomes(entry.getLearningOutcomes())
                .supervisorComments(entry.getSupervisorComments())
                .isApproved(entry.getIsApproved())
                .approvedBy(entry.getApprovedBy())
                .approvedAt(entry.getApprovedAt())
                .build();
    }
}
