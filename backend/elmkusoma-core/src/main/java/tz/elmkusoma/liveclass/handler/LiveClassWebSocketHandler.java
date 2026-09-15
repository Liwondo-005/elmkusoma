package tz.elmkusoma.liveclass.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveClassWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveClassWebSocketHandler.class);

    private final LiveClassRepository liveClassRepository;
    private final LiveClassParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionClassMap = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> classSessions = new ConcurrentHashMap<>();

    public LiveClassWebSocketHandler(LiveClassRepository liveClassRepository,
                                      LiveClassParticipantRepository participantRepository,
                                      UserRepository userRepository,
                                      InstitutionMembershipRepository membershipRepository,
                                      ObjectMapper objectMapper) {
        this.liveClassRepository = liveClassRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID userId = (UUID) session.getAttributes().get("userId");
        UUID institutionId = (UUID) session.getAttributes().get("institutionId");

        if (userId == null) {
            sendError(session, "Authentication required");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }

        String uri = session.getUri().toString();
        List<String> pathSegments = UriComponentsBuilder.fromUriString(uri).build().getPathSegments();
        String classIdStr = pathSegments.get(pathSegments.size() - 1);
        UUID classId = UUID.fromString(classIdStr);

        sessions.put(session.getId(), session);
        sessionClassMap.put(session.getId(), classId);

        log.info("WebSocket connected: session={}, classId={}, userId={}", session.getId(), classId, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) payload.get("type");
        UUID classId = sessionClassMap.get(session.getId());

        if (classId == null) {
            sendError(session, "Not connected to a class");
            return;
        }

        switch (type) {
            case "JOIN" -> handleJoin(session, classId, payload);
            case "CHAT" -> handleChat(session, classId, payload);
            case "LEAVE" -> handleLeave(session, classId);
            default -> sendError(session, "Unknown message type: " + type);
        }
    }

    private void handleJoin(WebSocketSession session, UUID classId, Map<String, Object> payload) throws IOException {
        UUID userId = (UUID) session.getAttributes().get("userId");
        UUID institutionId = (UUID) session.getAttributes().get("institutionId");

        if (userId == null) {
            sendError(session, "Authentication required");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            sendError(session, "User not found");
            return;
        }

        LiveClass liveClass = liveClassRepository.findById(classId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            sendError(session, "Live class not found");
            return;
        }

        if (!"IN_PROGRESS".equals(liveClass.getStatus())) {
            sendError(session, "This live class is not currently in session");
            return;
        }

        UUID classInstitutionId = liveClass.getInstitutionId();
        if (classInstitutionId != null && institutionId != null) {
            if (!classInstitutionId.equals(institutionId)) {
                sendError(session, "You are not authorized to join this class");
                return;
            }
        }

        boolean isMember = membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, classInstitutionId);
        if (!isMember) {
            sendError(session, "You are not a member of this institution");
            return;
        }

        Optional<LiveClassParticipant> existing = participantRepository
                .findByLiveClassIdAndUserIdAndIsDeletedFalse(classId, userId);

        LiveClassParticipant participant;
        if (existing.isPresent()) {
            participant = existing.get();
            participant.setLeftAt(null);
            participant.setConnectionId(session.getId());
            participant.setJoinedAt(LocalDateTime.now());
        } else {
            if (liveClass.getMaxParticipants() != null) {
                long currentCount = participantRepository.countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(classId);
                if (currentCount >= liveClass.getMaxParticipants()) {
                    sendError(session, "This live class is full");
                    return;
                }
            }

            participant = LiveClassParticipant.builder()
                    .liveClassId(classId)
                    .userId(userId)
                    .institutionId(classInstitutionId)
                    .role("LEARNER")
                    .joinedAt(LocalDateTime.now())
                    .connectionId(session.getId())
                    .build();
        }
        participantRepository.save(participant);

        sessionUserMap.put(session.getId(), userId);
        classSessions.computeIfAbsent(classId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());

        String displayName = user.getFullName();

        session.getAttributes().put("userName", displayName);

        Map<String, Object> joinEvent = new HashMap<>();
        joinEvent.put("type", "USER_JOINED");
        joinEvent.put("userId", userId.toString());
        joinEvent.put("userName", displayName);
        joinEvent.put("timestamp", LocalDateTime.now().toString());
        joinEvent.put("participantCount", (int) participantRepository.countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(classId));

        broadcastToClass(classId, joinEvent, null);

        List<LiveClassParticipant> currentParticipants = participantRepository
                .findByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(classId);
        List<Map<String, Object>> participantList = currentParticipants.stream().map(p -> {
            User pUser = userRepository.findById(p.getUserId()).orElse(null);
            Map<String, Object> pInfo = new HashMap<>();
            pInfo.put("userId", p.getUserId().toString());
            pInfo.put("userName", pUser != null ? pUser.getFullName() : "Unknown");
            pInfo.put("role", p.getRole());
            pInfo.put("joinedAt", p.getJoinedAt() != null ? p.getJoinedAt().toString() : null);
            return pInfo;
        }).toList();

        Map<String, Object> participantResponse = new HashMap<>();
        participantResponse.put("type", "PARTICIPANTS");
        participantResponse.put("participants", participantList);
        sendMessage(session, participantResponse);

        log.info("User {} joined live class {} (session: {})", userId, classId, session.getId());
    }

    private void handleChat(WebSocketSession session, UUID classId, Map<String, Object> payload) throws IOException {
        UUID userId = sessionUserMap.get(session.getId());
        if (userId == null) {
            sendError(session, "You must join the session first");
            return;
        }

        String msgContent = (String) payload.get("message");
        if (msgContent == null || msgContent.isBlank()) return;
        if (msgContent.length() > 1000) {
            sendError(session, "Message too long (max 1000 characters)");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        String displayName = (String) session.getAttributes().get("userName");
        if (displayName == null) {
            displayName = user != null ? user.getFullName() : "Unknown";
        }

        Map<String, Object> chatEvent = new HashMap<>();
        chatEvent.put("type", "CHAT_MESSAGE");
        chatEvent.put("userId", userId.toString());
        chatEvent.put("userName", displayName);
        chatEvent.put("message", msgContent);
        chatEvent.put("timestamp", LocalDateTime.now().toString());

        broadcastToClass(classId, chatEvent, null);

        log.debug("Chat message in live class {} from {}: {}", classId, userId, msgContent);
    }

    private void handleLeave(WebSocketSession session, UUID classId) throws IOException {
        UUID userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            leaveClass(session, classId, userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID classId = sessionClassMap.remove(session.getId());
        UUID userId = sessionUserMap.remove(session.getId());
        sessions.remove(session.getId());

        if (classId != null) {
            Set<String> classSessionSet = classSessions.get(classId);
            if (classSessionSet != null) {
                classSessionSet.remove(session.getId());
                if (classSessionSet.isEmpty()) classSessions.remove(classId);
            }
        }

        if (classId != null && userId != null) {
            try {
                leaveClass(session, classId, userId);
            } catch (IOException e) {
                log.error("Error handling disconnect for user {} in class {}", userId, classId, e);
            }
        }

        log.info("WebSocket disconnected: session={}", session.getId());
    }

    private void leaveClass(WebSocketSession session, UUID classId, UUID userId) throws IOException {
        participantRepository.findByLiveClassIdAndUserIdAndIsDeletedFalse(classId, userId)
                .ifPresent(participant -> {
                    participant.setLeftAt(LocalDateTime.now());
                    if (participant.getJoinedAt() != null) {
                        participant.setDurationSeconds(
                                java.time.Duration.between(participant.getJoinedAt(), LocalDateTime.now()).getSeconds());
                    }
                    participantRepository.save(participant);
                });

        User user = userRepository.findById(userId).orElse(null);

        Map<String, Object> leaveEvent = new HashMap<>();
        leaveEvent.put("type", "USER_LEFT");
        leaveEvent.put("userId", userId.toString());
        leaveEvent.put("userName", user != null ? user.getFullName() : "Unknown");
        leaveEvent.put("timestamp", LocalDateTime.now().toString());
        leaveEvent.put("participantCount", (int) participantRepository.countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(classId));

        broadcastToClass(classId, leaveEvent, null);

        log.info("User {} left live class {}", userId, classId);
    }

    private void broadcastToClass(UUID classId, Map<String, Object> message, String excludeSessionId) {
        Set<String> sessionIds = classSessions.get(classId);
        if (sessionIds == null) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize message", e);
            return;
        }

        TextMessage textMessage = new TextMessage(json);
        for (String sid : sessionIds) {
            if (excludeSessionId != null && sid.equals(excludeSessionId)) continue;
            WebSocketSession wsSession = sessions.get(sid);
            if (wsSession != null && wsSession.isOpen()) {
                try {
                    wsSession.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("Failed to send message to session {}", sid, e);
                }
            }
        }
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send message to session {}", session.getId(), e);
        }
    }

    private void sendError(WebSocketSession session, String error) {
        try {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("type", "ERROR");
            errorMap.put("error", error);
            sendMessage(session, errorMap);
        } catch (Exception e) {
            log.error("Failed to send error to session {}", session.getId(), e);
        }
    }
}
