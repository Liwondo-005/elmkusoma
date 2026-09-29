package tz.elmkusoma.grading.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.domain.AssessmentResult;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.assessment.repository.AssessmentResultRepository;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.grading.dto.response.GradebookResponse;
import tz.elmkusoma.grading.service.impl.GradebookServiceImpl;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.domain.AssignmentSubmission;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Class gradebook aggregate: learners of the class + every assignment submission
 * and assessment result in one request, with per-student totals (§31/§36 —
 * replaces the N+1 per-student loops on the grading page).
 */
@ExtendWith(MockitoExtension.class)
class GradebookServiceTest {

    @Mock private ClassAccessGuard classAccessGuard;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentResultRepository resultRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassGroupRepository classGroupRepository;

    @InjectMocks
    private GradebookServiceImpl service;

    private UUID classGroupId;
    private UUID institutionId;
    private UUID studentId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        classGroupId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private Assignment assignment(UUID id, String title, int totalMarks) {
        return Assignment.builder()
                .id(id).institutionId(institutionId)
                .subjectId(UUID.randomUUID()).classGroupId(classGroupId)
                .title(title).totalMarks(totalMarks)
                .status("PUBLISHED").isDeleted(false)
                .build();
    }

    @Test
    void getGradebook_aggregatesScoresStatusesAndTotals() {
        UUID gradedAssignment = UUID.randomUUID();
        UUID untouchedAssignment = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();

        Assignment graded = assignment(gradedAssignment, "Essay 1", 20);
        Assignment untouched = assignment(untouchedAssignment, "Worksheet", 10);
        Assessment assessment = Assessment.builder()
                .id(assessmentId).institutionId(institutionId)
                .subjectId(UUID.randomUUID()).classGroupId(classGroupId)
                .title("Midterm quiz").totalMarks(50).passMarks(25)
                .isPublished(true).isDeleted(false)
                .build();

        AssignmentSubmission submission = AssignmentSubmission.builder()
                .id(UUID.randomUUID()).institutionId(institutionId)
                .assignmentId(gradedAssignment).studentId(studentId)
                .grade(15).status("GRADED").isDraft(false).isDeleted(false)
                .build();
        AssessmentResult result = AssessmentResult.builder()
                .id(UUID.randomUUID()).institutionId(institutionId)
                .assessmentId(assessmentId).studentId(studentId)
                .totalScore(40).isPassed(true).isDeleted(false)
                .build();
        Student student = Student.builder()
                .id(studentId).userId(userId).admissionNumber("S-001")
                .status(StudentStatus.ACTIVE).isDeleted(false)
                .build();
        User user = User.builder()
                .id(userId).firstName("Amina").lastName("Hassan").isDeleted(false)
                .build();

        when(classAccessGuard.resolveClassStudentIds(classGroupId)).thenReturn(List.of(studentId));
        when(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of(graded, untouched));
        when(assessmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of(assessment));
        when(submissionRepository.findByStudentIdInAndIsDeletedFalse(List.of(studentId)))
                .thenReturn(List.of(submission));
        when(resultRepository.findByStudentIdInAndIsDeletedFalse(List.of(studentId)))
                .thenReturn(List.of(result));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(userRepository.findAllById(any(Set.class))).thenReturn(List.of(user));

        GradebookResponse response = service.getGradebook(classGroupId, institutionId);

        assertEquals(classGroupId, response.getClassGroupId());
        assertEquals(1, response.getRows().size());

        GradebookResponse.Row row = response.getRows().get(0);
        assertEquals(studentId, row.getStudentId());
        assertEquals("Amina Hassan", row.getStudentName());
        assertEquals("S-001", row.getAdmissionNumber());

        assertEquals(2, row.getAssignments().size());
        GradebookResponse.Item gradedItem = row.getAssignments().stream()
                .filter(i -> "Essay 1".equals(i.getTitle())).findFirst().orElseThrow();
        assertEquals("GRADED", gradedItem.getStatus());
        assertEquals(15, gradedItem.getScore());
        assertEquals(20, gradedItem.getMaxMarks());

        GradebookResponse.Item untouchedItem = row.getAssignments().stream()
                .filter(i -> "Worksheet".equals(i.getTitle())).findFirst().orElseThrow();
        assertEquals("NOT_SUBMITTED", untouchedItem.getStatus());
        assertNull(untouchedItem.getScore());

        assertEquals(1, row.getAssessments().size());
        assertEquals("COMPLETED", row.getAssessments().get(0).getStatus());
        assertEquals(40, row.getAssessments().get(0).getScore());

        // 15 + 40 = 55 obtained of 20 + 50 = 70 graded-possible → 78.57%.
        assertEquals(55, row.getTotalObtained());
        assertEquals(70, row.getTotalObtainable());
        assertEquals(new BigDecimal("78.57"), row.getAveragePercentage());
    }

    @Test
    void getGradebook_emptyClassReturnsEmptyRows() {
        when(classAccessGuard.resolveClassStudentIds(classGroupId)).thenReturn(List.of());
        when(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of());
        when(assessmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of());

        GradebookResponse response = service.getGradebook(classGroupId, institutionId);

        assertNotNull(response.getRows());
        assertTrue(response.getRows().isEmpty());
        assertEquals(classGroupId, response.getClassGroupId());
    }

    @Test
    void getGradebook_draftSubmissionShowsDraftStatus() {
        UUID assignmentId = UUID.randomUUID();
        Assignment assignment = assignment(assignmentId, "Draft work", 10);
        AssignmentSubmission draft = AssignmentSubmission.builder()
                .id(UUID.randomUUID()).institutionId(institutionId)
                .assignmentId(assignmentId).studentId(studentId)
                .status("DRAFT").isDraft(true).isDeleted(false)
                .build();
        Student student = Student.builder()
                .id(studentId).userId(userId).status(StudentStatus.ACTIVE).isDeleted(false).build();

        when(classAccessGuard.resolveClassStudentIds(classGroupId)).thenReturn(List.of(studentId));
        when(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of(assignment));
        when(assessmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of());
        when(submissionRepository.findByStudentIdInAndIsDeletedFalse(List.of(studentId)))
                .thenReturn(List.of(draft));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        GradebookResponse response = service.getGradebook(classGroupId, institutionId);

        GradebookResponse.Row row = response.getRows().get(0);
        assertEquals("DRAFT", row.getAssignments().get(0).getStatus());
        assertEquals(0, row.getTotalObtained());
        assertEquals(0, row.getTotalObtainable());
        // Nothing graded yet → no fabricated average.
        assertNull(row.getAveragePercentage());
    }
}
