package tz.elmkusoma.liveclass.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tz.elmkusoma.config.CorePresenceService;
import tz.elmkusoma.config.EventPublisherService;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassAttendanceDetailRepository;
import tz.elmkusoma.liveclass.repository.LiveClassChatMessageRepository;
import tz.elmkusoma.liveclass.repository.LiveClassHandRaiseQueueRepository;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.liveclass.repository.LiveClassSessionEventRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R6 regression: the JOIN gate on {@code LiveClassWebSocketHandler.handleJoin}.
 *
 * Proves the authorization fence stays closed for users who are not active
 * members of the class institution, for sessions whose token-derived institution
 * does not match the class, for classes that are not in session, for
 * unauthenticated sessions, and for observers without an authority role — and
 * open for a verified member of a live class.
 */
@ExtendWith(MockitoExtension.class)
class LiveClassWebSocketHandlerJoinGateTest {

    @Mock private LiveClassRepository liveClassRepository;
    @Mock private LiveClassParticipantRepository participantRepository;
    @Mock private LiveClassChatMessageRepository chatMessageRepository;
    @Mock private LiveClassSessionEventRepository sessionEventRepository;
    @Mock private UserRepository userRepository;
    @Mock private InstitutionMembershipRepository membershipRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private LiveClassHandRaiseQueueRepository handRaiseQueueRepository;
    @Mock private LiveClassAttendanceDetailRepository attendanceDetailRepository;
    @Mock private CorePresenceService corePresenceService;
    @Mock private EventPublisherService eventPublisherService;

    private LiveClassWebSocketHandler handler;

    private final UUID classId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID institutionId = UUID.randomUUID();

    private WebSocketSession session;
    private Map<String, Object> attrs;
    private LiveClass liveClass;

    @BeforeEach
    void setUp() {
        handler = new LiveClassWebSocketHandler(liveClassRepository, participantRepository,
                chatMessageRepository, sessionEventRepository, userRepository, membershipRepository,
                new ObjectMapper(), corePresenceService, eventPublisherService, teacherRepository,
                handRaiseQueueRepository, attendanceDetailRepository);

        liveClass = new LiveClass();
        liveClass.setInstitutionId(institutionId);
        liveClass.setStatus("IN_PROGRESS");

        session = mock(WebSocketSession.class);
        attrs = new HashMap<>();
        attrs.put("userId", userId);
        attrs.put("institutionId", institutionId);
        lenient().when(session.getId()).thenReturn("s1");
        lenient().when(session.getAttributes()).thenReturn(attrs);
        lenient().when(session.getUri()).thenReturn(URI.create("ws://localhost/live-class/" + classId));
        lenient().when(session.isOpen()).thenReturn(true);
    }

    @Test
    void joinDeniedForNonMemberOfInstitution() throws Exception {
        handler.afterConnectionEstablished(session);
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(liveClass));
        when(userRepository.findById(userId)).thenReturn(Optional.of(learner()));
        when(membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, institutionId))
                .thenReturn(false);

        send("{\"type\":\"JOIN\"}");

        assertTrue(lastFrame().contains("You are not a member of this institution"), lastFrame());
        verify(participantRepository, never()).save(any(LiveClassParticipant.class));
        verify(corePresenceService, never()).userOnline(any(), any());
    }

    @Test
    void joinDeniedWhenSessionInstitutionMismatchesClass() throws Exception {
        attrs.put("institutionId", UUID.randomUUID());
        handler.afterConnectionEstablished(session);
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(liveClass));
        when(userRepository.findById(userId)).thenReturn(Optional.of(learner()));

        send("{\"type\":\"JOIN\"}");

        assertTrue(lastFrame().contains("You are not authorized to join this class"), lastFrame());
        verify(participantRepository, never()).save(any(LiveClassParticipant.class));
    }

    @Test
    void joinDeniedWhenClassNotInSession() throws Exception {
        liveClass.setStatus("SCHEDULED");
        handler.afterConnectionEstablished(session);
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(liveClass));
        when(userRepository.findById(userId)).thenReturn(Optional.of(learner()));

        send("{\"type\":\"JOIN\"}");

        assertTrue(lastFrame().contains("not currently in session"), lastFrame());
        verify(participantRepository, never()).save(any(LiveClassParticipant.class));
    }

    @Test
    void joinDeniedWithoutAuthentication() throws Exception {
        attrs.remove("userId");
        // Simulate a session that somehow carries a class binding without a
        // token-derived user: the join must still refuse.
        sessionClassMap().put("s1", classId);

        send("{\"type\":\"JOIN\"}");

        assertTrue(lastFrame().contains("Authentication required"), lastFrame());
        verify(participantRepository, never()).save(any(LiveClassParticipant.class));
    }

    @Test
    void observerJoinDeniedForNonAuthorityRole() throws Exception {
        attrs.put("userRole", "STUDENT");
        handler.afterConnectionEstablished(session);
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(liveClass));
        when(userRepository.findById(userId)).thenReturn(Optional.of(learner()));

        send("{\"type\":\"OBSERVER_JOIN\"}");

        assertTrue(lastFrame().contains("Only education authority users can observe"), lastFrame());
        verify(participantRepository, never()).save(any(LiveClassParticipant.class));
    }

    @Test
    void joinSucceedsForMemberOfLiveClassInstitution() throws Exception {
        handler.afterConnectionEstablished(session);
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(liveClass));
        when(userRepository.findById(userId)).thenReturn(Optional.of(learner()));
        when(membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, institutionId))
                .thenReturn(true);
        when(teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)).thenReturn(Optional.empty());
        when(participantRepository.findByLiveClassIdAndUserIdAndIsDeletedFalse(classId, userId))
                .thenReturn(Optional.empty());
        when(chatMessageRepository.findByLiveClassIdAndIsDeletedFalseOrderBySentAtDesc(classId))
                .thenReturn(List.of());

        send("{\"type\":\"JOIN\"}");

        ArgumentCaptor<LiveClassParticipant> captor = ArgumentCaptor.forClass(LiveClassParticipant.class);
        verify(participantRepository).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                LiveClassParticipant.ROLE_LEARNER, captor.getValue().getRole());
        verify(corePresenceService).userOnline(userId, institutionId);
        verify(session, atLeastOnce()).sendMessage(any(TextMessage.class));
        assertTrue(lastFrame().contains("\"type\":\"PARTICIPANTS\""), lastFrame());
    }

    // ------------------------------------------------------------------

    private void send(String json) throws Exception {
        handler.handleTextMessage(session, new TextMessage(json));
    }

    private User learner() {
        User user = new User();
        user.setRole(User.Role.STUDENT);
        user.setFirstName("Asha");
        user.setLastName("Mbwana");
        return user;
    }

    @SuppressWarnings("unchecked")
    private Map<String, UUID> sessionClassMap() throws Exception {
        Field field = LiveClassWebSocketHandler.class.getDeclaredField("sessionClassMap");
        field.setAccessible(true);
        return (Map<String, UUID>) field.get(handler);
    }

    private String lastFrame() throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        return captor.getValue().getPayload();
    }
}
