package tz.elmkusoma.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassAttendanceDetailRepository;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
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
