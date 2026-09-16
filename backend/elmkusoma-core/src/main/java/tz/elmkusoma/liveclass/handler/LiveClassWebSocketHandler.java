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
import tz.elmkusoma.liveclass.domain.LiveClassChatMessage;
import tz.elmkusoma.liveclass.domain.LiveClassSessionEvent;
import tz.elmkusoma.liveclass.repository.LiveClassChatMessageRepository;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.liveclass.repository.LiveClassSessionEventRepository;
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
    private final LiveClassChatMessageRepository chatMessageRepository;
    private final LiveClassSessionEventRepository sessionEventRepository;
    private final UserRepository userRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionClassMap = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> classSessions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionHandRaised = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionScreenSharing = new ConcurrentHashMap<>();

    private final tz.elmkusoma.teacher.repository.TeacherRepository teacherRepository;

    public LiveClassWebSocketHandler(LiveClassRepository liveClassRepository,
                                      LiveClassParticipantRepository participantRepository,
                                      LiveClassChatMessageRepository chatMessageRepository,
                                      LiveClassSessionEventRepository sessionEventRepository,
                                      UserRepository userRepository,
                                      InstitutionMembershipRepository membershipRepository,
                                      ObjectMapper objectMapper,
                                      tz.elmkusoma.teacher.repository.TeacherRepository teacherRepository) {
        this.liveClassRepository = liveClassRepository;
        this.participantRepository = participantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.sessionEventRepository = sessionEventRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.objectMapper = objectMapper;
        this.teacherRepository = teacherRepository;
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
            case "RAISE_HAND" -> handleRaiseHand(session, classId, payload);
            case "LOWER_HAND" -> handleLowerHand(session, classId);
            case "SCREEN_SHARE_START" -> handleScreenShareStart(session, classId);
            case "SCREEN_SHARE_STOP" -> handleScreenShareStop(session, classId);
            case "MUTE_PARTICIPANT" -> handleMuteParticipant(session, classId, payload);
            case "UNMUTE_PARTICIPANT" -> handleUnmuteParticipant(session, classId, payload);
            case "KICK_PARTICIPANT" -> handleKickParticipant(session, classId, payload);
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

        if (!"IN_PROGRESS".equals(liveClass.getStatus()) && !"LIVE".equals(liveClass.getStatus())) {
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

        boolean isTeacher = teacherRepository.findByUserIdAndInstitutionId(userId, classInstitutionId).isPresent();
        String participantRole = isTeacher ? "TEACHER" : "LEARNER";

        Optional<LiveClassParticipant> existing = participantRepository
                .findByLiveClassIdAndUserIdAndIsDeletedFalse(classId, userId);

        LiveClassParticipant participant;
        if (existing.isPresent()) {
            participant = existing.get();
            participant.setLeftAt(null);
            participant.setConnectionId(session.getId());
            participant.setRole(participantRole);
        } else {
            if (!isTeacher && liveClass.getMaxParticipants() != null) {
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
                    .role(participantRole)
                    .joinedAt(LocalDateTime.now())
                    .connectionId(session.getId())
                    .build();
        }
        participantRepository.save(participant);

        sessionUserMap.put(session.getId(), userId);
        classSessions.computeIfAbsent(classId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());

        String displayName = user.getFullName();
        session.getAttributes().put("userName", displayName);

        recordEvent(classId, userId, "USER_JOINED", displayName);

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

        List<LiveClassChatMessage> recentMessages = chatMessageRepository
                .findByLiveClassIdAndIsDeletedFalseOrderBySentAtDesc(classId);
        if (!recentMessages.isEmpty()) {
            Map<String, Object> historyEvent = new HashMap<>();
            historyEvent.put("type", "CHAT_HISTORY");
            historyEvent.put("messages", recentMessages.stream().limit(50).map(m -> {
                Map<String, Object> msg = new HashMap<>();
                msg.put("userId", m.getUserId().toString());
                msg.put("userName", m.getUserName());
                msg.put("message", m.getMessage());
                msg.put("timestamp", m.getSentAt().toString());
                return msg;
            }).toList());
            sendMessage(session, historyEvent);
        }

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

        String displayName = (String) session.getAttributes().get("userName");
        if (displayName == null) {
            User user = userRepository.findById(userId).orElse(null);
            displayName = user != null ? user.getFullName() : "Unknown";
        }

        LiveClassChatMessage chatMessage = LiveClassChatMessage.builder()
                .liveClassId(classId)
                .userId(userId)
                .userName(displayName)
                .message(msgContent)
                .messageType("CHAT")
                .sentAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(chatMessage);

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

    private void handleRaiseHand(WebSocketSession session, UUID classId, Map<String, Object> payload) throws IOException {
        UUID userId = sessionUserMap.get(session.getId());
        if (userId == null) {
            sendError(session, "You must join the session first");
            return;
        }

        sessionHandRaised.put(session.getId(), true);

        User user = userRepository.findById(userId).orElse(null);
        String displayName = user != null ? user.getFullName() : "Unknown";

        Map<String, Object> event = new HashMap<>();
        event.put("type", "HAND_RAISED");
        event.put("userId", userId.toString());
        event.put("userName", displayName);
        event.put("timestamp", LocalDateTime.now().toString());

        broadcastToClass(classId, event, null);
        recordEvent(classId, userId, "HAND_RAISED", displayName);
    }

    private void handleLowerHand(WebSocketSession session, UUID classId) throws IOException {
        UUID userId = sessionUserMap.get(session.getId());
        if (userId == null) return;

        sessionHandRaised.put(session.getId(), false);

        User user = userRepository.findById(userId).orElse(null);
        String displayName = user != null ? user.getFullName() : "Unknown";

        Map<String, Object> event = new HashMap<>();
        event.put("type", "HAND_LOWERED");
        event.put("userId", userId.toString());
        event.put("userName", displayName);
        event.put("timestamp", LocalDateTime.now().toString());

        broadcastToClass(classId, event, null);
    }

    private void handleScreenShareStart(WebSocketSession session, UUID classId) throws IOException {
        UUID userId = sessionUserMap.get(session.getId());
        if (userId == null) return;

        sessionScreenSharing.put(session.getId(), true);

        User user = userRepository.findById(userId).orElse(null);
        String displayName = user != null ? user.getFullName() : "Unknown";

        Map<String, Object> event = new HashMap<>();
        event.put("type", "SCREEN_SHARE_STARTED");
        event.put("userId", userId.toString());
        event.put("userName", displayName);
        event.put("timestamp", LocalDateTime.now().toString());

        broadcastToClass(classId, event, null);
        recordEvent(classId, userId, "SCREEN_SHARE_START", displayName);
    }

    private void handleScreenShareStop(WebSocketSession session, UUID classId) throws IOException {
        UUID userId = sessionUserMap.get(session.getId());
        if (userId == null) return;

        sessionScreenSharing.put(session.getId(), false);

        User user = userRepository.findById(userId).orElse(null);
        String displayName = user != null ? user.getFullName() : "Unknown";

        Map<String, Object> event = new HashMap<>();
        event.put("type", "SCREEN_SHARE_STOPPED");
        event.put("userId", userId.toString());
        event.put("userName", displayName);
        event.put("timestamp", LocalDateTime.now().toString());

        broadcastToClass(classId, event, null);
    }

    private void handleMuteParticipant(WebSocketSession session, UUID classId, Map<String, Object> payload) throws IOException {
        UUID teacherId = (UUID) session.getAttributes().get("userId");
        if (!isTeacher(teacherId, classId)) {
            sendError(session, "Only teachers can mute participants");
            return;
        }
        String targetUserId = (String) payload.get("userId");
        if (targetUserId == null) {
            sendError(session, "Target userId required");
            return;
        }
        Map<String, Object> event = new HashMap<>();
        event.put("type", "PARTICIPANT_MUTED");
        event.put("targetUserId", targetUserId);
        event.put("mutedBy", teacherId.toString());
        event.put("timestamp", LocalDateTime.now().toString());
        broadcastToClass(classId, event, null);
    }

    private void handleUnmuteParticipant(WebSocketSession session, UUID classId, Map<String, Object> payload) throws IOException {
        UUID teacherId = (UUID) session.getAttributes().get("userId");
        if (!isTeacher(teacherId, classId)) {
            sendError(session, "Only teachers can unmute participants");
            return;
        }
        String targetUserId = (String) payload.get("userId");
        if (targetUserId == null) {
            sendError(session, "Target userId required");
            return;
        }
        Map<String, Object> event = new HashMap<>();
        event.put("type", "PARTICIPANT_UNMUTED");
        event.put("targetUserId", targetUserId);
        event.put("unmutedBy", teacherId.toString());
        event.put("timestamp", LocalDateTime.now().toString());
        broadcastToClass(classId, event, null);
    }

    private void handleKickParticipant(WebSocketSession session, UUID classId, Map<String, Object> payload) throws IOException {
        UUID teacherId = (UUID) session.getAttributes().get("userId");
        if (!isTeacher(teacherId, classId)) {
            sendError(session, "Only teachers can kick participants");
            return;
        }
        String targetUserId = (String) payload.get("userId");
        if (targetUserId == null) {
            sendError(session, "Target userId required");
            return;
        }
        UUID targetId = UUID.fromString(targetUserId);
        for (Map.Entry<String, UUID> entry : sessionUserMap.entrySet()) {
            if (entry.getValue().equals(targetId)) {
                WebSocketSession targetSession = sessions.get(entry.getKey());
                if (targetSession != null && targetSession.isOpen()) {
                    Map<String, Object> kickEvent = new HashMap<>();
                    kickEvent.put("type", "KICKED");
                    kickEvent.put("message", "You have been removed from the class by the teacher.");
                    kickEvent.put("timestamp", LocalDateTime.now().toString());
                    targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(kickEvent)));
                    targetSession.close();
                }
                break;
            }
        }
        Map<String, Object> event = new HashMap<>();
        event.put("type", "PARTICIPANT_KICKED");
        event.put("targetUserId", targetUserId);
        event.put("kickedBy", teacherId.toString());
        event.put("timestamp", LocalDateTime.now().toString());
        broadcastToClass(classId, event, null);
    }

    private boolean isTeacher(UUID userId, UUID classId) {
        if (userId == null) return false;
        LiveClass liveClass = liveClassRepository.findById(classId).orElse(null);
        if (liveClass == null) return false;
        if (userId.equals(liveClass.getTeacherId())) return true;
        User user = userRepository.findById(userId).orElse(null);
        return user != null && (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.INSTITUTION_ADMIN);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID classId = sessionClassMap.remove(session.getId());
        UUID userId = sessionUserMap.remove(session.getId());
        sessions.remove(session.getId());
        sessionHandRaised.remove(session.getId());
        sessionScreenSharing.remove(session.getId());

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
        String displayName = user != null ? user.getFullName() : "Unknown";

        recordEvent(classId, userId, "USER_LEFT", displayName);

        Map<String, Object> leaveEvent = new HashMap<>();
        leaveEvent.put("type", "USER_LEFT");
        leaveEvent.put("userId", userId.toString());
        leaveEvent.put("userName", displayName);
        leaveEvent.put("timestamp", LocalDateTime.now().toString());
        leaveEvent.put("participantCount", (int) participantRepository.countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(classId));

        broadcastToClass(classId, leaveEvent, null);

        log.info("User {} left live class {}", userId, classId);
    }

    private void recordEvent(UUID classId, UUID userId, String eventType, String eventData) {
        try {
            LiveClassSessionEvent event = LiveClassSessionEvent.builder()
                    .liveClassId(classId)
                    .userId(userId)
                    .eventType(eventType)
                    .eventData(eventData)
                    .build();
            sessionEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to record session event: {}", eventType, e);
        }
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
