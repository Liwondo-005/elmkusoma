package tz.elmkusoma.grading.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.Term;
import tz.elmkusoma.academic.repository.AcademicYearRepository;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.repository.TermRepository;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.domain.AssessmentResult;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.assessment.repository.AssessmentResultRepository;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.grading.domain.GradeBoundary;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.domain.ReportCard.ReportCardStatus;
import tz.elmkusoma.grading.dto.request.GenerateReportCardRequest;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;
import tz.elmkusoma.grading.repository.GradeBoundaryRepository;
import tz.elmkusoma.grading.repository.ReportCardRepository;
import tz.elmkusoma.grading.service.impl.ReportCardServiceImpl;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.domain.AssignmentSubmission;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Real report-card computation from graded assignments + assessment results
 * (§37): average percentage, boundary-derived grade/GPA, upsert per
 * student+term, class ranking and GRADE_RELEASED learner notification.
 */
@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

    @Mock private ReportCardRepository reportCardRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private TermRepository termRepository;
    @Mock private ClassGroupRepository classGroupRepository;
    @Mock private StudentClassAssignmentRepository studentClassAssignmentRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentResultRepository resultRepository;
    @Mock private GradeBoundaryRepository gradeBoundaryRepository;
    @Mock private GradeBoundaryService gradeBoundaryService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private ClassAccessGuard classAccessGuard;

    @InjectMocks
    private ReportCardServiceImpl service;

    private UUID institutionId;
    private UUID studentId;
    private UUID userId;
    private UUID termId;
    private UUID yearId;
    private UUID classGroupId;
    private UUID scaleId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        termId = UUID.randomUUID();
        yearId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        scaleId = UUID.randomUUID();
    }

    private Student student() {
        return Student.builder()
                .id(studentId)
                .userId(userId)
                .admissionNumber("S-001")
                .status(StudentStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    @Test
    void generate_computesAverageGradeAndGpaFromGradedWork() {
        UUID assignmentId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();

        when(termRepository.findById(termId)).thenReturn(Optional.of(
                Term.builder().id(termId).name("Term 1").academicYearId(yearId).build()));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
        when(classAccessGuard.resolveClassGroupIdForStudent(studentId)).thenReturn(classGroupId);

        // 15/20 on the assignment …
        when(submissionRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of(AssignmentSubmission.builder()
                        .id(UUID.randomUUID()).institutionId(institutionId)
                        .assignmentId(assignmentId).studentId(studentId)
                        .grade(15).status("GRADED").isDraft(false).isDeleted(false)
                        .build()));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(
                Assignment.builder().id(assignmentId).institutionId(institutionId)
                        .title("Essay").totalMarks(20).isDeleted(false).build()));

        // … plus 40/50 on the assessment.
        when(resultRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of(AssessmentResult.builder()
                        .id(UUID.randomUUID()).institutionId(institutionId)
                        .assessmentId(assessmentId).studentId(studentId)
                        .totalScore(40).isPassed(true).isDeleted(false)
                        .build()));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(
                Assessment.builder().id(assessmentId).institutionId(institutionId)
                        .title("Midterm").totalMarks(50).isDeleted(false).build()));

        when(gradeBoundaryService.calculateGradeForPercentage(eq(scaleId), any(BigDecimal.class)))
                .thenReturn("B");
        when(gradeBoundaryRepository.findBoundariesByScaleId(scaleId))
                .thenReturn(List.of(GradeBoundary.builder()
                        .gradingScaleId(scaleId).gradeLabel("B")
                        .minPercentage(BigDecimal.valueOf(70))
                        .maxPercentage(BigDecimal.valueOf(84))
                        .gpaPoints(BigDecimal.valueOf(3.5))
                        .build()));

        when(reportCardRepository.findByStudentIdAndTermIdAndIsDeletedFalse(studentId, termId))
                .thenReturn(Optional.empty());
        when(reportCardRepository.save(any(ReportCard.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GenerateReportCardRequest request = new GenerateReportCardRequest();
        request.setStudentId(studentId);
        request.setTermId(termId);
        request.setGradingScaleId(scaleId);

        ReportCardResponse response = service.generate(institutionId, request);

        ArgumentCaptor<ReportCard> captor = ArgumentCaptor.forClass(ReportCard.class);
        verify(reportCardRepository).save(captor.capture());
        ReportCard saved = captor.getValue();

        // 15 + 40 = 55 obtained of 20 + 50 = 70 → 78.57%.
        assertEquals(new BigDecimal("55"), saved.getTotalMarks());
        assertEquals(new BigDecimal("78.57"), saved.getAverageMark());
        assertEquals("B", saved.getOverallGrade());
        assertEquals(0, new BigDecimal("3.5").compareTo(saved.getGpa()));
        assertEquals(yearId, saved.getAcademicYearId());
        assertEquals(classGroupId, saved.getClassGroupId());
        assertEquals(ReportCardStatus.DRAFT, saved.getStatus());

        assertEquals(new BigDecimal("78.57"), response.getAverageMark());
        assertEquals("B", response.getOverallGrade());
        assertEquals("Term 1", response.getTermName());
        assertEquals("DRAFT", response.getStatus());
        assertEquals(studentId, response.getStudentId());

        verify(auditService).recordAuditLog(
                eq(institutionId), any(), isNull(), isNull(),
                eq("ReportCard"), any(), anyString(), any(), any(), any());
    }

    @Test
    void generate_upsertsExistingCardForSameStudentAndTerm() {
        UUID assignmentId = UUID.randomUUID();
        ReportCard existing = ReportCard.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .studentId(studentId)
                .termId(termId)
                .status(ReportCardStatus.PUBLISHED)
                .isDeleted(false)
                .build();

        when(termRepository.findById(termId)).thenReturn(Optional.of(
                Term.builder().id(termId).name("Term 1").academicYearId(yearId).build()));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
        when(classAccessGuard.resolveClassGroupIdForStudent(studentId)).thenReturn(classGroupId);
        when(submissionRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of(AssignmentSubmission.builder()
                        .id(UUID.randomUUID()).institutionId(institutionId)
                        .assignmentId(assignmentId).studentId(studentId)
                        .grade(10).status("GRADED").isDraft(false).isDeleted(false)
                        .build()));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(
                Assignment.builder().id(assignmentId).institutionId(institutionId)
                        .title("Essay").totalMarks(20).isDeleted(false).build()));
        when(resultRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of());
        when(gradeBoundaryService.calculateGradeForPercentage(eq(scaleId), any(BigDecimal.class)))
                .thenReturn("D");
        when(gradeBoundaryRepository.findBoundariesByScaleId(scaleId)).thenReturn(List.of());
        when(reportCardRepository.findByStudentIdAndTermIdAndIsDeletedFalse(studentId, termId))
                .thenReturn(Optional.of(existing));
        when(reportCardRepository.save(any(ReportCard.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GenerateReportCardRequest request = new GenerateReportCardRequest();
        request.setStudentId(studentId);
        request.setTermId(termId);
        request.setGradingScaleId(scaleId);

        service.generate(institutionId, request);

        ArgumentCaptor<ReportCard> captor = ArgumentCaptor.forClass(ReportCard.class);
        verify(reportCardRepository).save(captor.capture());
        // Same row updated — no duplicate card for the student + term.
        assertEquals(existing.getId(), captor.getValue().getId());
        assertEquals(0, new BigDecimal("50").compareTo(captor.getValue().getAverageMark()));
        // totalMarks stores marks obtained (10), not the percentage.
        assertEquals(10, captor.getValue().getTotalMarks().intValue());
    }

    @Test
    void updateStatus_publishNotifiesLearnerAndAudits() {
        ReportCard card = ReportCard.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .studentId(studentId)
                .termId(termId)
                .status(ReportCardStatus.DRAFT)
                .isDeleted(false)
                .build();
        when(reportCardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(reportCardRepository.save(any(ReportCard.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
        when(userRepository.findByEmailAndIsDeletedFalse("admin@test.com"))
                .thenReturn(Optional.empty());

        ReportCardResponse response = service.updateStatus(
                card.getId(), "PUBLISHED", "admin@test.com", "ADMIN");

        assertEquals("PUBLISHED", response.getStatus());
        assertNotNull(response.getPublishedAt());
        assertNotNull(card.getPublishedAt());

        verify(notificationService).notifyUser(
                eq(userId), eq("Report card released"), anyString(),
                eq("GRADE_RELEASED"), eq("reportCard"), eq(card.getId()));
        verify(auditService).recordAuditLog(
                eq(institutionId), any(), eq("admin@test.com"), eq("ADMIN"),
                eq("ReportCard"), eq(card.getId()), anyString(), any(), any(), any());
    }

    @Test
    void updateStatus_draftDoesNotNotify() {
        ReportCard card = ReportCard.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .studentId(studentId)
                .status(ReportCardStatus.DRAFT)
                .isDeleted(false)
                .build();
        when(reportCardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(reportCardRepository.save(any(ReportCard.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmailAndIsDeletedFalse("admin@test.com"))
                .thenReturn(Optional.empty());

        ReportCardResponse response = service.updateStatus(
                card.getId(), "DRAFT", "admin@test.com", "ADMIN");

        assertEquals("DRAFT", response.getStatus());
        verifyNoInteractions(notificationService);
    }

    @Test
    void getByStudentIds_batchesCardsInOneQuery() {
        UUID otherStudentId = UUID.randomUUID();
        ReportCard mine = ReportCard.builder()
                .id(UUID.randomUUID()).institutionId(institutionId)
                .studentId(studentId).termId(termId)
                .status(ReportCardStatus.PUBLISHED)
                .build();
        ReportCard other = ReportCard.builder()
                .id(UUID.randomUUID()).institutionId(institutionId)
                .studentId(otherStudentId).termId(termId)
                .status(ReportCardStatus.PUBLISHED)
                .build();
        when(reportCardRepository.findByStudentIdInAndIsDeletedFalse(
                List.of(studentId, otherStudentId))).thenReturn(List.of(mine, other));

        List<ReportCardResponse> response =
                service.getByStudentIds(List.of(studentId, otherStudentId));

        assertEquals(2, response.size());
        assertTrue(service.getByStudentIds(null).isEmpty());
        assertTrue(service.getByStudentIds(List.of()).isEmpty());
    }
}
