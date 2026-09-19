package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.WorkshopSession;
import tz.elmkusoma.highereducation.domain.WorkshopType;
import tz.elmkusoma.highereducation.domain.WorkshopStatus;
import tz.elmkusoma.highereducation.dto.WorkshopSessionDTO;
import tz.elmkusoma.highereducation.repository.WorkshopSessionRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkshopSessionService {

    private final WorkshopSessionRepository workshopSessionRepository;

    public WorkshopSessionDTO createSession(WorkshopSessionDTO dto) {
        WorkshopSession session = WorkshopSession.builder()
                .institutionId(dto.getInstitutionId())
                .studentId(dto.getStudentId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .workshopType(WorkshopType.valueOf(
                        dto.getWorkshopType() != null ? dto.getWorkshopType() : "WORKSHOP"))
                .courseId(dto.getCourseId())
                .scheduledAt(dto.getScheduledAt())
                .durationMinutes(dto.getDurationMinutes())
                .location(dto.getLocation())
                .status(WorkshopStatus.SCHEDULED)
                .maxParticipants(dto.getMaxParticipants())
                .currentParticipants(0)
                .materialsUrl(dto.getMaterialsUrl())
                .instructorId(dto.getInstructorId())
                .build();
        return toDTO(workshopSessionRepository.save(session));
    }

    public WorkshopSessionDTO getSession(UUID id) {
        WorkshopSession session = workshopSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop session not found"));
        return toDTO(session);
    }

    public List<WorkshopSessionDTO> getStudentSessions(UUID studentId) {
        return workshopSessionRepository.findByStudentIdOrderByScheduledAtDesc(studentId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<WorkshopSessionDTO> getInstitutionSessions(UUID institutionId) {
        return workshopSessionRepository.findByInstitutionIdOrderByScheduledAtDesc(institutionId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public WorkshopSessionDTO updateSession(UUID id, WorkshopSessionDTO dto) {
        WorkshopSession session = workshopSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop session not found"));
        if (dto.getTitle() != null) session.setTitle(dto.getTitle());
        if (dto.getDescription() != null) session.setDescription(dto.getDescription());
        if (dto.getStatus() != null) session.setStatus(WorkshopStatus.valueOf(dto.getStatus()));
        if (dto.getScheduledAt() != null) session.setScheduledAt(dto.getScheduledAt());
        if (dto.getLocation() != null) session.setLocation(dto.getLocation());
        if (dto.getCurrentParticipants() != null) session.setCurrentParticipants(dto.getCurrentParticipants());
        return toDTO(workshopSessionRepository.save(session));
    }

    public void deleteSession(UUID id) {
        if (!workshopSessionRepository.existsById(id))
            throw new ResourceNotFoundException("Workshop session not found");
        workshopSessionRepository.deleteById(id);
    }

    private WorkshopSessionDTO toDTO(WorkshopSession s) {
        return WorkshopSessionDTO.builder()
                .id(s.getId()).institutionId(s.getInstitutionId()).studentId(s.getStudentId())
                .title(s.getTitle()).description(s.getDescription())
                .workshopType(s.getWorkshopType().name()).courseId(s.getCourseId())
                .scheduledAt(s.getScheduledAt()).durationMinutes(s.getDurationMinutes())
                .location(s.getLocation()).status(s.getStatus().name())
                .maxParticipants(s.getMaxParticipants()).currentParticipants(s.getCurrentParticipants())
                .materialsUrl(s.getMaterialsUrl()).instructorId(s.getInstructorId())
                .build();
    }
}
