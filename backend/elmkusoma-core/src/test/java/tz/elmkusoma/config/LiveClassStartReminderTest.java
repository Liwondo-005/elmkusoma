package tz.elmkusoma.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Live class start reminders.
 *
 * <p>Events already had a reminder sweep; live classes had none, so a learner who booked a session
 * only ever learned about it by opening the app. The audience is derived server-side from the
 * institution membership (never from request input), the hosting teacher is excluded, and the
 * (user, class, offset) guard makes the 5-minute sweep idempotent.
 */
@ExtendWith(MockitoExtension.class)
class LiveClassStartReminderTest {

    @Mock private EventPublisherService eventPublisherService;
    @Mock private tz.elmkusoma.administration.repository.AdminDelegationRepository delegationRepository;
    @Mock private tz.elmkusoma.administration.repository.PlatformConfigRepository configRepository;
    @Mock private tz.elmkusoma.administration.service.DataGovernanceService dataGovernanceService;
    @Mock private tz.elmkusoma.administration.service.PlatformAdminService platformAdminService;
    @Mock private tz.elmkusoma.event.repository.EventRepository eventRepository;
    @Mock private tz.elmkusoma.event.repository.EventRegistrationRepository registrationRepository;
    @Mock private LearnerNotificationRepository learnerNotificationRepository;
    @Mock private tz.elmkusoma.identity.repository.TokenCleanupRepository tokenCleanupRepository;
    @Mock private NotificationService notificationService;
    @Mock private tz.elmkusoma.oversight.service.OversightAnnouncementService oversightAnnouncementService;
    @Mock private tz.elmkusoma.course.service.LiveClassService liveClassService;
    @Mock private LiveClassRepository liveClassRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private InstitutionMembershipRepository membershipRepository;

    @InjectMocks
    private CoreScheduler scheduler;

    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    /** LiveClass.teacherId is a teachers.id; the teacher user id is what membership holds. */
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID TEACHER_USER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID OTHER_LEARNER_ID = UUID.randomUUID();
    private static final UUID PARENT_ID = UUID.randomUUID();

    private LiveClass liveClass;

    @BeforeEach
    void setUp() {
        liveClass = new LiveClass();
        liveClass.setId(CLASS_ID);
        liveClass.setInstitutionId(INSTITUTION_ID);
        liveClass.setTeacherId(TEACHER_ID);
        liveClass.setTitle("Fractions");
        liveClass.setStatus("SCHEDULED");
        liveClass.setScheduledAt(LocalDateTime.now().plusMinutes(10));
    }

    private void stubTeacher() {
        Teacher teacher = new Teacher();
        teacher.setId(TEACHER_ID);
        teacher.setUserId(TEACHER_USER_ID);
        when(teacherRepository.findById(TEACHER_ID)).thenReturn(java.util.Optional.of(teacher));
    }

    private InstitutionMembership member(UUID userId, InstitutionMembership.Role role) {
        InstitutionMembership m = new InstitutionMembership();
        m.setUserId(userId);
        m.setInstitutionId(INSTITUTION_ID);
        m.setRole(role);
        return m;
    }

    private void stubMemberships() {
        when(membershipRepository.findByInstitutionIdAndIsActiveTrue(INSTITUTION_ID))
                .thenReturn(List.of(
                        member(STUDENT_ID, InstitutionMembership.Role.STUDENT),
                        member(OTHER_LEARNER_ID, InstitutionMembership.Role.OTHER_LEARNER),
                        member(PARENT_ID, InstitutionMembership.Role.PARENT)));
    }

    @Test
    void dueSoon_RemindsStudentsAndOtherLearners() {
        stubTeacher();
        when(liveClassRepository.findByStatusInAndIsDeletedFalse(List.of("SCHEDULED")))
                .thenReturn(List.of(liveClass));
        stubMemberships();
        when(learnerNotificationRepository
                .existsByUserIdAndTargetIdAndNotificationTypeAndIsDeletedFalse(any(), any(), any()))
                .thenReturn(false);

        scheduler.sendLiveClassStartReminders();

        // 10 minutes out is inside the 15-minute offset window.
        verify(notificationService).notifyUser(eq(STUDENT_ID), anyString(), anyString(),
                eq("EVENT_REMINDER_15M"), eq("live_class"), eq(CLASS_ID));
        verify(notificationService).notifyUser(eq(OTHER_LEARNER_ID), anyString(), anyString(),
                eq("EVENT_REMINDER_15M"), eq("live_class"), eq(CLASS_ID));
        // A parent is not a learner seat for this content and must not be pulled in.
        verify(notificationService, never()).notifyUser(eq(PARENT_ID), any(), any(), any(), any(), any());
    }

    @Test
    void alreadyRemindedOffset_IsNotSentAgain() {
        stubTeacher();
        when(liveClassRepository.findByStatusInAndIsDeletedFalse(List.of("SCHEDULED")))
                .thenReturn(List.of(liveClass));
        stubMemberships();
        when(learnerNotificationRepository
                .existsByUserIdAndTargetIdAndNotificationTypeAndIsDeletedFalse(any(), any(), any()))
                .thenReturn(true);

        scheduler.sendLiveClassStartReminders();

        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void hostingTeacher_IsNotRemindedAboutTheirOwnClass() {
        stubTeacher();
        when(liveClassRepository.findByStatusInAndIsDeletedFalse(List.of("SCHEDULED")))
                .thenReturn(List.of(liveClass));
        when(membershipRepository.findByInstitutionIdAndIsActiveTrue(INSTITUTION_ID))
                .thenReturn(List.of(
                        member(TEACHER_USER_ID, InstitutionMembership.Role.STUDENT),
                        member(STUDENT_ID, InstitutionMembership.Role.STUDENT)));
        when(learnerNotificationRepository
                .existsByUserIdAndTargetIdAndNotificationTypeAndIsDeletedFalse(any(), any(), any()))
                .thenReturn(false);

        scheduler.sendLiveClassStartReminders();

        verify(notificationService, never()).notifyUser(eq(TEACHER_USER_ID), any(), any(), any(), any(), any());
        verify(notificationService, times(1)).notifyUser(eq(STUDENT_ID), any(), any(), any(), any(), any());
    }

    @Test
    void classStartingTooFarAway_SendsNothing() {
        liveClass.setScheduledAt(LocalDateTime.now().plusHours(5));

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(List.of("SCHEDULED")))
                .thenReturn(List.of(liveClass));

        scheduler.sendLiveClassStartReminders();

        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
        // Membership is not even resolved when no offset is due.
        verify(membershipRepository, never()).findByInstitutionIdAndIsActiveTrue(any());
    }

    @Test
    void brokenNotificationChannel_DoesNotFailTheSweep() {
        stubTeacher();
        when(liveClassRepository.findByStatusInAndIsDeletedFalse(List.of("SCHEDULED")))
                .thenReturn(List.of(liveClass));
        stubMemberships();
        when(learnerNotificationRepository
                .existsByUserIdAndTargetIdAndNotificationTypeAndIsDeletedFalse(any(), any(), any()))
                .thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("channel down"))
                .when(notificationService).notifyUser(any(), any(), any(), any(), any(), any());

        // A notification failure must never stop the scheduled sweep.
        assertDoesNotThrow(() -> scheduler.sendLiveClassStartReminders());
    }
}
