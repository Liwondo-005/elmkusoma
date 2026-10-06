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
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.domain.AssignmentSubmission;
import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.response.AssignmentResponse;
import tz.elmkusoma.learning.dto.response.SubmissionResponse;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.learning.repository.LessonProgressRepository;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.service.impl.LearningServiceImpl;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * B40: assignment update/delete/grade/list endpoints must only be usable by the
 * teacher who created the assignment or an admin, and teacher list endpoints must
 * be scoped to the caller.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentOwnershipSecurityTest {

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
    private UUID submissionId;
    private UUID institutionId;
    private UUID otherInstitutionId;
    private UUID classGroupId;

    @BeforeEach
    void setUp() {
        assignmentId = UUID.randomUUID();
        submissionId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
    }

    private Assignment assignment(String createdBy) {
        return Assignment.builder()
                .id(assignmentId)
                .institutionId(institutionId)
                .subjectId(UUID.randomUUID())
                .classGroupId(classGroupId)
                .title("Existing assignment")
                .totalMarks(10)
                .status("PUBLISHED")
                .isDeleted(false)
                .createdBy(createdBy)
                .build();
    }

    private AssignmentSubmission submission() {
        return AssignmentSubmission.builder()
                .id(submissionId)
                .institutionId(institutionId)
                .assignmentId(assignmentId)
                .studentId(UUID.randomUUID())
                .status("SUBMITTED")
                .isDraft(false)
                .isDeleted(false)
                .build();
    }

    private AssignmentRequest request() {
        AssignmentRequest request = new AssignmentRequest();
        request.setSubjectId(UUID.randomUUID());
        request.setClassGroupId(classGroupId);
        request.setTitle("Updated title");
        request.setTotalMarks(10);
        return request;
    }

    private void stubAssignment(Assignment assignment) {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    }

    private void stubSave() {
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── updateAssignment ──

    @Test
    void updateAssignment_ownerMayUpdateOwnAssignment() {
        stubAssignment(assignment("owner@example.com"));
        stubSave();

        AssignmentResponse response = service.updateAssignment(
                assignmentId, request(), institutionId, "owner@example.com", "TEACHER");

        assertEquals("Updated title", response.getTitle());
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    void updateAssignment_nonOwnerTeacherIsRejected() {
        stubAssignment(assignment("owner@example.com"));

        SecurityException ex = assertThrows(SecurityException.class, () -> service.updateAssignment(
                assignmentId, request(), institutionId, "intruder@example.com", "TEACHER"));

        assertTrue(ex.getMessage().contains("do not own"));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void updateAssignment_adminRolesBypassOwnership() {
        for (String role : List.of("ADMIN", "INSTITUTION_ADMIN", "NATIONAL_ADMIN")) {
            stubAssignment(assignment("owner@example.com"));
            stubSave();

            assertDoesNotThrow(() -> service.updateAssignment(
                            assignmentId, request(), institutionId, "other@example.com", role),
                    "role " + role + " must bypass assignment ownership");

            reset(assignmentRepository);
        }
    }

    @Test
    void updateAssignment_crossInstitutionIsRejectedEvenForOwner() {
        stubAssignment(assignment("owner@example.com"));

        assertThrows(ResourceNotFoundException.class, () -> service.updateAssignment(
                assignmentId, request(), otherInstitutionId, "owner@example.com", "TEACHER"));
        verify(assignmentRepository, never()).save(any());
    }

    // ── deleteAssignment ──

    @Test
    void deleteAssignment_nonOwnerTeacherIsRejected() {
        stubAssignment(assignment("owner@example.com"));

        assertThrows(SecurityException.class, () -> service.deleteAssignment(
                assignmentId, institutionId, "intruder@example.com", "TEACHER"));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void deleteAssignment_ownerMaySoftDeleteOwnAssignment() {
        Assignment assignment = assignment("owner@example.com");
        stubAssignment(assignment);
        stubSave();

        service.deleteAssignment(assignmentId, institutionId, "owner@example.com", "TEACHER");

        assertTrue(assignment.getIsDeleted());
        verify(assignmentRepository).save(assignment);
    }

    @Test
    void deleteAssignment_institutionAdminMayDeleteAnotherTeachersAssignment() {
        Assignment assignment = assignment("owner@example.com");
        stubAssignment(assignment);
        stubSave();

        service.deleteAssignment(assignmentId, institutionId, "admin@example.com", "INSTITUTION_ADMIN");

        assertTrue(assignment.getIsDeleted());
    }

    // ── gradeSubmission ──

    @Test
    void gradeSubmission_nonOwnerTeacherIsRejected() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission()));
        stubAssignment(assignment("owner@example.com"));

        assertThrows(SecurityException.class, () -> service.gradeSubmission(
                submissionId, 8, "good", UUID.randomUUID(), institutionId, "intruder@example.com", "TEACHER"));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void gradeSubmission_ownerMayGrade() {
        AssignmentSubmission submission = submission();
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        stubAssignment(assignment("owner@example.com"));
        when(submissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmissionResponse response = service.gradeSubmission(
                submissionId, 9, "well done", UUID.randomUUID(), institutionId, "owner@example.com", "TEACHER");

        assertEquals(Integer.valueOf(9), response.getGrade());
        assertEquals("GRADED", response.getStatus());
        verify(submissionRepository).save(any(AssignmentSubmission.class));
    }

    @Test
    void gradeSubmission_institutionAdminMayGradeAnotherTeachersSubmission() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission()));
        stubAssignment(assignment("owner@example.com"));
        when(submissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.gradeSubmission(
                submissionId, 5, "ok", UUID.randomUUID(), institutionId, "admin@example.com", "INSTITUTION_ADMIN"));
    }

    // ── getSubmissionsByAssignment ──

    @Test
    void getSubmissionsByAssignment_nonOwnerTeacherIsRejected() {
        stubAssignment(assignment("owner@example.com"));

        assertThrows(SecurityException.class, () -> service.getSubmissionsByAssignment(
                assignmentId, institutionId, "intruder@example.com", "TEACHER"));
        verify(submissionRepository, never()).findByAssignmentIdAndIsDeletedFalse(any());
    }

    @Test
    void getSubmissionsByAssignment_ownerReceivesSubmissions() {
        stubAssignment(assignment("owner@example.com"));
        when(submissionRepository.findByAssignmentIdAndIsDeletedFalse(assignmentId))
                .thenReturn(List.of(submission()));

        List<SubmissionResponse> response = service.getSubmissionsByAssignment(
                assignmentId, institutionId, "owner@example.com", "TEACHER");

        assertEquals(1, response.size());
        assertEquals(submissionId, response.get(0).getId());
    }

    // ── teacher list scoping ──

    @Test
    void getAssignmentsByClass_teacherSeesOnlyOwnAssignments() {
        Assignment mine = assignment("teacher@example.com");
        Assignment theirs = Assignment.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .subjectId(UUID.randomUUID())
                .classGroupId(classGroupId)
                .title("Colleague's assignment")
                .totalMarks(10)
                .status("PUBLISHED")
                .isDeleted(false)
                .createdBy("colleague@example.com")
                .build();
        when(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of(mine, theirs));

        List<AssignmentResponse> response = service.getAssignmentsByClass(
                classGroupId, institutionId, "teacher@example.com", "TEACHER");

        assertEquals(1, response.size());
        assertEquals("Existing assignment", response.get(0).getTitle());
    }

    @Test
    void getAssignmentsByClass_studentStillSeesEveryClassAssignment() {
        Assignment mine = assignment("teacher@example.com");
        Assignment theirs = Assignment.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .subjectId(UUID.randomUUID())
                .classGroupId(classGroupId)
                .title("Someone else's assignment")
                .totalMarks(10)
                .status("PUBLISHED")
                .isDeleted(false)
                .createdBy("colleague@example.com")
                .build();
        when(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of(mine, theirs));

        List<AssignmentResponse> response = service.getAssignmentsByClass(
                classGroupId, institutionId, "learner@example.com", "STUDENT");

        assertEquals(2, response.size(), "enrolled learners must keep seeing the whole class list");
        verify(classAccessGuard).assertLearnerCanAccessClass("learner@example.com", classGroupId);
    }

    @Test
    void getAssignmentsByClasses_teacherSeesOnlyOwnAssignments() {
        Assignment mine = assignment("teacher@example.com");
        Assignment theirs = Assignment.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .subjectId(UUID.randomUUID())
                .classGroupId(UUID.randomUUID())
                .title("Colleague's assignment")
                .totalMarks(10)
                .status("PUBLISHED")
                .isDeleted(false)
                .createdBy("colleague@example.com")
                .build();
        List<UUID> ids = List.of(classGroupId, theirs.getClassGroupId());
        when(assignmentRepository.findByClassGroupIdInAndIsDeletedFalse(ids))
                .thenReturn(List.of(mine, theirs));

        List<AssignmentResponse> response = service.getAssignmentsByClasses(
                ids, institutionId, "teacher@example.com", "TEACHER");

        assertEquals(1, response.size());
        assertEquals("Existing assignment", response.get(0).getTitle());
    }

    @Test
    void getAssignmentsByClasses_adminSeesEveryAssignmentInInstitution() {
        Assignment mine = assignment("teacher@example.com");
        Assignment theirs = Assignment.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .subjectId(UUID.randomUUID())
                .classGroupId(UUID.randomUUID())
                .title("Colleague's assignment")
                .totalMarks(10)
                .status("PUBLISHED")
                .isDeleted(false)
                .createdBy("colleague@example.com")
                .build();
        List<UUID> ids = List.of(classGroupId, theirs.getClassGroupId());
        when(assignmentRepository.findByClassGroupIdInAndIsDeletedFalse(ids))
                .thenReturn(List.of(mine, theirs));

        List<AssignmentResponse> response = service.getAssignmentsByClasses(
                ids, institutionId, "admin@example.com", "INSTITUTION_ADMIN");

        assertEquals(2, response.size());
    }

    @Test
    void getAssignmentsByClasses_excludesOtherInstitutionsRows() {
        Assignment foreign = assignment("teacher@example.com");
        foreign.setInstitutionId(otherInstitutionId);
        List<UUID> ids = List.of(classGroupId);
        when(assignmentRepository.findByClassGroupIdInAndIsDeletedFalse(ids))
                .thenReturn(List.of(foreign));

        List<AssignmentResponse> response = service.getAssignmentsByClasses(
                ids, institutionId, "admin@example.com", "INSTITUTION_ADMIN");

        assertTrue(response.isEmpty());
    }
}
