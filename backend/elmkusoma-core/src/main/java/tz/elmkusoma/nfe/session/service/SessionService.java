package tz.elmkusoma.nfe.session.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.session.dto.SessionRequest;
import tz.elmkusoma.nfe.session.dto.SessionResponse;

import java.util.List;
import java.util.UUID;

public interface SessionService {

    SessionResponse createSession(UUID institutionId, SessionRequest request);

    SessionResponse getSession(UUID institutionId, UUID sessionId);

    PageResponse<SessionResponse> listSessions(UUID institutionId, int page, int size);

    SessionResponse updateSession(UUID institutionId, UUID sessionId, SessionRequest request);

    void deleteSession(UUID institutionId, UUID sessionId);

    List<SessionResponse> getSessionsByProvider(UUID institutionId, UUID providerId);

    List<SessionResponse> getSessionsByProgram(UUID institutionId, UUID programId);

    List<SessionResponse> getSessionsByStatus(UUID institutionId, String status);
}
