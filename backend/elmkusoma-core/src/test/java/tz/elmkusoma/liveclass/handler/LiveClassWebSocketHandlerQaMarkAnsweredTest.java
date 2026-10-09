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
import tz.elmkusoma.liveclass.domain.LiveClassChatMessage;
import tz.elmkusoma.liveclass.domain.LiveClassSessionEvent;
import tz.elmkusoma.liveclass.repository.LiveClassAttendanceDetailRepository;
import tz.elmkusoma.liveclass.repository.LiveClassChatMessageRepository;
import tz.elmkusoma.liveclass.repository.LiveClassHandRaiseQueueRepository;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.liveclass.repository.LiveClassSessionEventRepository;
import tz.elmkusoma.liveclass.service.LiveKitService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Q&A "mark answered" workflow: teacher-only toggle persisted on the chat message
 * (V91 answered_at/answered_by), audited as a session event, and broadcast to the
 * room so every participant's badge updates. Learners and non-question messages are
 * rejected without any repository write.
 */
@ExtendWith(MockitoExtension.class)
class LiveClassWebSocketHandlerQaMarkAnsweredTest {

    @Mock private LiveClassRepository liveClassRepository;
    @Mock private LiveClassParticipantRepository participantRepository;
    @Mock private LiveClassChatMessageRepository chatMessageRepository;
    @Mock private LiveClassSessionEventRepository sessionEventRepository;
    @Mock private UserRepository userRepository;
    @Mock private InstitutionMembershipRepository membershipRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private LiveClassHandRaiseQueueRepository handRaiseQueueRepository;
    @Mock private LiveClassAttendanceDetailRepository attendanceDetailRepository;
    @Mock private LiveKitService liveKitService;
    @Mock private CorePresenceService corePresenceService;
    @Mock private EventPublisherService eventPublisherService;

    private LiveClassWebSocketHandler handler;

    private final UUID classId = UUID.randomUUID();
    private final UUID teacherUserId = UUID.randomUUID();
    private final UUID institutionId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    private WebSocketSession session;
    private LiveClassChatMessage question;

    @BeforeEach
    void setUp() throws Exception {
        handler = new LiveClassWebSocketHandler(liveClassRepository, participantRepository,
                chatMessageRepository, sessionEventRepository, userRepository, membershipRepository,
                new ObjectMapper(), corePresenceService, eventPublisherService, teacherRepository,
                handRaiseQueueRepository, attendanceDetailRepository, liveKitService);

        LiveClass liveClass = new LiveClass();
        liveClass.setInstitutionId(institutionId);
        liveClass.setTeacherId(teacherId);
        when(liveClassRepository.findById(classId)).thenReturn(Optional.of(liveClass));

        session = mock(WebSocketSession.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", teacherUserId);
        attrs.put("institutionId", institutionId);
        when(session.getId()).thenReturn("s1");
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/live-class/" + classId));
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
        joinRoom();

        question = LiveClassChatMessage.builder()
                .liveClassId(classId)
                .userId(UUID.randomUUID())
                .userName("Neema")
                .message("[Q&A] Why is the sky blue?")
                .messageType("Q&A")
                .sentAt(LocalDateTime.now())
                .build();
        question.setId(UUID.randomUUID());
    }

    @Test
    void teacherMarksQuestionAnswered_persistsAuditsAndBroadcasts() throws Exception {
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(owningTeacher()));
        when(chatMessageRepository.findById(question.getId())).thenReturn(Optional.of(question));

        send("{\"type\":\"QA_MARK_ANSWERED\",\"messageId\":\"" + question.getId() + "\"}");

        assertNotNull(question.getAnsweredAt());
        assertEquals(teacherUserId, question.getAnsweredBy());
        verify(chatMessageRepository).save(question);
        verify(sessionEventRepository).save(argThat((LiveClassSessionEvent e) ->
                "QA_ANSWERED".equals(e.getEventType()) && question.getId().toString().equals(e.getEventData().split(" ")[0])));

        String broadcast = lastFrame();
        assertTrue(broadcast.contains("\"type\":\"QA_ANSWERED\""), broadcast);
        assertTrue(broadcast.contains("\"answered\":true"), broadcast);
        assertTrue(broadcast.contains(question.getId().toString()), broadcast);
    }

    @Test
    void teacherReopensAnsweredQuestion_clearsAnswerAndBroadcasts() throws Exception {
        question.setAnsweredAt(LocalDateTime.now());
        question.setAnsweredBy(UUID.randomUUID());
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(owningTeacher()));
        when(chatMessageRepository.findById(question.getId())).thenReturn(Optional.of(question));

        send("{\"type\":\"QA_MARK_ANSWERED\",\"messageId\":\"" + question.getId() + "\"}");

        assertNull(question.getAnsweredAt());
        assertNull(question.getAnsweredBy());
        verify(chatMessageRepository).save(question);

        String broadcast = lastFrame();
        assertTrue(broadcast.contains("\"answered\":false"), broadcast);
    }

    @Test
    void learnerCannotMarkAnswered_rejectedWithoutRepositoryWrite() throws Exception {
        when(userRepository.findById(teacherUserId)).thenReturn(Optional.of(student()));

        send("{\"type\":\"QA_MARK_ANSWERED\",\"messageId\":\"" + question.getId() + "\"}");

        assertNull(question.getAnsweredAt());
        verify(chatMessageRepository, never()).save(any());
        verify(sessionEventRepository, never()).save(any());
        assertTrue(lastFrame().contains("Only teachers can mark questions answered"), lastFrame());
    }

    @Test
    void markAnswered_onRegularChatMessage_rejectedAsNotAQuestion() throws Exception {
        question.setMessageType("CHAT");
        question.setMessage("just chatting");
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(owningTeacher()));
        when(chatMessageRepository.findById(question.getId())).thenReturn(Optional.of(question));

        send("{\"type\":\"QA_MARK_ANSWERED\",\"messageId\":\"" + question.getId() + "\"}");

        verify(chatMessageRepository, never()).save(any());
        assertTrue(lastFrame().contains("Only Q&A questions"), lastFrame());
    }

    @Test
    void markAnswered_unknownMessage_reportsNotFound() throws Exception {
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(owningTeacher()));
        when(chatMessageRepository.findById(question.getId())).thenReturn(Optional.empty());

        send("{\"type\":\"QA_MARK_ANSWERED\",\"messageId\":\"" + question.getId() + "\"}");

        verify(chatMessageRepository, never()).save(any());
        assertTrue(lastFrame().contains("Message not found"), lastFrame());
    }

    // ------------------------------------------------------------------

    private void send(String json) throws Exception {
        handler.handleTextMessage(session, new TextMessage(json));
    }

    private User student() {
        User user = new User();
        user.setRole(User.Role.STUDENT);
        return user;
    }

    /** Puts this session into the class room set so broadcasts actually reach it. */
    private void joinRoom() throws Exception {
        Field field = LiveClassWebSocketHandler.class.getDeclaredField("classSessions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Set<String>> rooms = (Map<UUID, Set<String>>) field.get(handler);
        rooms.put(classId, new HashSet<>(Set.of("s1")));
    }

    private String lastFrame() throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        return captor.getValue().getPayload();
    }

    /** Teacher profile whose id is stored as live_classes.teacher_id (ownership match). */
    private Teacher owningTeacher() {
        Teacher teacher = Teacher.builder().userId(teacherUserId).build();
        teacher.setId(teacherId);
        return teacher;
    }
}
