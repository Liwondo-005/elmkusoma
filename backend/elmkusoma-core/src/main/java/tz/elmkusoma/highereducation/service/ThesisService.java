package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.Thesis;
import tz.elmkusoma.highereducation.domain.ThesisStatus;
import tz.elmkusoma.highereducation.dto.ThesisDTO;
import tz.elmkusoma.highereducation.repository.ThesisRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ThesisService {

    private final ThesisRepository thesisRepository;

    public ThesisDTO createThesis(ThesisDTO dto) {
        Thesis thesis = Thesis.builder()
                .institutionId(dto.getInstitutionId())
                .studentId(dto.getStudentId())
                .title(dto.getTitle())
                .researchProjectId(dto.getResearchProjectId())
                .supervisorId(dto.getSupervisorId())
                .status(dto.getStatus() != null ? dto.getStatus() : ThesisStatus.NOT_STARTED)
                .programmeId(dto.getProgrammeId())
                .submissionDate(dto.getSubmissionDate())
                .defenseDate(dto.getDefenseDate())
                .finalGrade(dto.getFinalGrade())
                .abstractText(dto.getAbstractText())
                .wordCount(dto.getWordCount())
                .build();
        Thesis saved = thesisRepository.save(thesis);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ThesisDTO> getTheses(UUID institutionId) {
        return thesisRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ThesisDTO getThesis(UUID id) {
        Thesis thesis = thesisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thesis", "id", id));
        return toDTO(thesis);
    }

    public ThesisDTO updateThesis(UUID id, ThesisDTO dto) {
        Thesis thesis = thesisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thesis", "id", id));
        thesis.setStudentId(dto.getStudentId());
        thesis.setTitle(dto.getTitle());
        thesis.setResearchProjectId(dto.getResearchProjectId());
        thesis.setSupervisorId(dto.getSupervisorId());
        thesis.setStatus(dto.getStatus());
        thesis.setProgrammeId(dto.getProgrammeId());
        thesis.setSubmissionDate(dto.getSubmissionDate());
        thesis.setDefenseDate(dto.getDefenseDate());
        thesis.setFinalGrade(dto.getFinalGrade());
        thesis.setAbstractText(dto.getAbstractText());
        thesis.setWordCount(dto.getWordCount());
        Thesis saved = thesisRepository.save(thesis);
        return toDTO(saved);
    }

    public void deleteThesis(UUID id) {
        Thesis thesis = thesisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thesis", "id", id));
        thesis.setIsDeleted(true);
        thesisRepository.save(thesis);
    }

    @Transactional(readOnly = true)
    public List<ThesisDTO> getStudentTheses(UUID studentId, UUID institutionId) {
        return thesisRepository.findByStudentIdAndInstitutionIdAndIsDeletedFalse(studentId, institutionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ThesisDTO> getThesesByStatus(UUID institutionId, ThesisStatus status) {
        return thesisRepository.findByStatusAndIsDeletedFalse(status)
                .stream()
                .filter(t -> institutionId.equals(t.getInstitutionId()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ThesisDTO> getThesesBySupervisor(UUID supervisorId) {
        return thesisRepository.findBySupervisorIdAndIsDeletedFalse(supervisorId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private ThesisDTO toDTO(Thesis thesis) {
        return ThesisDTO.builder()
                .id(thesis.getId())
                .institutionId(thesis.getInstitutionId())
                .studentId(thesis.getStudentId())
                .title(thesis.getTitle())
                .researchProjectId(thesis.getResearchProjectId())
                .supervisorId(thesis.getSupervisorId())
                .status(thesis.getStatus())
                .programmeId(thesis.getProgrammeId())
                .submissionDate(thesis.getSubmissionDate())
                .defenseDate(thesis.getDefenseDate())
                .finalGrade(thesis.getFinalGrade())
                .abstractText(thesis.getAbstractText())
                .wordCount(thesis.getWordCount())
                .build();
    }
}
