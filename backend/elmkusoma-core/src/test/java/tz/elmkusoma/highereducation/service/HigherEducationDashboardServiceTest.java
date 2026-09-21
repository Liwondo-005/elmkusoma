package tz.elmkusoma.highereducation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.highereducation.domain.*;
import tz.elmkusoma.highereducation.dto.*;
import tz.elmkusoma.highereducation.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HigherEducationDashboardServiceTest {

    @Mock private StudyTaskRepository studyTaskRepository;
    @Mock private ResearchProjectRepository researchProjectRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CompetencyRecordRepository competencyRecordRepository;
    @Mock private PortfolioRepository portfolioRepository;
    @Mock private PortfolioItemRepository portfolioItemRepository;
    @Mock private DemonstrationRepository demonstrationRepository;
    @Mock private LogbookEntryRepository logbookEntryRepository;
    @Mock private FieldworkPlacementRepository fieldworkPlacementRepository;
    @Mock private StudentCourseEnrollmentRepository enrollmentRepository;
    @Mock private AcademicRecordRepository academicRecordRepository;
    @Mock private ProjectSubmissionRepository projectSubmissionRepository;
    @Mock private CareerProfileRepository careerProfileRepository;
    @Mock private LiveClassRepository liveClassRepository;
    @Mock private GpaCalculationService gpaCalculationService;

    @InjectMocks
    private HigherEducationDashboardService dashboardService;

    private UUID studentId;
    private UUID institutionId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
    }

    @Test
    void getDashboard_returnsCompleteDTO() {
        when(studyTaskRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(studyTaskRepository.findByStudentIdAndScheduledDateAndIsDeletedFalse(any(), any())).thenReturn(Collections.emptyList());
        when(researchProjectRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(projectRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(competencyRecordRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(portfolioRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(demonstrationRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(fieldworkPlacementRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(enrollmentRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(enrollmentRepository.findByStudentIdAndStatusAndIsDeletedFalse(any(), any())).thenReturn(Collections.emptyList());
        when(academicRecordRepository.findTopByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(careerProfileRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Optional.empty());
        when(liveClassRepository.findByInstitutionIdAndScheduledAtBetween(any(), any(), any())).thenReturn(Collections.emptyList());
        when(gpaCalculationService.computeSemesterGpa(any(), any(), any())).thenReturn(null);
        when(gpaCalculationService.computeCumulativeGpa(any())).thenReturn(null);

        HigherEducationDashboardDTO result = dashboardService.getDashboard(studentId, institutionId, "UNIVERSITY");

        assertNotNull(result);
        assertEquals("UNIVERSITY", result.getAcademicContext());
        assertNotNull(result.getWhatsNext());
        assertNotNull(result.getToday());
        assertNotNull(result.getLiveCampus());
        assertNotNull(result.getAcademicLoad());
        assertNotNull(result.getMyProgress());
        assertNotNull(result.getMyEvidence());
        assertNotNull(result.getCareerWorld());
        assertNotNull(result.getDayWeekView());
    }

    @Test
    void getDashboard_withLiveClasses_returnsLiveCampusData() {
        LiveClass liveClass = LiveClass.builder()
                .id(UUID.randomUUID())
                .title("Database Systems")
                .status("IN_PROGRESS")
                .scheduledAt(LocalDateTime.now())
                .durationMinutes(60)
                .build();
        when(liveClassRepository.findByInstitutionIdAndScheduledAtBetween(any(), any(), any()))
                .thenReturn(List.of(liveClass));
        when(studyTaskRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(studyTaskRepository.findByStudentIdAndScheduledDateAndIsDeletedFalse(any(), any())).thenReturn(Collections.emptyList());
        when(researchProjectRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(projectRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(competencyRecordRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(portfolioRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(demonstrationRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(fieldworkPlacementRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(enrollmentRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(enrollmentRepository.findByStudentIdAndStatusAndIsDeletedFalse(any(), any())).thenReturn(Collections.emptyList());
        when(academicRecordRepository.findTopByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(careerProfileRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Optional.empty());
        when(gpaCalculationService.computeSemesterGpa(any(), any(), any())).thenReturn(null);
        when(gpaCalculationService.computeCumulativeGpa(any())).thenReturn(null);

        HigherEducationDashboardDTO result = dashboardService.getDashboard(studentId, institutionId, "COLLEGE");

        assertNotNull(result.getLiveCampus());
        assertEquals(1, result.getLiveCampus().getLiveNow());
        assertEquals("Database Systems", result.getLiveCampus().getSessions().get(0).getTitle());
    }

    @Test
    void getDashboard_withStudyTasks_returnsTodayData() {
        StudyTask task = StudyTask.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .title("Review Chapter 5")
                .scheduledDate(LocalDate.now())
                .isCompleted(false)
                .build();
        when(studyTaskRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(List.of(task));
        when(studyTaskRepository.findByStudentIdAndScheduledDateAndIsDeletedFalse(any(), any())).thenReturn(List.of(task));
        when(researchProjectRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(projectRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(competencyRecordRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(portfolioRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(demonstrationRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(fieldworkPlacementRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(enrollmentRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Collections.emptyList());
        when(enrollmentRepository.findByStudentIdAndStatusAndIsDeletedFalse(any(), any())).thenReturn(Collections.emptyList());
        when(academicRecordRepository.findTopByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(careerProfileRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(Optional.empty());
        when(liveClassRepository.findByInstitutionIdAndScheduledAtBetween(any(), any(), any())).thenReturn(Collections.emptyList());
        when(gpaCalculationService.computeSemesterGpa(any(), any(), any())).thenReturn(null);
        when(gpaCalculationService.computeCumulativeGpa(any())).thenReturn(null);

        HigherEducationDashboardDTO result = dashboardService.getDashboard(studentId, institutionId, "UNIVERSITY");

        assertNotNull(result.getToday());
        assertEquals(1, result.getToday().getTotalTasks());
        assertEquals("Review Chapter 5", result.getToday().getItems().get(0).getTitle());
    }
}
