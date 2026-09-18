package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.DemonstrationStatus;
import tz.elmkusoma.highereducation.domain.PracticalDemonstration;
import tz.elmkusoma.highereducation.dto.DemonstrationDTO;
import tz.elmkusoma.highereducation.repository.DemonstrationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DemonstrationService {

    private final DemonstrationRepository demonstrationRepository;

    // ── CRUD ─────────────────────────────────────────────────────

    public DemonstrationDTO createDemonstration(DemonstrationDTO dto) {
        PracticalDemonstration demonstration = PracticalDemonstration.builder()
                .institutionId(dto.getInstitutionId())
                .studentId(dto.getStudentId())
                .competencyId(dto.getCompetencyId())
                .projectId(dto.getProjectId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .mediaUrls(dto.getMediaUrls())
                .status(dto.getStatus() != null ? dto.getStatus() : DemonstrationStatus.DRAFT)
                .reviewerId(dto.getReviewerId())
                .reviewNotes(dto.getReviewNotes())
                .score(dto.getScore())
                .build();
        PracticalDemonstration saved = demonstrationRepository.save(demonstration);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<DemonstrationDTO> getDemonstrations(UUID institutionId) {
        return demonstrationRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DemonstrationDTO getDemonstration(UUID id) {
        PracticalDemonstration demonstration = demonstrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PracticalDemonstration", "id", id));
        return toDTO(demonstration);
    }

    public DemonstrationDTO updateDemonstration(UUID id, DemonstrationDTO dto) {
        PracticalDemonstration demonstration = demonstrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PracticalDemonstration", "id", id));
        demonstration.setStudentId(dto.getStudentId());
        demonstration.setCompetencyId(dto.getCompetencyId());
        demonstration.setProjectId(dto.getProjectId());
        demonstration.setTitle(dto.getTitle());
        demonstration.setDescription(dto.getDescription());
        demonstration.setMediaUrls(dto.getMediaUrls());
        demonstration.setStatus(dto.getStatus());
        demonstration.setReviewerId(dto.getReviewerId());
        demonstration.setReviewNotes(dto.getReviewNotes());
        demonstration.setScore(dto.getScore());
        PracticalDemonstration saved = demonstrationRepository.save(demonstration);
        return toDTO(saved);
    }

    public void deleteDemonstration(UUID id) {
        PracticalDemonstration demonstration = demonstrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PracticalDemonstration", "id", id));
        demonstration.setIsDeleted(true);
        demonstrationRepository.save(demonstration);
    }

    // ── Workflow ─────────────────────────────────────────────────

    public DemonstrationDTO submitForReview(UUID id) {
        PracticalDemonstration demonstration = demonstrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PracticalDemonstration", "id", id));
        demonstration.setStatus(DemonstrationStatus.SUBMITTED);
        demonstration.setSubmittedAt(LocalDateTime.now());
        PracticalDemonstration saved = demonstrationRepository.save(demonstration);
        return toDTO(saved);
    }

    public DemonstrationDTO review(UUID id, UUID reviewerId, DemonstrationStatus status, String notes, Integer score) {
        PracticalDemonstration demonstration = demonstrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PracticalDemonstration", "id", id));
        demonstration.setReviewerId(reviewerId);
        demonstration.setStatus(status);
        demonstration.setReviewNotes(notes);
        demonstration.setScore(score);
        demonstration.setReviewedAt(LocalDateTime.now());
        PracticalDemonstration saved = demonstrationRepository.save(demonstration);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<DemonstrationDTO> getStudentDemonstrations(UUID studentId) {
        return demonstrationRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DemonstrationDTO> getDemonstrationsByStatus(UUID institutionId, DemonstrationStatus status) {
        return demonstrationRepository.findByStatusAndIsDeletedFalse(status)
                .stream()
                .filter(d -> institutionId.equals(d.getInstitutionId()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DemonstrationDTO> getDemonstrationsByCompetency(UUID competencyId) {
        return demonstrationRepository.findByCompetencyIdAndIsDeletedFalse(competencyId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Mapping helpers ──────────────────────────────────────────

    private DemonstrationDTO toDTO(PracticalDemonstration demonstration) {
        return DemonstrationDTO.builder()
                .id(demonstration.getId())
                .institutionId(demonstration.getInstitutionId())
                .studentId(demonstration.getStudentId())
                .competencyId(demonstration.getCompetencyId())
                .projectId(demonstration.getProjectId())
                .title(demonstration.getTitle())
                .description(demonstration.getDescription())
                .mediaUrls(demonstration.getMediaUrls())
                .status(demonstration.getStatus())
                .reviewerId(demonstration.getReviewerId())
                .reviewNotes(demonstration.getReviewNotes())
                .score(demonstration.getScore())
                .reviewedAt(demonstration.getReviewedAt())
                .submittedAt(demonstration.getSubmittedAt())
                .build();
    }
}
