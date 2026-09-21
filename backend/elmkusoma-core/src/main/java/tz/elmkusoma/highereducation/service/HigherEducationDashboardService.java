package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.highereducation.domain.*;
import tz.elmkusoma.highereducation.dto.*;
import tz.elmkusoma.highereducation.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HigherEducationDashboardService {

    private final StudyTaskRepository studyTaskRepository;
    private final ResearchProjectRepository researchProjectRepository;
    private final ProjectRepository projectRepository;
    private final CompetencyRecordRepository competencyRecordRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final DemonstrationRepository demonstrationRepository;
    private final LogbookEntryRepository logbookEntryRepository;
    private final FieldworkPlacementRepository fieldworkPlacementRepository;
    private final StudentCourseEnrollmentRepository enrollmentRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final ProjectSubmissionRepository projectSubmissionRepository;
    private final CareerProfileRepository careerProfileRepository;
    private final LiveClassRepository liveClassRepository;
    private final GpaCalculationService gpaCalculationService;

    public HigherEducationDashboardDTO getDashboard(UUID studentId, UUID institutionId, String learningLevel) {
        HigherEducationDashboardDTO dashboard = new HigherEducationDashboardDTO();
        dashboard.setAcademicContext(learningLevel != null ? learningLevel.toUpperCase() : "COLLEGE");

        LocalDate today = LocalDate.now();

        dashboard.setWhatsNext(computeWhatsNext(studentId));
        dashboard.setToday(computeToday(studentId, today));
        dashboard.setContinueLearning(computeContinueLearning(studentId));
        dashboard.setLiveCampus(computeLiveCampus(institutionId, today));

        List<StudentCourseEnrollment> activeEnrollments = enrollmentRepository
                .findByStudentIdAndStatusAndIsDeletedFalse(studentId, EnrollmentStatus.ENROLLED);
        dashboard.setMyCourses(activeEnrollments.stream().map(e -> {
            int progress = computeEnrollmentProgressPercent(e);
            return HigherEducationDashboardDTO.CourseSummaryDTO.builder()
                    .id(e.getId().toString())
                    .title("Course " + e.getCourseId().toString().substring(0, Math.min(8, e.getCourseId().toString().length())))
                    .progressPercent(progress).totalModules(0).completedModules(0).build();
        }).collect(Collectors.toList()));

        dashboard.setAcademicLoad(computeAcademicLoad(studentId));
        dashboard.setProjects(computeProjects(studentId));
        dashboard.setResearch(computeResearch(studentId));
        dashboard.setMyProgress(computeMyProgress(studentId));
        dashboard.setMyEvidence(computeMyEvidence(studentId));
        dashboard.setCareerWorld(computeCareerWorld(studentId));
        dashboard.setDayWeekView(computeDayWeek(studentId, today));

        List<StudyTask> allTasks = studyTaskRepository.findByStudentIdAndIsDeletedFalse(studentId);
        dashboard.setStudyPlannerTasks(allTasks.stream().limit(10).map(this::toStudyTaskDTO).collect(Collectors.toList()));

        Double computedSemesterGpa = gpaCalculationService.computeSemesterGpa(studentId, dashboard.getAcademicLoad().getCurrentSemesterCourses() > 0 ? "CURRENT" : null, null);
        Double computedCumulativeGpa = gpaCalculationService.computeCumulativeGpa(studentId);
        if (computedSemesterGpa != null) dashboard.getAcademicLoad().setCurrentSemesterGpa(computedSemesterGpa);
        if (computedCumulativeGpa != null) dashboard.getAcademicLoad().setCumulativeGpa(computedCumulativeGpa);

        return dashboard;
    }

    private HigherEducationDashboardDTO.WhatsNextDTO computeWhatsNext(UUID studentId) {
        List<StudyTask> pendingTasks = studyTaskRepository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsCompleted()))
                .filter(t -> t.getScheduledDate() != null)
                .sorted(Comparator.comparing(StudyTask::getScheduledDate))
                .limit(1).collect(Collectors.toList());

        if (!pendingTasks.isEmpty()) {
            StudyTask task = pendingTasks.get(0);
            return HigherEducationDashboardDTO.WhatsNextDTO.builder()
                    .title(task.getTitle()).description(task.getDescription())
                    .type(task.getTaskType() != null ? task.getTaskType().name() : "TASK")
                    .url("/dashboard/learner/study-planner")
                    .deadline(task.getScheduledDate() != null ? task.getScheduledDate().toString() : null)
                    .build();
        }

        List<Project> activeProjects = projectRepository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                .filter(p -> p.getStatus() != ProjectStatus.COMPLETED && p.getStatus() != ProjectStatus.ARCHIVED)
                .limit(1).collect(Collectors.toList());
        if (!activeProjects.isEmpty()) {
            Project project = activeProjects.get(0);
            return HigherEducationDashboardDTO.WhatsNextDTO.builder()
                    .title(project.getTitle()).description(project.getDescription()).type("PROJECT")
                    .url("/dashboard/learner/projects")
                    .deadline(project.getDueDate() != null ? project.getDueDate().toString() : null)
                    .build();
        }

        return HigherEducationDashboardDTO.WhatsNextDTO.builder()
                .title("No upcoming items").description("You're all caught up!").type("NONE").build();
    }

    private HigherEducationDashboardDTO.TodayDTO computeToday(UUID studentId, LocalDate today) {
        List<StudyTask> todayTasks = studyTaskRepository.findByStudentIdAndScheduledDateAndIsDeletedFalse(studentId, today);

        List<HigherEducationDashboardDTO.TodayItemDTO> items = todayTasks.stream()
                .map(t -> HigherEducationDashboardDTO.TodayItemDTO.builder()
                        .title(t.getTitle())
                        .type(t.getTaskType() != null ? t.getTaskType().name() : "TASK")
                        .time(t.getScheduledTime() != null ? t.getScheduledTime().toString() : null)
                        .status(Boolean.TRUE.equals(t.getIsCompleted()) ? "DONE" : "PENDING")
                        .build()).collect(Collectors.toList());

        return HigherEducationDashboardDTO.TodayDTO.builder()
                .totalTasks(todayTasks.size())
                .completedTasks((int) todayTasks.stream().filter(t -> Boolean.TRUE.equals(t.getIsCompleted())).count())
                .pendingTasks((int) todayTasks.stream().filter(t -> !Boolean.TRUE.equals(t.getIsCompleted())).count())
                .items(items).build();
    }

    private HigherEducationDashboardDTO.ContinueLearningDTO computeContinueLearning(UUID studentId) {
        List<StudyTask> recentTasks = studyTaskRepository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsCompleted()))
                .sorted(Comparator.comparing(StudyTask::getCreatedAt).reversed())
                .limit(1).collect(Collectors.toList());

        if (!recentTasks.isEmpty()) {
            StudyTask task = recentTasks.get(0);
            return HigherEducationDashboardDTO.ContinueLearningDTO.builder()
                    .lastCourse(task.getTitle()).lastModule(task.getDescription())
                    .progressPercent(computeTaskProgressPercent(studentId)).courseId(task.getId().toString()).build();
        }
        return null;
    }

    private HigherEducationDashboardDTO.LiveCampusDTO computeLiveCampus(UUID institutionId, LocalDate today) {
        if (institutionId == null) {
            return HigherEducationDashboardDTO.LiveCampusDTO.builder()
                    .liveNow(0).upcomingToday(0).sessions(Collections.emptyList()).build();
        }

        List<LiveClass> todaySessions = liveClassRepository
                .findByInstitutionIdAndScheduledAtBetween(
                        institutionId,
                        today.atStartOfDay(),
                        today.atTime(LocalTime.MAX));

        long liveNow = todaySessions.stream()
                .filter(lc -> "IN_PROGRESS".equals(lc.getStatus()) || "LIVE".equals(lc.getStatus())
                        || "STARTING".equals(lc.getStatus()))
                .count();

        long upcomingToday = todaySessions.stream()
                .filter(lc -> "SCHEDULED".equals(lc.getStatus()))
                .count();

        List<HigherEducationDashboardDTO.LiveSessionSummaryDTO> sessions = todaySessions.stream()
                .limit(10)
                .map(lc -> HigherEducationDashboardDTO.LiveSessionSummaryDTO.builder()
                        .id(lc.getId().toString())
                        .title(lc.getTitle())
                        .sessionType(lc.getSessionType() != null ? lc.getSessionType().name() : "LECTURE")
                        .startTime(lc.getScheduledAt() != null ? lc.getScheduledAt().toString() : null)
                        .status(lc.getStatus())
                        .build())
                .collect(Collectors.toList());

        return HigherEducationDashboardDTO.LiveCampusDTO.builder()
                .liveNow((int) liveNow)
                .upcomingToday((int) upcomingToday)
                .sessions(sessions)
                .build();
    }

    private HigherEducationDashboardDTO.AcademicLoadDTO computeAcademicLoad(UUID studentId) {
        List<StudentCourseEnrollment> allEnrollments = enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId);
        long activeCount = allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED || e.getStatus() == EnrollmentStatus.IN_PROGRESS).count();
        int totalCredits = allEnrollments.stream().filter(e -> e.getCreditHours() != null).mapToInt(StudentCourseEnrollment::getCreditHours).sum();
        int completedCredits = allEnrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED && e.getCreditHours() != null).mapToInt(StudentCourseEnrollment::getCreditHours).sum();
        OptionalDouble avgGpa = allEnrollments.stream().filter(e -> e.getGradePoints() != null).mapToDouble(StudentCourseEnrollment::getGradePoints).average();

        AcademicRecord latestRecord = academicRecordRepository.findTopByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId).orElse(null);

        return HigherEducationDashboardDTO.AcademicLoadDTO.builder()
                .totalCreditHours(totalCredits).enrolledCourses((int) activeCount)
                .completedCreditHours(completedCredits).currentSemesterCourses((int) activeCount)
                .currentSemesterGpa(avgGpa.isPresent() ? Math.round(avgGpa.getAsDouble() * 100.0) / 100.0 : null)
                .cumulativeGpa(latestRecord != null ? latestRecord.getCumulativeGpa() : null).build();
    }

    private List<HigherEducationDashboardDTO.CourseSummaryDTO> computeProjects(UUID studentId) {
        List<Project> projects = projectRepository.findByStudentIdAndIsDeletedFalse(studentId);
        return projects.stream().map(p -> HigherEducationDashboardDTO.CourseSummaryDTO.builder()
                .id(p.getId().toString()).title(p.getTitle())
                .progressPercent(0).totalModules(0).completedModules(0).build())
                .collect(Collectors.toList());
    }

    private List<ResearchProjectDTO> computeResearch(UUID studentId) {
        List<ResearchProject> research = researchProjectRepository.findByStudentIdAndIsDeletedFalse(studentId);
        return research.stream().map(p -> ResearchProjectDTO.builder()
                .id(p.getId()).title(p.getTitle()).researchQuestion(p.getResearchQuestion())
                .status(p.getStatus() != null ? p.getStatus() : ResearchStatus.IDEA)
                .supervisorId(p.getSupervisorId()).programmeId(p.getProgrammeId())
                .methodology(p.getMethodology()).startDate(p.getStartDate())
                .dueDate(p.getDueDate()).completedDate(p.getCompletedDate())
                .abstractText(p.getAbstractText()).keywords(p.getKeywords()).build())
                .collect(Collectors.toList());
    }

    private HigherEducationDashboardDTO.MyProgressDTO computeMyProgress(UUID studentId) {
        List<CompetencyRecord> competencies = competencyRecordRepository.findByStudentIdAndIsDeletedFalse(studentId);
        int competentCount = (int) competencies.stream()
                .filter(c -> c.getStatus() == CompetencyStatus.COMPETENT || c.getStatus() == CompetencyStatus.COMPLETED).count();

        List<Project> projects = projectRepository.findByStudentIdAndIsDeletedFalse(studentId);
        int completedProjects = (int) projects.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();

        AcademicRecord latestRecord = academicRecordRepository.findTopByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId).orElse(null);
        List<StudentCourseEnrollment> enrollments = enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId);
        long completedCourses = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count();

        return HigherEducationDashboardDTO.MyProgressDTO.builder()
                .cumulativeGpa(latestRecord != null ? latestRecord.getCumulativeGpa() : null)
                .semesterGpa(latestRecord != null ? latestRecord.getSemesterGpa() : null)
                .totalCourses(enrollments.size()).completedCourses((int) completedCourses)
                .competenciesCompleted(competentCount).competenciesTotal(competencies.size())
                .projectsCompleted(completedProjects).projectsTotal(projects.size())
                .academicStanding(latestRecord != null ? latestRecord.getAcademicStanding() : null).build();
    }

    private HigherEducationDashboardDTO.MyEvidenceDTO computeMyEvidence(UUID studentId) {
        List<Portfolio> portfolios = portfolioRepository.findByStudentIdAndIsDeletedFalse(studentId);
        int portfolioItems = 0;
        for (Portfolio p : portfolios) {
            portfolioItems += portfolioItemRepository.findByPortfolioIdAndIsDeletedFalse(p.getId()).size();
        }

        long demonstrations = demonstrationRepository.findByStudentIdAndIsDeletedFalse(studentId).size();

        List<Project> projects = projectRepository.findByStudentIdAndIsDeletedFalse(studentId);
        int projectSubmissions = projects.stream()
                .mapToInt(p -> projectSubmissionRepository.findByProjectIdAndIsDeletedFalse(p.getId()).size()).sum();

        List<FieldworkPlacement> placements = fieldworkPlacementRepository.findByStudentIdAndIsDeletedFalse(studentId);
        long logbookEntries = 0;
        for (FieldworkPlacement fp : placements) {
            logbookEntries += logbookEntryRepository.findByPlacementIdAndIsDeletedFalse(fp.getId()).size();
        }

        List<CompetencyRecord> competencyRecords = competencyRecordRepository.findByStudentIdAndIsDeletedFalse(studentId);
        int competenciesRecorded = competencyRecords.size();

        return HigherEducationDashboardDTO.MyEvidenceDTO.builder()
                .portfolioItems(portfolioItems).demonstrations((int) demonstrations)
                .projectSubmissions(projectSubmissions).logbookEntries((int) logbookEntries)
                .competenciesRecorded(competenciesRecorded).build();
    }

    private HigherEducationDashboardDTO.CareerWorldDTO computeCareerWorld(UUID studentId) {
        CareerProfile profile = careerProfileRepository.findByStudentIdAndIsDeletedFalse(studentId).orElse(null);
        boolean hasProfile = profile != null;
        int skillsCount = 0;
        if (profile != null && profile.getSkills() != null && !profile.getSkills().isEmpty()) {
            skillsCount = profile.getSkills().split(",").length;
        }
        return HigherEducationDashboardDTO.CareerWorldDTO.builder()
                .careerObjective(profile != null ? profile.getCareerObjective() : null)
                .targetIndustry(profile != null ? profile.getTargetIndustry() : null)
                .targetRole(profile != null ? profile.getTargetRole() : null)
                .skillsCount(skillsCount).hasProfile(hasProfile).build();
    }

    private HigherEducationDashboardDTO.DayWeekViewDTO computeDayWeek(UUID studentId, LocalDate today) {
        List<StudyTask> allTasks = studyTaskRepository.findByStudentIdAndIsDeletedFalse(studentId);
        LocalDate weekEnd = today.plusDays(7);

        List<HigherEducationDashboardDTO.DayItemDTO> todayItems = allTasks.stream()
                .filter(t -> today.equals(t.getScheduledDate()))
                .map(t -> HigherEducationDashboardDTO.DayItemDTO.builder()
                        .title(t.getTitle()).type(t.getTaskType() != null ? t.getTaskType().name() : "TASK")
                        .time(t.getScheduledTime() != null ? t.getScheduledTime().toString() : null)
                        .date(t.getScheduledDate())
                        .status(Boolean.TRUE.equals(t.getIsCompleted()) ? "DONE" : "PENDING").build())
                .collect(Collectors.toList());

        List<HigherEducationDashboardDTO.DayItemDTO> weekItems = allTasks.stream()
                .filter(t -> t.getScheduledDate() != null && !t.getScheduledDate().isBefore(today) && !t.getScheduledDate().isAfter(weekEnd))
                .map(t -> HigherEducationDashboardDTO.DayItemDTO.builder()
                        .title(t.getTitle()).type(t.getTaskType() != null ? t.getTaskType().name() : "TASK")
                        .time(t.getScheduledTime() != null ? t.getScheduledTime().toString() : null)
                        .date(t.getScheduledDate())
                        .status(Boolean.TRUE.equals(t.getIsCompleted()) ? "DONE" : "PENDING").build())
                .collect(Collectors.toList());

        return HigherEducationDashboardDTO.DayWeekViewDTO.builder()
                .todayItems(todayItems).weekItems(weekItems).build();
    }

    private int computeTaskProgressPercent(UUID studentId) {
        List<StudyTask> tasks = studyTaskRepository.findByStudentIdAndIsDeletedFalse(studentId);
        if (tasks.isEmpty()) return 0;
        long completed = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getIsCompleted())).count();
        return (int) Math.round((completed * 100.0) / tasks.size());
    }

    private int computeEnrollmentProgressPercent(StudentCourseEnrollment enrollment) {
        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) return 100;
        if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
            if (enrollment.getGradePoints() != null && enrollment.getGradePoints() > 0) return 75;
            return 10;
        }
        if (enrollment.getGrade() != null && !enrollment.getGrade().isEmpty()) return 80;
        return 0;
    }

    private StudyTaskDTO toStudyTaskDTO(StudyTask t) {
        return StudyTaskDTO.builder()
                .id(t.getId()).studentId(t.getStudentId()).title(t.getTitle())
                .description(t.getDescription()).taskType(t.getTaskType())
                .priority(t.getPriority()).scheduledDate(t.getScheduledDate())
                .scheduledTime(t.getScheduledTime()).durationMinutes(t.getDurationMinutes())
                .isCompleted(t.getIsCompleted()).notes(t.getNotes()).build();
    }
}
