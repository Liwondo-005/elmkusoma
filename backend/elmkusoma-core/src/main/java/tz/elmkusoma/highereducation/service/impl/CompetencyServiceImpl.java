package tz.elmkusoma.highereducation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.*;
import tz.elmkusoma.highereducation.dto.*;
import tz.elmkusoma.highereducation.repository.*;
import tz.elmkusoma.highereducation.service.CompetencyService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CompetencyServiceImpl implements CompetencyService {

    private final CompetencyRepository competencyRepository;
    private final CompetencyRecordRepository competencyRecordRepository;
    private final CompetencyAssessmentRepository competencyAssessmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CompetencyDTO> getAllCompetencies(UUID institutionId) {
        return competencyRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::toCompetencyDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompetencyDTO getCompetencyById(UUID id, UUID institutionId) {
        Competency competency = competencyRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .filter(c -> c.getInstitutionId().equals(institutionId))
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", id));
        return toCompetencyDTO(competency);
    }

    @Override
    public CompetencyDTO createCompetency(UUID institutionId, CompetencyDTO dto) {
        Competency competency = Competency.builder()
                .institutionId(institutionId)
                .name(dto.getName())
                .code(dto.getCode())
                .description(dto.getDescription())
                .competencyType(CompetencyType.valueOf(dto.getCompetencyType()))
                .programmeId(dto.getProgrammeId())
                .subjectId(dto.getSubjectId())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        return toCompetencyDTO(competencyRepository.save(competency));
    }

    @Override
    public CompetencyDTO updateCompetency(UUID id, UUID institutionId, CompetencyDTO dto) {
        Competency competency = competencyRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .filter(c -> c.getInstitutionId().equals(institutionId))
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", id));

        competency.setName(dto.getName());
        competency.setCode(dto.getCode());
        competency.setDescription(dto.getDescription());
        competency.setCompetencyType(CompetencyType.valueOf(dto.getCompetencyType()));
        competency.setProgrammeId(dto.getProgrammeId());
        competency.setSubjectId(dto.getSubjectId());
        competency.setSortOrder(dto.getSortOrder());
        competency.setIsActive(dto.getIsActive());

        return toCompetencyDTO(competencyRepository.save(competency));
    }

    @Override
    public void deleteCompetency(UUID id, UUID institutionId) {
        Competency competency = competencyRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .filter(c -> c.getInstitutionId().equals(institutionId))
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", id));

        competency.setIsDeleted(true);
        competencyRepository.save(competency);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentCompetencyDTO> getStudentCompetencies(UUID studentId, UUID institutionId) {
        List<Competency> competencies = competencyRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
        List<UUID> competencyIds = competencies.stream().map(Competency::getId).toList();

        List<CompetencyRecord> records = competencyRecordRepository
                .findByStudentIdAndCompetencyIdInAndIsDeletedFalse(studentId, competencyIds);

        Map<UUID, CompetencyRecord> recordMap = records.stream()
                .collect(Collectors.toMap(CompetencyRecord::getCompetencyId, r -> r));

        return competencies.stream().map(competency -> {
            CompetencyRecord record = recordMap.get(competency.getId());
            StudentCompetencyDTO.StudentCompetencyDTOBuilder builder = StudentCompetencyDTO.builder()
                    .competencyId(competency.getId())
                    .name(competency.getName())
                    .code(competency.getCode())
                    .description(competency.getDescription())
                    .competencyType(competency.getCompetencyType().name())
                    .subjectId(competency.getSubjectId())
                    .sortOrder(competency.getSortOrder());

            if (record != null) {
                builder.status(record.getStatus().name())
                        .evidence(record.getEvidence())
                        .assessedBy(record.getAssessedBy())
                        .assessmentDate(record.getAssessmentDate() != null ? record.getAssessmentDate().toString() : null)
                        .lastPracticeDate(record.getLastPracticeDate() != null ? record.getLastPracticeDate().toString() : null)
                        .notes(record.getNotes());
            } else {
                builder.status(CompetencyStatus.NOT_STARTED.name());
            }

            return builder.build();
        }).toList();
    }

    @Override
    public CompetencyRecordDTO updateCompetencyRecord(UUID studentId, UUID competencyId, String status, String evidence, UUID assessedBy) {
        Competency competency = competencyRepository.findById(competencyId)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", competencyId));

        CompetencyRecord record = competencyRecordRepository
                .findByStudentIdAndCompetencyIdAndIsDeletedFalse(studentId, competencyId)
                .orElse(null);

        CompetencyStatus newStatus = CompetencyStatus.valueOf(status);

        if (record == null) {
            record = CompetencyRecord.builder()
                    .institutionId(competency.getInstitutionId())
                    .studentId(studentId)
                    .competencyId(competencyId)
                    .status(newStatus)
                    .evidence(evidence)
                    .assessedBy(assessedBy)
                    .assessmentDate(newStatus == CompetencyStatus.ASSESSED || newStatus == CompetencyStatus.COMPETENT ? LocalDate.now() : null)
                    .build();
        } else {
            record.setStatus(newStatus);
            record.setEvidence(evidence);
            if (assessedBy != null) {
                record.setAssessedBy(assessedBy);
            }
            if (newStatus == CompetencyStatus.ASSESSED || newStatus == CompetencyStatus.COMPETENT) {
                record.setAssessmentDate(LocalDate.now());
            }
            if (newStatus == CompetencyStatus.PRACTICING || newStatus == CompetencyStatus.LEARNING) {
                record.setLastPracticeDate(LocalDate.now());
            }
        }

        return toCompetencyRecordDTO(competencyRecordRepository.save(record), competency);
    }

    @Override
    @Transactional(readOnly = true)
    public CompetencySummaryDTO getCompetencySummary(UUID studentId, UUID institutionId) {
        List<Object[]> statusCounts = competencyRecordRepository.countByStatusForStudent(studentId);

        long totalCompetencies = competencyRepository
                .findByInstitutionIdAndIsDeletedFalse(institutionId).size();

        Map<String, Long> countMap = statusCounts.stream()
                .collect(Collectors.toMap(
                        obj -> ((CompetencyStatus) obj[0]).name(),
                        obj -> (Long) obj[1]
                ));

        return CompetencySummaryDTO.builder()
                .totalCompetencies(totalCompetencies)
                .notStarted(countMap.getOrDefault("NOT_STARTED", 0L))
                .learning(countMap.getOrDefault("LEARNING", 0L))
                .practicing(countMap.getOrDefault("PRACTICING", 0L))
                .assessed(countMap.getOrDefault("ASSESSED", 0L))
                .competent(countMap.getOrDefault("COMPETENT", 0L))
                .needsPractice(countMap.getOrDefault("NEEDS_PRACTICE", 0L))
                .completed(countMap.getOrDefault("COMPLETED", 0L))
                .build();
    }

    @Override
    public CompetencyAssessmentDTO linkAssessmentToCompetency(UUID competencyId, UUID assessmentId, Integer weight) {
        Competency competency = competencyRepository.findById(competencyId)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", competencyId));

        boolean exists = competencyAssessmentRepository
                .existsByCompetencyIdAndAssessmentIdAndIsDeletedFalse(competencyId, assessmentId);
        if (exists) {
            throw new IllegalArgumentException("Assessment already linked to this competency");
        }

        CompetencyAssessment ca = CompetencyAssessment.builder()
                .institutionId(competency.getInstitutionId())
                .competencyId(competencyId)
                .assessmentId(assessmentId)
                .weight(weight != null ? weight : 100)
                .build();

        return toCompetencyAssessmentDTO(competencyAssessmentRepository.save(ca));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetencyDTO> getCompetenciesForSubject(UUID subjectId) {
        return competencyRepository.findBySubjectIdAndIsDeletedFalse(subjectId)
                .stream()
                .map(this::toCompetencyDTO)
                .toList();
    }

    private CompetencyDTO toCompetencyDTO(Competency competency) {
        return CompetencyDTO.builder()
                .id(competency.getId())
                .name(competency.getName())
                .code(competency.getCode())
                .description(competency.getDescription())
                .competencyType(competency.getCompetencyType().name())
                .programmeId(competency.getProgrammeId())
                .subjectId(competency.getSubjectId())
                .sortOrder(competency.getSortOrder())
                .isActive(competency.getIsActive())
                .build();
    }

    private CompetencyRecordDTO toCompetencyRecordDTO(CompetencyRecord record, Competency competency) {
        return CompetencyRecordDTO.builder()
                .id(record.getId())
                .studentId(record.getStudentId())
                .competencyId(record.getCompetencyId())
                .competencyName(competency.getName())
                .competencyCode(competency.getCode())
                .competencyType(competency.getCompetencyType().name())
                .status(record.getStatus().name())
                .evidence(record.getEvidence())
                .assessedBy(record.getAssessedBy())
                .assessmentDate(record.getAssessmentDate())
                .lastPracticeDate(record.getLastPracticeDate())
                .notes(record.getNotes())
                .build();
    }

    private CompetencyAssessmentDTO toCompetencyAssessmentDTO(CompetencyAssessment ca) {
        return CompetencyAssessmentDTO.builder()
                .id(ca.getId())
                .competencyId(ca.getCompetencyId())
                .assessmentId(ca.getAssessmentId())
                .weight(ca.getWeight())
                .build();
    }
}
