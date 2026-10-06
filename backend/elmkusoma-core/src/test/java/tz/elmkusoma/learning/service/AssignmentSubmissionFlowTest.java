package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.domain.AssignmentSubmission;
import tz.elmkusoma.learning.dto.request.SubmissionRequest;
import tz.elmkusoma.learning.dto.response.SubmissionResponse;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.learning.repository.LessonProgressRepository;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.service.impl.LearningServiceImpl;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Real-data submission flow: users.id → students.id resolution (the two ids are
 * distinct), submission_text/status persistence, window enforcement, resubmission
 * rules, teacher notification and audit writes.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentSubmissionFlowTest {

    @Mock private LessonRepository lessonRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private StudentClassAssignmentRepository studentClassAssignmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private ClassAccessGuard classAccessGuard;

    @InjectMocks
    private LearningServiceImpl service;

    private UUID assignmentId;
    private UUID institutionId;
    private UUID callerUserId;
    private UUID studentId;
    private UUID teacherUserId;

    @BeforeEach
    void setUp() {
        assignmentId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        callerUserId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        teacherUserId = UUID.randomUUID();
    }

    private Assignment assignment(LocalDateTime openDate, LocalDateTime closeDate,
                                  LocalDateTime dueDate, boolean allowLate) {
        return Assignment.builder()
                .id(assignmentId)
                .institutionId(institutionId)
                .subjectId(UUID.randomUUID())
                .classGroupId(UUID.randomUUID())
                .title("Essay 1")
                .openDate(openDate)
                .closeDate(closeDate)
                .dueDate(dueDate)
                .allowLateSubmission(allowLate)
                .status("PUBLISHED")
                .isDeleted(false)
                .build();
    }

    private Student student() {
        return Student.builder()
                .id(studentId)
                .userId(callerUserId)
                .status(StudentStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    private SubmissionRequest request(String content, Boolean draft) {
        SubmissionRequest request = new SubmissionRequest();
        request.setContent(content);
        request.setDraft(draft);
        return request;
    }

    @Test
    void submitAssignment_persistsContentForResolvedStudentProfile() {
        Assignment assignment = assignment(null, null, null, false);
        assignment.setCreatedBy("teacher@test.com");
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(submissionRepository.findByAssignmentIdAndStudentIdAndIsDeletedFalse(assignmentId, studentId))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmailAndIsDeletedFalse("teacher@test.com"))
                .thenReturn(Optional.of(User.builder().id(teacherUserId).build()));

        SubmissionResponse response = service.submitAssignment(
                assignmentId, callerUserId, institutionId, request("My answer", false));

        ArgumentCaptor<AssignmentSubmission> captor = ArgumentCaptor.forClass(AssignmentSubmission.class);
        verify(submissionRepository).save(captor.capture());
        AssignmentSubmission saved = captor.getValue();

        // The FK target is students(id), not users(id) — regression guard for the
        // FK violation that made submissions silently impossible before.
        assertEquals(studentId, saved.getStudentId());
        assertEquals(assignmentId, saved.getAssignmentId());
        assertEquals(institutionId, saved.getInstitutionId());
        assertEquals("My answer", saved.getSubmissionText());
        assertEquals("SUBMITTED", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getIsDraft());
        assertEquals(Boolean.FALSE, saved.getIsLate());
        assertNotNull(saved.getSubmittedAt());

        assertEquals("My answer", response.getSubmissionText());
        assertEquals("SUBMITTED", response.getStatus());
        assertEquals(studentId, response.getStudentId());

        verify(notificationService).notifyUser(
                eq(teacherUserId), anyString(), anyString(),
                eq("ASSIGNMENT_SUBMITTED"), eq("assignment"), eq(assignmentId));
        verify(auditService, atLeastOnce()).recordAuditLog(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitAssignment_persistsSubmissionTextAndFileUrlFromRequestPayload() {
        Assignment assignment = assignment(null, null, null, false);
        assignment.setCreatedBy("teacher@test.com");
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(submissionRepository.findByAssignmentIdAndStudentIdAndIsDeletedFalse(assignmentId, studentId))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmissionRequest request = new SubmissionRequest();
        request.setSubmissionText("Typed answer");
        request.setFileUrl("https://cdn.example.com/work.pdf");

        SubmissionResponse response = service.submitAssignment(assignmentId, callerUserId, institutionId, request);

        assertEquals("Typed answer", response.getSubmissionText());
        assertEquals("https://cdn.example.com/work.pdf", response.getFileUrl());
        assertEquals("SUBMITTED", response.getStatus());
        assertEquals(Boolean.FALSE, response.getIsDraft());
    }

    @Test
    void submitAssignment_crossInstitutionAssignmentLooksMissing() {
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(assignment(null, null, null, false)));

        assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class,
                () -> service.submitAssignment(assignmentId, callerUserId, UUID.randomUUID(),
                        request("x", false)));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitAssignment_withoutStudentProfileIsRejected() {
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(assignment(null, null, null, false)));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(null);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.submitAssignment(assignmentId, callerUserId, institutionId,
                        request("x", false)));
        assertTrue(ex.getMessage().toLowerCase().contains("student"));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitAssignment_afterCloseDateIsRejected() {
        Assignment assignment = assignment(null, LocalDateTime.now().minusHours(1), null, false);
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());

        assertThrows(IllegalStateException.class,
                () -> service.submitAssignment(assignmentId, callerUserId, institutionId,
                        request("late work", false)));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitAssignment_draftBypassesClosedWindow() {
        Assignment assignment = assignment(null, LocalDateTime.now().minusHours(1), null, false);
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(submissionRepository.findByAssignmentIdAndStudentIdAndIsDeletedFalse(assignmentId, studentId))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmissionResponse response = service.submitAssignment(
                assignmentId, callerUserId, institutionId, request("wip", true));

        assertEquals("DRAFT", response.getStatus());
        assertEquals(Boolean.TRUE, response.getIsDraft());
        // Drafts never ping the teacher.
        verify(notificationService, never()).notifyUser(
                any(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void submitAssignment_gradedSubmissionIsImmutable() {
        AssignmentSubmission graded = AssignmentSubmission.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignmentId)
                .studentId(studentId)
                .status("GRADED")
                .grade(10)
                .isDraft(false)
                .isDeleted(false)
                .build();
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(assignment(null, null, null, false)));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(submissionRepository.findByAssignmentIdAndStudentIdAndIsDeletedFalse(assignmentId, studentId))
                .thenReturn(Optional.of(graded));

        assertThrows(IllegalStateException.class,
                () -> service.submitAssignment(assignmentId, callerUserId, institutionId,
                        request("changed answer", false)));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitAssignment_resubmitsWhileNotGraded() {
        AssignmentSubmission existing = AssignmentSubmission.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignmentId)
                .studentId(studentId)
                .status("SUBMITTED")
                .submissionText("first try")
                .isDraft(false)
                .isDeleted(false)
                .build();
        when(assignmentRepository.findById(assignmentId))
                .thenReturn(Optional.of(assignment(null, null, null, false)));
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(submissionRepository.findByAssignmentIdAndStudentIdAndIsDeletedFalse(assignmentId, studentId))
                .thenReturn(Optional.of(existing));
        when(submissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmissionResponse response = service.submitAssignment(
                assignmentId, callerUserId, institutionId, request("second try", false));

        assertEquals(existing.getId(), response.getId());
        assertEquals("second try", response.getSubmissionText());
    }

    @Test
    void getMySubmission_resolvesStudentProfileAndReturnsSavedWork() {
        AssignmentSubmission existing = AssignmentSubmission.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignmentId)
                .studentId(studentId)
                .submissionText("saved answer")
                .status("SUBMITTED")
                .isDraft(false)
                .isDeleted(false)
                .build();
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(student());
        when(submissionRepository.findByAssignmentIdAndStudentIdAndIsDeletedFalse(assignmentId, studentId))
                .thenReturn(Optional.of(existing));

        SubmissionResponse response = service.getMySubmission(assignmentId, callerUserId);

        assertNotNull(response);
        assertEquals("saved answer", response.getSubmissionText());
        assertEquals(studentId, response.getStudentId());
    }

    @Test
    void getMySubmission_returnsNullWhenNoStudentProfile() {
        when(classAccessGuard.findStudentByUserId(callerUserId)).thenReturn(null);
        assertNull(service.getMySubmission(assignmentId, callerUserId));
    }
}
