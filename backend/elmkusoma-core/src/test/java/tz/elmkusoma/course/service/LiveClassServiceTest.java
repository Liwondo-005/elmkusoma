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
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.domain.LiveClass.LiveClassStatus;
import tz.elmkusoma.course.domain.LiveClassSessionType;
import tz.elmkusoma.course.dto.CreateLiveClassRequest;
import tz.elmkusoma.course.dto.LiveClassResponse;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
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
    @Mock private AttendanceRecordRepository attendanceRecordRepository;
    @Mock private CertificateRepository certificateRepository;

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
        when(participantRepository.findByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(liveClassId))
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
