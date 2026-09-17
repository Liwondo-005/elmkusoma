package tz.elmkusoma.nfe.session.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.session.domain.NfeSession;
import tz.elmkusoma.nfe.session.dto.SessionRequest;
import tz.elmkusoma.nfe.session.dto.SessionResponse;
import tz.elmkusoma.nfe.session.repository.NfeSessionRepository;
import tz.elmkusoma.nfe.session.service.SessionService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionServiceImpl implements SessionService {

    private final NfeSessionRepository sessionRepository;
    private final OwnershipGuard ownershipGuard;

    @Override
    public SessionResponse createSession(UUID institutionId, SessionRequest request) {
        NfeSession session = NfeSession.builder()
                .providerId(request.getProviderId())
                .programId(request.getProgramId())
                .title(request.getTitle())
                .description(request.getDescription())
                .sessionType(NfeSession.SessionType.valueOf(request.getSessionType()))
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(request.getDurationMinutes())
                .meetingUrl(request.getMeetingUrl())
                .maxParticipants(request.getMaxParticipants())
                .status(request.getStatus() != null ? NfeSession.SessionStatus.valueOf(request.getStatus()) : NfeSession.SessionStatus.SCHEDULED)
                .build();
        session.setInstitutionId(institutionId);

        NfeSession saved = sessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID institutionId, UUID sessionId) {
        NfeSession session = sessionRepository.findByIdAndInstitutionId(sessionId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));
        ownershipGuard.verifyInstitution(session.getInstitutionId(), institutionId);
        return mapToResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SessionResponse> listSessions(UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<NfeSession> allSessions = sessionRepository.findAllByInstitutionId(institutionId);

        int start = Math.min(page * size, allSessions.size());
        int end = Math.min((page + 1) * size, allSessions.size());
        List<NfeSession> paged = allSessions.subList(start, end);

        Page<NfeSession> sessionPage = new org.springframework.data.domain.PageImpl<>(
                paged, pageable, allSessions.size());

        List<SessionResponse> content = sessionPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                sessionPage.getNumber(),
                sessionPage.getSize(),
                sessionPage.getTotalElements(),
                sessionPage.getTotalPages(),
                sessionPage.isFirst(),
                sessionPage.isLast()
        );
    }

    @Override
    public SessionResponse updateSession(UUID institutionId, UUID sessionId, SessionRequest request) {
        NfeSession session = sessionRepository.findByIdAndInstitutionId(sessionId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));
        ownershipGuard.verifyInstitution(session.getInstitutionId(), institutionId);

        if (request.getProviderId() != null) session.setProviderId(request.getProviderId());
        if (request.getProgramId() != null) session.setProgramId(request.getProgramId());
        if (request.getTitle() != null) session.setTitle(request.getTitle());
        if (request.getDescription() != null) session.setDescription(request.getDescription());
        if (request.getSessionType() != null) session.setSessionType(NfeSession.SessionType.valueOf(request.getSessionType()));
        if (request.getScheduledAt() != null) session.setScheduledAt(request.getScheduledAt());
        if (request.getDurationMinutes() != null) session.setDurationMinutes(request.getDurationMinutes());
        if (request.getMeetingUrl() != null) session.setMeetingUrl(request.getMeetingUrl());
        if (request.getMaxParticipants() != null) session.setMaxParticipants(request.getMaxParticipants());
        if (request.getStatus() != null) session.setStatus(NfeSession.SessionStatus.valueOf(request.getStatus()));

        NfeSession saved = sessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Override
    public void deleteSession(UUID institutionId, UUID sessionId) {
        NfeSession session = sessionRepository.findByIdAndInstitutionId(sessionId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));
        ownershipGuard.verifyInstitution(session.getInstitutionId(), institutionId);
        session.setIsDeleted(true);
        sessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getSessionsByProvider(UUID institutionId, UUID providerId) {
        return sessionRepository.findByProviderId(providerId).stream()
                .filter(s -> institutionId.equals(s.getInstitutionId()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getSessionsByProgram(UUID institutionId, UUID programId) {
        return sessionRepository.findByProgramId(programId).stream()
                .filter(s -> institutionId.equals(s.getInstitutionId()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getSessionsByStatus(UUID institutionId, String status) {
        return sessionRepository.findByStatusAndInstitutionId(status, institutionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private SessionResponse mapToResponse(NfeSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .institutionId(session.getInstitutionId())
                .providerId(session.getProviderId())
                .programId(session.getProgramId())
                .title(session.getTitle())
                .description(session.getDescription())
                .sessionType(session.getSessionType().name())
                .scheduledAt(session.getScheduledAt())
                .durationMinutes(session.getDurationMinutes())
                .meetingUrl(session.getMeetingUrl())
                .maxParticipants(session.getMaxParticipants())
                .status(session.getStatus().name())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
