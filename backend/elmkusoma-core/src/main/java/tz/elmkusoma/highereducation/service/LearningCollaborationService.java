package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.LearningCollaboration;
import tz.elmkusoma.highereducation.domain.CollaborationType;
import tz.elmkusoma.highereducation.domain.CollaborationStatus;
import tz.elmkusoma.highereducation.dto.LearningCollaborationDTO;
import tz.elmkusoma.highereducation.repository.LearningCollaborationRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningCollaborationService {

    private final LearningCollaborationRepository collaborationRepository;

    public LearningCollaborationDTO createCollaboration(LearningCollaborationDTO dto) {
        LearningCollaboration collab = LearningCollaboration.builder()
                .studentId(dto.getStudentId())
                .peerStudentId(dto.getPeerStudentId())
                .collaborationType(CollaborationType.valueOf(
                        dto.getCollaborationType() != null ? dto.getCollaborationType() : "STUDY_GROUP"))
                .title(dto.getTitle())
                .description(dto.getDescription())
                .courseId(dto.getCourseId())
                .status(CollaborationStatus.ACTIVE)
                .institutionId(dto.getInstitutionId() != null ? dto.getInstitutionId() : UUID.randomUUID())
                .build();
        return toDTO(collaborationRepository.save(collab));
    }

    public LearningCollaborationDTO getCollaboration(UUID id) {
        LearningCollaboration collab = collaborationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration not found"));
        return toDTO(collab);
    }

    public List<LearningCollaborationDTO> getStudentCollaborations(UUID studentId) {
        return collaborationRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<LearningCollaborationDTO> getStudentActiveCollaborations(UUID studentId) {
        return collaborationRepository.findByStudentIdAndStatus(studentId, "ACTIVE")
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public LearningCollaborationDTO updateCollaboration(UUID id, LearningCollaborationDTO dto) {
        LearningCollaboration collab = collaborationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration not found"));
        if (dto.getTitle() != null) collab.setTitle(dto.getTitle());
        if (dto.getDescription() != null) collab.setDescription(dto.getDescription());
        if (dto.getStatus() != null) collab.setStatus(CollaborationStatus.valueOf(dto.getStatus()));
        return toDTO(collaborationRepository.save(collab));
    }

    public void deleteCollaboration(UUID id) {
        if (!collaborationRepository.existsById(id))
            throw new ResourceNotFoundException("Collaboration not found");
        collaborationRepository.deleteById(id);
    }

    private LearningCollaborationDTO toDTO(LearningCollaboration c) {
        return LearningCollaborationDTO.builder()
                .id(c.getId()).studentId(c.getStudentId()).peerStudentId(c.getPeerStudentId())
                .collaborationType(c.getCollaborationType().name()).title(c.getTitle())
                .description(c.getDescription()).courseId(c.getCourseId())
                .status(c.getStatus().name())
                .build();
    }
}
