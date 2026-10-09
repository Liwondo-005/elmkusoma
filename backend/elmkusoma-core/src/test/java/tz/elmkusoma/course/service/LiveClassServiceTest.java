package tz.elmkusoma.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.domain.LiveClass.LiveClassStatus;
import tz.elmkusoma.course.domain.LiveClassSessionType;
import tz.elmkusoma.course.dto.CreateLiveClassRequest;
import tz.elmkusoma.course.dto.LiveClassResponse;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.LiveClassAttendanceDetailRepository;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveClassServiceTest {

    @Mock private LiveClassRepository liveClassRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private LiveClassParticipantRepository participantRepository;
    @Mock private LiveClassAttendanceDetailRepository attendanceDetailRepository;
    @Mock private AttendanceRecordRepository attendanceRecordRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private StudentRepository studentRepository;
    @Mock private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Mock private tz.elmkusoma.liveclass.repository.MediaAssetRepository mediaAssetRepository;
    @Mock private ReplayRepository replayRepository;
    @Mock private tz.elmkusoma.liveclass.service.LiveKitService liveKitService;

    @InjectMocks
    private LiveClassServiceImpl liveClassService;

    private UUID teacherId;
    private UUID institutionId;
    private UUID liveClassId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        liveClassId = UUID.randomUUID();
        // Each expiry runs in its own REQUIRES_NEW transaction; without a real manager the
        // template cannot execute, so hand it a no-op status.
        lenient().when(transactionManager.getTransaction(any()))
                .thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
    }

    /**
     * Authoritative expiry: a started class past scheduledAt + duration must end
     * server-side, so a 60-minute class starting at 10:00 ends at 11:00 regardless of
     * refresh, reconnect or client state.
     */
    @Test
    void endExpiredSessions_shouldEndLiveClassPastItsDuration() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.IN_PROGRESS.name());
        live.setScheduledAt(LocalDateTime.now().minusMinutes(61));
        live.setDurationMinutes(60);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any()))
                .thenReturn(List.of(live));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        int ended = liveClassService.endExpiredSessions();

        assertEquals(1, ended);
        assertEquals(LiveClassStatus.ENDED.name(), live.getStatus());
        verify(liveClassRepository).save(live);
    }

    @Test
    void endExpiredSessions_shouldNotEndClassStillWithinItsDuration() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.IN_PROGRESS.name());
        live.setScheduledAt(LocalDateTime.now().minusMinutes(10));
        live.setDurationMinutes(60);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any()))
                .thenReturn(List.of(live));

        assertEquals(0, liveClassService.endExpiredSessions());
        assertEquals(LiveClassStatus.IN_PROGRESS.name(), live.getStatus());
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    /** A class that never started is not a running session: no fabricated attendance. */
    @Test
    void endExpiredSessions_shouldIgnoreClassesThatNeverStarted() {
        LiveClass scheduled = buildLiveClass();
        scheduled.setStatus(LiveClassStatus.SCHEDULED.name());
        scheduled.setScheduledAt(LocalDateTime.now().minusHours(3));

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any()))
                .thenReturn(Collections.emptyList());

        assertEquals(0, liveClassService.endExpiredSessions());
        assertEquals(LiveClassStatus.SCHEDULED.name(), scheduled.getStatus());
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    /** One failing session must not abort the sweep for the rest. */
    @Test
    void endExpiredSessions_shouldContinueAfterOneClassFails() {
        LiveClass broken = buildLiveClass();
        broken.setId(UUID.randomUUID());
        broken.setStatus(LiveClassStatus.LIVE.name());
        broken.setScheduledAt(LocalDateTime.now().minusHours(2));
        broken.setDurationMinutes(60);

        LiveClass healthy = buildLiveClass();
        healthy.setId(UUID.randomUUID());
        healthy.setStatus(LiveClassStatus.LIVE.name());
        healthy.setScheduledAt(LocalDateTime.now().minusHours(2));
        healthy.setDurationMinutes(60);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any()))
                .thenReturn(List.of(broken, healthy));
        when(liveClassRepository.save(any(LiveClass.class)))
                .thenAnswer(inv -> {
                    LiveClass c = inv.getArgument(0);
                    if (broken.getId().equals(c.getId())) {
                        throw new IllegalStateException("boom");
                    }
                    return c;
                });

        int ended = liveClassService.endExpiredSessions();

        assertEquals(1, ended);
        assertEquals(LiveClassStatus.ENDED.name(), healthy.getStatus());
        // Each session gets its own transaction: a shared one would be marked rollback-only by
        // the failure and take every later session down with it (observed in production).
        verify(transactionManager, times(2)).getTransaction(any());
    }

    /**
     * Regression test for a production failure that unit tests had masked: certificates.issued_by
     * is NOT NULL, and an automatic expiry passes no pressing teacher, so the certificate insert
     * violated the constraint. That aborted the entire sweep, so no live class was ever
     * auto-completed (11 sessions were stuck IN_PROGRESS). The issuer must fall back to the
     * teacher who ran the session.
     */
    @Test
    void endExpiredSessions_shouldIssueCertificatesWithANonNullIssuer() {
        UUID classGroupId = UUID.randomUUID();
        UUID participantUserId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        LiveClass live = buildLiveClass();
        live.setClassGroupId(classGroupId);
        live.setStatus(LiveClassStatus.LIVE.name());
        live.setScheduledAt(LocalDateTime.now().minusHours(2));
        live.setStartedAt(LocalDateTime.now().minusMinutes(61));
        live.setDurationMinutes(60);

        LiveClassParticipant participant = new LiveClassParticipant();
        participant.setId(UUID.randomUUID());
        participant.setUserId(participantUserId);
        participant.setIsDeleted(false);

        Student student = new Student();
        student.setId(studentId);
        student.setUserId(participantUserId);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(List.of(live));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepository.findByLiveClassIdAndIsDeletedFalse(liveClassId))
                .thenReturn(List.of(participant));
        when(studentRepository.findByUserIdAndIsDeletedFalse(participantUserId))
                .thenReturn(Optional.of(student));
        when(userRepository.findById(participantUserId)).thenReturn(Optional.of(
                User.builder().id(participantUserId).firstName("Test").lastName("Student").build()));
        when(attendanceRecordRepository.findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(
                any(), any())).thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(certificateRepository.findAllByStudentId(studentId)).thenReturn(Collections.emptyList());

        org.mockito.ArgumentCaptor<tz.elmkusoma.certificate.domain.Certificate> certCaptor =
                org.mockito.ArgumentCaptor.forClass(tz.elmkusoma.certificate.domain.Certificate.class);
        when(certificateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, liveClassService.endExpiredSessions());

        verify(certificateRepository).save(certCaptor.capture());
        tz.elmkusoma.certificate.domain.Certificate cert = certCaptor.getValue();
        assertNotNull(cert.getIssuedBy(), "certificates.issued_by is NOT NULL - a null issuer aborts the sweep");
        assertEquals(teacherId, cert.getIssuedBy(), "an automatic expiry is issued by the session's teacher");
    }

    @Test
    void getTeacherLiveClasses_shouldReturnList() {
        LiveClass lc = buildLiveClass();
        when(liveClassRepository.findByTeacherIdAndIsDeletedFalse(teacherId))
                .thenReturn(List.of(lc));

        List<LiveClassResponse> result = liveClassService.getTeacherLiveClasses(teacherId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Mathematics - Live Session", result.get(0).getTitle());
    }

    @Test
    void getTeacherLiveClasses_shouldFilterCancelled() {
        LiveClass active = buildLiveClass();
        LiveClass cancelled = buildLiveClass();
        cancelled.setStatus("CANCELLED");

        when(liveClassRepository.findByTeacherIdAndIsDeletedFalse(teacherId))
                .thenReturn(List.of(active, cancelled));

        List<LiveClassResponse> result = liveClassService.getTeacherLiveClasses(teacherId);

        assertEquals(1, result.size());
    }

    @Test
    void createLiveClass_shouldSaveAndReturn() {
        Teacher teacher = Teacher.builder().userId(UUID.randomUUID()).build();
        teacher.setId(teacherId);
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setTitle("Physics 101");
        request.setScheduledAt(LocalDateTime.now().plusDays(1).toString());
        request.setDurationMinutes(60);
        request.setMaxParticipants(30);
        request.setSessionType("LECTURE");
        request.setTimezone("Africa/Dar_es_Salaam");

        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> {
            LiveClass saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        LiveClassResponse response = liveClassService.createLiveClass(teacherId, institutionId, request);

        assertNotNull(response);
        assertEquals("Physics 101", response.getTitle());
        assertEquals(60, response.getDurationMinutes());
        verify(liveClassRepository).save(any(LiveClass.class));
    }

    @Test
    void createLiveClass_withoutBroadcastSource_shouldDefaultToBrowser() {
        Teacher teacher = Teacher.builder().userId(UUID.randomUUID()).build();
        teacher.setId(teacherId);
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setTitle("Chemistry 101");
        request.setScheduledAt(LocalDateTime.now().plusDays(1).toString());
        request.setDurationMinutes(60);

        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> {
            LiveClass saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        LiveClassResponse response = liveClassService.createLiveClass(teacherId, institutionId, request);

        // Older clients that never send broadcastSource keep the exact pre-existing
        // behaviour: a browser/laptop-camera sourced session.
        assertEquals("BROWSER", response.getBroadcastSource());
    }

    @Test
    void createLiveClass_withExternalUsbCamera_shouldEchoSource() {
        Teacher teacher = Teacher.builder().userId(UUID.randomUUID()).build();
        teacher.setId(teacherId);
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setTitle("Lab Demo");
        request.setScheduledAt(LocalDateTime.now().plusDays(1).toString());
        request.setDurationMinutes(45);
        request.setBroadcastSource("USB_CAMERA");

        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> {
            LiveClass saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        LiveClassResponse response = liveClassService.createLiveClass(teacherId, institutionId, request);

        assertEquals("USB_CAMERA", response.getBroadcastSource());
    }

    @Test
    void createLiveClass_whenUnknownBroadcastSource_shouldThrowWithSupportedList() {
        Teacher teacher = Teacher.builder().userId(UUID.randomUUID()).build();
        teacher.setId(teacherId);
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setTitle("Bad source");
        request.setScheduledAt(LocalDateTime.now().plusDays(1).toString());
        request.setDurationMinutes(60);
        request.setBroadcastSource("TOASTER");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> liveClassService.createLiveClass(teacherId, institutionId, request));
        assertTrue(ex.getMessage().contains("USB_CAMERA"));
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    @Test
    void updateLiveClass_shouldChangeBroadcastSource() {
        LiveClass lc = buildLiveClass();
        lc.setStatus("SCHEDULED");
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));
        when(liveClassRepository.findOverlappingForTeacher(eq(teacherId), any(), any()))
                .thenReturn(new java.util.ArrayList<>(List.of()));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setBroadcastSource("OBS");

        LiveClassResponse response = liveClassService.updateLiveClass(teacherId, liveClassId, request);

        assertEquals("OBS", response.getBroadcastSource());
    }

    @Test
    void updateLiveClass_whenInProgress_shouldRejectSourceChange() {
        LiveClass lc = buildLiveClass();
        lc.setStatus("IN_PROGRESS");
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setBroadcastSource("STUDIO");

        assertThrows(IllegalArgumentException.class,
                () -> liveClassService.updateLiveClass(teacherId, liveClassId, request));
    }

    @Test
    void createLiveClass_whenTeacherNotFound_shouldThrow() {
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.empty());

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setTitle("Test");
        request.setScheduledAt(LocalDateTime.now().plusDays(1).toString());
        request.setDurationMinutes(60);

        assertThrows(ResourceNotFoundException.class,
                () -> liveClassService.createLiveClass(teacherId, institutionId, request));
    }

    @Test
    void startSession_shouldSetInProgress() {
        LiveClass lc = buildLiveClass();
        lc.setStatus("SCHEDULED");
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        LiveClassResponse response = liveClassService.startSession(teacherId, liveClassId);

        assertNotNull(response);
        assertEquals("IN_PROGRESS", response.getStatus());
    }

    @Test
    void startSession_whenNotScheduled_shouldThrow() {
        LiveClass lc = buildLiveClass();
        lc.setStatus("IN_PROGRESS");
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));

        assertThrows(IllegalArgumentException.class,
                () -> liveClassService.startSession(teacherId, liveClassId));
    }

    @Test
    void endSession_shouldSetCompleted() {
        LiveClass lc = buildLiveClass();
        lc.setStatus("IN_PROGRESS");
        lc.setClassGroupId(UUID.randomUUID());
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepository.findByLiveClassIdAndIsDeletedFalse(liveClassId))
                .thenReturn(Collections.emptyList());

        LiveClassResponse response = liveClassService.endSession(teacherId, liveClassId, teacherId);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
    }

    @Test
    void cancelLiveClass_shouldSetCancelled() {
        LiveClass lc = buildLiveClass();
        lc.setStatus("SCHEDULED");
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        liveClassService.cancelLiveClass(teacherId, liveClassId);

        assertEquals("CANCELLED", lc.getStatus());
        verify(liveClassRepository).save(lc);
    }

    @Test
    void cancelLiveClass_whenInProgress_shouldSucceed() {
        LiveClass lc = buildLiveClass();
        lc.setStatus("IN_PROGRESS");
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        liveClassService.cancelLiveClass(teacherId, liveClassId);

        assertEquals("CANCELLED", lc.getStatus());
    }

    @Test
    void getLiveClassById_shouldReturn() {
        LiveClass lc = buildLiveClass();
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));

        LiveClassResponse response = liveClassService.getLiveClassById(liveClassId);

        assertNotNull(response);
        assertEquals("Mathematics - Live Session", response.getTitle());
    }

    @Test
    void getLiveClassById_whenNotFound_shouldThrow() {
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> liveClassService.getLiveClassById(liveClassId));
    }

    @Test
    void getUpcomingClasses_shouldReturnScheduled() {
        LiveClass lc = buildLiveClass();
        when(liveClassRepository.findByInstitutionIdAndIsDeletedFalse(institutionId))
                .thenReturn(List.of(lc));

        List<LiveClassResponse> result = liveClassService.getUpcomingClasses(institutionId);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // ---- Lesson ↔ Live Class (link/unlink, authorization, duplicate guard) ----

    @Test
    void linkLesson_shouldLinkAuthorizedLesson() {
        UUID classGroupId = UUID.randomUUID();
        LiveClass lc = buildLiveClass();
        lc.setClassGroupId(classGroupId);
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));

        Lesson lesson = buildLessonRow(institutionId, classGroupId, "Geography: Map Reading");
        UUID lessonId = lesson.getId();
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(teacherAssignmentRepository.findClassGroupIdsByTeacherId(teacherId))
                .thenReturn(List.of(classGroupId));
        when(liveClassRepository.findByLessonIdAndIsDeletedFalse(lessonId)).thenReturn(List.of());
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        LiveClassResponse response = liveClassService.linkLesson(teacherId, liveClassId, lessonId);

        assertNotNull(response);
        assertEquals(lessonId, response.getLessonId());
        assertEquals("Geography: Map Reading", response.getLessonTitle());
        verify(liveClassRepository).save(lc);
    }

    @Test
    void linkLesson_whenLessonFromDifferentInstitution_shouldThrow() {
        LiveClass lc = buildLiveClass();
        lc.setClassGroupId(null);
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));

        Lesson foreign = buildLessonRow(UUID.randomUUID(), null, "Foreign lesson");
        when(lessonRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> liveClassService.linkLesson(teacherId, liveClassId, foreign.getId()));
        assertTrue(ex.getMessage().contains("different institution"));
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    @Test
    void linkLesson_whenLessonClassMismatch_shouldThrow() {
        LiveClass lc = buildLiveClass();
        lc.setClassGroupId(UUID.randomUUID());
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));

        Lesson lesson = buildLessonRow(institutionId, UUID.randomUUID(), "Other class lesson");
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> liveClassService.linkLesson(teacherId, liveClassId, lesson.getId()));
        assertTrue(ex.getMessage().contains("different class"));
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    @Test
    void linkLesson_whenTeacherNotAssignedToLessonClass_shouldThrow() {
        UUID classGroupId = UUID.randomUUID();
        LiveClass lc = buildLiveClass();
        lc.setClassGroupId(classGroupId);
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));

        Lesson lesson = buildLessonRow(institutionId, classGroupId, "Colleague lesson");
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(teacherAssignmentRepository.findClassGroupIdsByTeacherId(teacherId))
                .thenReturn(List.of(UUID.randomUUID()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> liveClassService.linkLesson(teacherId, liveClassId, lesson.getId()));
        assertTrue(ex.getMessage().contains("not assigned"));
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    @Test
    void linkLesson_whenLessonAlreadyLinkedToActiveClass_shouldThrow() {
        UUID classGroupId = UUID.randomUUID();
        LiveClass lc = buildLiveClass();
        lc.setClassGroupId(classGroupId);
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));

        Lesson lesson = buildLessonRow(institutionId, classGroupId, "Duplicated lesson");
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(teacherAssignmentRepository.findClassGroupIdsByTeacherId(teacherId))
                .thenReturn(List.of(classGroupId));

        LiveClass other = buildLiveClass();
        other.setId(UUID.randomUUID());
        other.setTitle("Existing scheduled class");
        other.setStatus(LiveClassStatus.SCHEDULED.name());
        when(liveClassRepository.findByLessonIdAndIsDeletedFalse(lesson.getId()))
                .thenReturn(List.of(other));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> liveClassService.linkLesson(teacherId, liveClassId, lesson.getId()));
        assertTrue(ex.getMessage().contains("already linked"));
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    @Test
    void linkLesson_whenExistingLinkIsTerminal_shouldAllow() {
        UUID classGroupId = UUID.randomUUID();
        LiveClass lc = buildLiveClass();
        lc.setClassGroupId(classGroupId);
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));

        Lesson lesson = buildLessonRow(institutionId, classGroupId, "Re-linked lesson");
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(teacherAssignmentRepository.findClassGroupIdsByTeacherId(teacherId))
                .thenReturn(List.of(classGroupId));

        LiveClass past = buildLiveClass();
        past.setId(UUID.randomUUID());
        past.setStatus(LiveClassStatus.COMPLETED.name());
        when(liveClassRepository.findByLessonIdAndIsDeletedFalse(lesson.getId()))
                .thenReturn(List.of(past));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        LiveClassResponse response = liveClassService.linkLesson(teacherId, liveClassId, lesson.getId());

        assertEquals(lesson.getId(), response.getLessonId());
        verify(liveClassRepository).save(lc);
    }

    @Test
    void unlinkLesson_shouldClearLessonLink() {
        UUID lessonId = UUID.randomUUID();
        LiveClass lc = buildLiveClass();
        lc.setLessonId(lessonId);
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        LiveClassResponse response = liveClassService.unlinkLesson(teacherId, liveClassId);

        assertNull(response.getLessonId());
        assertNull(lc.getLessonId());
        verify(liveClassRepository).save(lc);
    }

    @Test
    void unlinkLesson_whenNoLinkedLesson_shouldThrow() {
        LiveClass lc = buildLiveClass();
        lc.setLessonId(null);
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(lc));

        assertThrows(IllegalArgumentException.class,
                () -> liveClassService.unlinkLesson(teacherId, liveClassId));
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    @Test
    void createLiveClass_withCrossInstitutionLesson_shouldThrow() {
        Teacher teacher = Teacher.builder().userId(UUID.randomUUID()).build();
        teacher.setId(teacherId);
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        Lesson foreign = buildLessonRow(UUID.randomUUID(), null, "Foreign lesson");
        when(lessonRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setTitle("Linked session");
        request.setScheduledAt(LocalDateTime.now().plusDays(1).toString());
        request.setDurationMinutes(45);
        request.setLessonId(foreign.getId());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> liveClassService.createLiveClass(teacherId, institutionId, request));
        assertTrue(ex.getMessage().contains("different institution"));
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    // ------------------------------------------------------------------------------------------
    // Actual-start expiry (V135): expiry = startedAt + durationMinutes, NOT scheduledAt + duration.
    // Reference scenario used throughout: scheduled 10:00, duration 5m, teacher starts at 10:02.
    // Expected end 10:07. The old scheduledAt baseline ended it at 10:05 (3 minutes instead of 5).
    // ------------------------------------------------------------------------------------------

    /** The start timestamp comes from the server clock; the client cannot supply it. */
    @Test
    void startSession_shouldStampAuthoritativeServerStartedAt() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.SCHEDULED.name());
        assertNull(live.getStartedAt());

        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(live));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        liveClassService.startSession(teacherId, liveClassId);
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(live.getStartedAt());
        assertFalse(live.getStartedAt().isBefore(before), "startedAt must be >= request start");
        assertFalse(live.getStartedAt().isAfter(after), "startedAt must be <= request end");
        assertEquals(LiveClassStatus.IN_PROGRESS.name(), live.getStatus());
    }

    /**
     * Re-entry must not re-stamp: otherwise a refresh that replays "start" would extend the
     * session indefinitely and expiry would never arrive.
     */
    @Test
    void startSession_shouldNotOverwriteAnAlreadyEstablishedStartedAt() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.SCHEDULED.name());
        LocalDateTime originalStart = LocalDateTime.now().minusMinutes(3);
        live.setStartedAt(originalStart);

        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(live));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        liveClassService.startSession(teacherId, liveClassId);

        assertEquals(originalStart, live.getStartedAt());
    }

    /** A client cannot start someone else's class, so it cannot forge a start. */
    @Test
    void startSession_forAnotherTeacher_shouldThrowAndLeaveStartedAtUnset() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.SCHEDULED.name());
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(live));

        assertThrows(ResourceNotFoundException.class,
                () -> liveClassService.startSession(UUID.randomUUID(), liveClassId));
        assertNull(live.getStartedAt());
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    /** Started 10:02, duration 5m. At 10:06 (4 minutes in) the session is still live. */
    @Test
    void endExpiredSessions_shouldKeepSessionRunningBeforeActualStartPlusDuration() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.IN_PROGRESS.name());
        live.setStartedAt(LocalDateTime.now().minusMinutes(4));
        live.setDurationMinutes(5);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(List.of(live));

        assertEquals(0, liveClassService.endExpiredSessions());
        assertEquals(LiveClassStatus.IN_PROGRESS.name(), live.getStatus());
    }

    /** Started 10:02, duration 5m. From 10:07 on, the sweep ends it without the teacher. */
    @Test
    void endExpiredSessions_shouldEndSessionOnceActualStartPlusDurationHasElapsed() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.IN_PROGRESS.name());
        live.setStartedAt(LocalDateTime.now().minusMinutes(5).minusSeconds(10));
        live.setDurationMinutes(5);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(List.of(live));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, liveClassService.endExpiredSessions());
        assertEquals(LiveClassStatus.ENDED.name(), live.getStatus());
    }

    /**
     * The core correction: scheduled 10:00 with a 5m duration that the teacher started at 10:02.
     * At 10:06 the OLD baseline (scheduledAt + duration = 10:05) would already have ended it.
     * It must still be running, because 10:07 is the real deadline.
     */
    @Test
    void endExpiredSessions_shouldNotUseScheduledAtBaselineWhenTeacherStartedLate() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.IN_PROGRESS.name());
        live.setScheduledAt(LocalDateTime.now().minusMinutes(6)); // "scheduled 10:00"
        live.setStartedAt(LocalDateTime.now().minusMinutes(4));   // "started 10:02" -> ends 10:07
        live.setDurationMinutes(5);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(List.of(live));

        assertEquals(0, liveClassService.endExpiredSessions());
        assertEquals(LiveClassStatus.IN_PROGRESS.name(), live.getStatus());
        verify(liveClassRepository, never()).save(any(LiveClass.class));
    }

    /** Same class, now past 10:07: the late start gets its full 5 minutes, not 3. */
    @Test
    void endExpiredSessions_shouldEndLateStartedSessionAtStartedAtPlusDuration() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.IN_PROGRESS.name());
        live.setScheduledAt(LocalDateTime.now().minusMinutes(7)); // "scheduled 10:00"
        live.setStartedAt(LocalDateTime.now().minusMinutes(5).minusSeconds(5)); // "started 10:02" -> 10:07
        live.setDurationMinutes(5);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(List.of(live));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, liveClassService.endExpiredSessions());
        assertEquals(LiveClassStatus.ENDED.name(), live.getStatus());
    }

    /**
     * A never-started class is not a running session. The sweep must not even consider
     * SCHEDULED rows, so scheduledAt + duration < now can never fabricate attendance/replay.
     */
    @Test
    void endExpiredSessions_shouldNeverQueryScheduledOrTerminalClasses() {
        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());

        assertEquals(0, liveClassService.endExpiredSessions());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(liveClassRepository).findByStatusInAndIsDeletedFalse(captor.capture());
        List<String> statuses = captor.getValue();
        assertFalse(statuses.contains(LiveClassStatus.SCHEDULED.name()));
        assertFalse(statuses.contains(LiveClassStatus.ENDED.name()));
        assertFalse(statuses.contains(LiveClassStatus.COMPLETED.name()));
    }

    /**
     * Refresh/reconnect safety: reading the class exposes startedAt but never mutates it,
     * so a browser reload cannot reset, restart or extend the countdown.
     */
    @Test
    void readingTheClass_shouldNotResetOrExtendTheExpiryBaseline() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.IN_PROGRESS.name());
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(2);
        live.setStartedAt(startedAt);
        live.setDurationMinutes(5);
        when(liveClassRepository.findByTeacherIdAndIsDeletedFalse(teacherId)).thenReturn(List.of(live));

        LiveClassResponse response = liveClassService.getTeacherLiveClasses(teacherId).get(0);

        assertEquals(startedAt, live.getStartedAt(), "a read must not move startedAt");
        assertEquals(startedAt.toString(), response.getStartedAt(), "startedAt must reach the client");
        assertEquals(LiveClassStatus.IN_PROGRESS.name(), live.getStatus());
    }

    /**
     * Legacy active sessions (started before started_at existed) keep the previous scheduledAt
     * baseline so no in-flight session is stranded forever.
     */
    @Test
    void endExpiredSessions_shouldFallBackToScheduledAtForLegacySessionsWithoutStartedAt() {
        LiveClass legacy = buildLiveClass();
        legacy.setId(UUID.randomUUID());
        legacy.setStatus(LiveClassStatus.IN_PROGRESS.name());
        legacy.setScheduledAt(LocalDateTime.now().minusHours(2)); // pre-V135 active row
        legacy.setStartedAt(null);
        legacy.setDurationMinutes(60);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(List.of(legacy));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, liveClassService.endExpiredSessions());
        assertEquals(LiveClassStatus.ENDED.name(), legacy.getStatus());
    }

    /** A legacy active session still inside its window is left alone. */
    @Test
    void endExpiredSessions_shouldNotEndLegacySessionStillWithinItsDuration() {
        LiveClass legacy = buildLiveClass();
        legacy.setId(UUID.randomUUID());
        legacy.setStatus(LiveClassStatus.LIVE.name());
        legacy.setScheduledAt(LocalDateTime.now().minusMinutes(10));
        legacy.setStartedAt(null);
        legacy.setDurationMinutes(60);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(List.of(legacy));

        assertEquals(0, liveClassService.endExpiredSessions());
        assertEquals(LiveClassStatus.LIVE.name(), legacy.getStatus());
    }

    /** Missing/zero duration falls back to 60 minutes rather than ending immediately. */
    @Test
    void endExpiredSessions_shouldUseDefaultDurationWhenDurationIsMissing() {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.IN_PROGRESS.name());
        live.setStartedAt(LocalDateTime.now().minusMinutes(59));
        live.setDurationMinutes(null);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(List.of(live));

        assertEquals(0, liveClassService.endExpiredSessions(), "default 60m window must not expire at 59m");
        assertEquals(LiveClassStatus.IN_PROGRESS.name(), live.getStatus());
    }

    /**
     * Auto-expiry must reuse the single completion path: attendance and certificates are still
     * finalised, and they stay system-attributed (markedBy == null) because nobody pressed End.
     */
    @Test
    void endExpiredSessions_shouldStillFinaliseAttendanceAndCertificates() {
        UUID classGroupId = UUID.randomUUID();
        UUID participantUserId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        LiveClass live = buildLiveClass();
        live.setClassGroupId(classGroupId);
        live.setStatus(LiveClassStatus.LIVE.name());
        live.setScheduledAt(LocalDateTime.now().minusHours(2));
        live.setStartedAt(LocalDateTime.now().minusMinutes(61));
        live.setDurationMinutes(60);

        LiveClassParticipant participant = new LiveClassParticipant();
        participant.setId(UUID.randomUUID());
        participant.setUserId(participantUserId);
        participant.setIsDeleted(false);

        Student student = new Student();
        student.setId(studentId);
        student.setUserId(participantUserId);

        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(List.of(live));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepository.findByLiveClassIdAndIsDeletedFalse(liveClassId))
                .thenReturn(List.of(participant));
        when(studentRepository.findByUserIdAndIsDeletedFalse(participantUserId))
                .thenReturn(Optional.of(student));
        when(userRepository.findById(participantUserId)).thenReturn(Optional.of(
                User.builder().id(participantUserId).firstName("Test").lastName("Student").build()));
        when(attendanceRecordRepository.findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(
                any(), any())).thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(certificateRepository.findAllByStudentId(studentId)).thenReturn(Collections.emptyList());
        when(certificateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, liveClassService.endExpiredSessions());

        assertEquals(LiveClassStatus.ENDED.name(), live.getStatus());
        verify(attendanceRecordRepository).save(any());
        verify(certificateRepository).save(any());
    }

    // ------------------------------------------------------------------------------------------
    // Recording -> Media Library (RECORDING). The Replay row already existed; the Media Library
    // entry did not, so a finalized recording was invisible in the library.
    // ------------------------------------------------------------------------------------------

    /** A live class that has just ended, with a resolved (non-egress) recording URL. */
    private LiveClass endedRecordedClass(String recordingUrl, Boolean recordingEnabled) {
        LiveClass live = buildLiveClass();
        live.setStatus(LiveClassStatus.ENDED.name());
        live.setRecordingEnabled(recordingEnabled);
        live.setRecordingUrl(recordingUrl);
        live.setClassGroupId(UUID.randomUUID());
        return live;
    }

    /**
     * Drives the terminal path through the public API. completeSession is private, so the
     * expiry sweep is used: it reaches finalizeRecordingAndCreateReplay for any candidate
     * whose duration has elapsed.
     */
    private void triggerFinalization(LiveClass live) {
        // Keep the class in an active status with an elapsed window so the sweep picks it up.
        live.setStatus(LiveClassStatus.IN_PROGRESS.name());
        live.setStartedAt(java.time.LocalDateTime.now().minusHours(2));
        live.setDurationMinutes(60);
        when(liveClassRepository.findByStatusInAndIsDeletedFalse(any())).thenReturn(List.of(live));
        liveClassService.endExpiredSessions();
    }

    private void stubCompletionScaffolding(LiveClass live) {
        // All lenient: the negative tests below deliberately return before these are reached.
        lenient().when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(
                any(), any())).thenReturn(Collections.emptyList());
        lenient().when(replayRepository.findByLiveSessionIdAndIsDeletedFalse(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(replayRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(teacherRepository.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void finalizedRecording_shouldBePublishedToMediaLibrary() {
        LiveClass live = endedRecordedClass("https://cdn.test/rec.mp4", true);
        stubCompletionScaffolding(live);

        triggerFinalization(live);

        org.mockito.ArgumentCaptor<tz.elmkusoma.liveclass.domain.MediaAsset> captor =
                org.mockito.ArgumentCaptor.forClass(tz.elmkusoma.liveclass.domain.MediaAsset.class);
        verify(mediaAssetRepository).save(captor.capture());
        MediaAsset asset = captor.getValue();

        assertEquals("RECORDING", asset.getMediaType());
        assertEquals("https://cdn.test/rec.mp4", asset.getFileUrl());
        assertEquals("LIVE_CLASS", asset.getSourceType());
        assertEquals(liveClassId, asset.getSourceId(), "must be keyed on the originating live class");
        assertEquals(live.getClassGroupId(), asset.getClassGroupId(), "recording must target its class");
        assertEquals(institutionId, asset.getInstitutionId(), "ownership comes from the live class");
        assertEquals(live.getTitle(), asset.getTitle());
    }

    /**
     * Case D: finalization runs more than once (manual End followed by the expiry sweep, or a
     * reprocess). A second run must refresh the existing asset, never create a second one.
     */
    @Test
    void finalizedRecording_twice_shouldNotDuplicateMediaAsset() {
        LiveClass live = endedRecordedClass("https://cdn.test/rec.mp4", true);
        stubCompletionScaffolding(live);

        MediaAsset existing = MediaAsset.builder()
                .title("stale")
                .mediaType("RECORDING")
                .fileUrl("https://cdn.test/old.mp4")
                .sourceType("LIVE_CLASS")
                .sourceId(liveClassId)
                .build();
        existing.setId(UUID.randomUUID());
        existing.setInstitutionId(institutionId);
        when(mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse("LIVE_CLASS", liveClassId))
                .thenReturn(List.of(existing));

        triggerFinalization(live);

        // One save, and it updated the existing row rather than inserting.
        verify(mediaAssetRepository).save(same(existing));
        assertEquals("https://cdn.test/rec.mp4", existing.getFileUrl(), "existing entry must be refreshed");
        assertEquals(live.getClassGroupId(), existing.getClassGroupId());
    }

    /**
     * The transient "egress:<id>" marker is LiveKit bookkeeping, not a playable URL. It must
     * never reach the Media Library, and an unresolvable egress produces no library entry.
     */
    @Test
    void unresolvedEgress_shouldNotPublishMediaAsset() {
        LiveClass live = endedRecordedClass("egress:eg_12345", true);
        stubCompletionScaffolding(live);
        when(liveKitService.resolveRecordingUrl("eg_12345")).thenReturn(null);

        triggerFinalization(live);

        verify(mediaAssetRepository, never()).save(any());
    }

    /** Case A: recording disabled leaves the library untouched. */
    @Test
    void recordingDisabledWithNoUrl_shouldNotPublishMediaAsset() {
        LiveClass live = endedRecordedClass(null, false);
        stubCompletionScaffolding(live);

        triggerFinalization(live);

        verify(mediaAssetRepository, never()).save(any());
    }

    /** A non-http url is not a playable recording and must not be published. */
    @Test
    void nonHttpRecordingUrl_shouldNotPublishMediaAsset() {
        LiveClass live = endedRecordedClass("file:///tmp/rec.mp4", true);
        stubCompletionScaffolding(live);

        triggerFinalization(live);

        verify(mediaAssetRepository, never()).save(any());
    }

    /** The existing Replay behaviour must be unchanged by the media publication. */
    @Test
    void finalizedRecording_stillCreatesExactlyOneReplay() {
        LiveClass live = endedRecordedClass("https://cdn.test/rec.mp4", true);
        stubCompletionScaffolding(live);

        triggerFinalization(live);

        org.mockito.ArgumentCaptor<tz.elmkusoma.event.domain.Replay> captor =
                org.mockito.ArgumentCaptor.forClass(tz.elmkusoma.event.domain.Replay.class);
        verify(replayRepository).save(captor.capture());
        assertEquals(liveClassId, captor.getValue().getLiveSessionId());
        assertEquals("https://cdn.test/rec.mp4", captor.getValue().getRecordingUrl());
        assertEquals(institutionId, captor.getValue().getInstitutionId());
    }

    private Lesson buildLessonRow(UUID lessonInstitutionId, UUID classGroupId, String title) {
        Lesson lesson = new Lesson();
        lesson.setId(UUID.randomUUID());
        lesson.setTitle(title);
        lesson.setInstitutionId(lessonInstitutionId);
        lesson.setClassGroupId(classGroupId);
        lesson.setIsDeleted(false);
        return lesson;
    }

    private LiveClass buildLiveClass() {
        LiveClass lc = LiveClass.builder()
                .teacherId(teacherId)
                .title("Mathematics - Live Session")
                .description("Live session on algebra")
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .durationMinutes(60)
                .status(LiveClassStatus.SCHEDULED.name())
                .maxParticipants(50)
                .subjectId(UUID.randomUUID())
                .sessionType(LiveClassSessionType.LECTURE)
                .timezone("Africa/Dar_es_Salaam")
                .isRecurring(false)
                .lobbyEnabled(false)
                .recordingEnabled(false)
                .build();
        lc.setId(liveClassId);
        lc.setInstitutionId(institutionId);
        lc.setIsDeleted(false);

        Subject subject = Subject.builder().name("Mathematics").build();
        subject.setId(lc.getSubjectId());

        User user = User.builder().id(teacherId).firstName("Test").lastName("Teacher").build();

        lenient().when(userRepository.findById(teacherId)).thenReturn(Optional.of(user));
        lenient().when(subjectRepository.findById(any())).thenReturn(Optional.of(subject));

        return lc;
    }
}
