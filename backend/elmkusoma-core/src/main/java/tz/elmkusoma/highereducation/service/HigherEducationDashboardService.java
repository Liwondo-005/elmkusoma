package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tz.elmkusoma.course.domain.Course;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.CourseRepository;
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
    private final CourseRepository courseRepository;
    private final LearningModuleRepository learningModuleRepository;
    private final ProgrammeRepository programmeRepository;
    private final DepartmentRepository departmentRepository;

    public HigherEducationDashboardDTO getDashboard(UUID studentId, UUID institutionId, String learningLevel) {
        HigherEducationDashboardDTO dashboard = new HigherEducationDashboardDTO();
        dashboard.setAcademicContext(learningLevel != null ? learningLevel.toUpperCase() : "COLLEGE");

        LocalDate today = LocalDate.now();

        AcademicRecord latestRecord = academicRecordRepository
                .findTopByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId).orElse(null);
        populateAcademicContext(dashboard, studentId, institutionId, latestRecord);

        dashboard.setWhatsNext(computeWhatsNext(studentId));
        dashboard.setToday(computeToday(studentId, today));
        dashboard.setContinueLearning(computeContinueLearning(studentId));
        dashboard.setLiveCampus(computeLiveCampus(institutionId, today));

        Map<UUID, List<LearningModule>> modulesByCourse = learningModuleRepository
                .findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .filter(m -> m.getCourseId() != null)
                .collect(Collectors.groupingBy(LearningModule::getCourseId));

        List<StudentCourseEnrollment> activeEnrollments = enrollmentRepository
                .findByStudentIdAndIsDeletedFalse(studentId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED
                        || e.getStatus() == EnrollmentStatus.IN_PROGRESS)
                .collect(Collectors.toList());

        dashboard.setMyCourses(activeEnrollments.stream().map(e -> {
            List<LearningModule> courseModules = e.getCourseId() != null
                    ? modulesByCourse.getOrDefault(e.getCourseId(), Collections.emptyList())
                    : Collections.emptyList();
            int totalModules = courseModules.size();
            int completedModules = (int) courseModules.stream()
                    .filter(m -> m.getStatus() == ModuleStatus.COMPLETED).count();
            int progress;
            if (!courseModules.isEmpty()) {
                progress = (int) Math.round(courseModules.stream()
                        .mapToInt(m -> m.getProgressPercent() != null ? m.getProgressPercent() : 0)
                        .average().orElse(0));
            } else {
                progress = e.getStatus() == EnrollmentStatus.COMPLETED ? 100 : 0;
            }
            return HigherEducationDashboardDTO.CourseSummaryDTO.builder()
                    .id(e.getId().toString())
                    .title(resolveCourseTitle(e.getCourseId(), courseModules))
                    .progressPercent(progress).totalModules(totalModules).completedModules(completedModules).build();
        }).collect(Collectors.toList()));

        dashboard.setAcademicLoad(computeAcademicLoad(studentId, latestRecord));
        dashboard.setProjects(computeProjects(studentId));
        dashboard.setResearch(computeResearch(studentId));
        dashboard.setMyProgress(computeMyProgress(studentId));
        dashboard.setMyEvidence(computeMyEvidence(studentId));
        dashboard.setCareerWorld(computeCareerWorld(studentId));
        dashboard.setDayWeekView(computeDayWeek(studentId, today));

        List<StudyTask> allTasks = studyTaskRepository.findByStudentIdAndIsDeletedFalse(studentId);
        dashboard.setStudyPlannerTasks(allTasks.stream().limit(10).map(this::toStudyTaskDTO).collect(Collectors.toList()));

        if (latestRecord != null && latestRecord.getSemester() != null) {
            Double computedSemesterGpa = gpaCalculationService.computeSemesterGpa(
                    studentId, latestRecord.getSemester(), latestRecord.getAcademicYear());
            if (computedSemesterGpa != null) dashboard.getAcademicLoad().setCurrentSemesterGpa(computedSemesterGpa);
        }
        Double computedCumulativeGpa = gpaCalculationService.computeCumulativeGpa(studentId);
        if (computedCumulativeGpa != null) dashboard.getAcademicLoad().setCumulativeGpa(computedCumulativeGpa);

        return dashboard;
    }

    private void populateAcademicContext(HigherEducationDashboardDTO dashboard, UUID studentId,
                                         UUID institutionId, AcademicRecord latestRecord) {
        UUID programmeId = latestRecord != null ? latestRecord.getProgrammeId() : null;
        String academicYear = latestRecord != null ? latestRecord.getAcademicYear() : null;
        String semester = latestRecord != null ? latestRecord.getSemester() : null;

        if (programmeId == null || academicYear == null || semester == null) {
            for (StudentCourseEnrollment e : enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId)) {
                if (programmeId == null && e.getProgrammeId() != null) programmeId = e.getProgrammeId();
                if (academicYear == null && e.getAcademicYear() != null) academicYear = e.getAcademicYear();
                if (semester == null && e.getSemester() != null) semester = e.getSemester();
                if (programmeId != null && academicYear != null && semester != null) break;
            }
        }

        if (programmeId != null) {
            UUID contextProgrammeId = programmeId;
            Programme programme = programmeRepository.findById(contextProgrammeId).orElse(null);
            if (programme != null) {
                dashboard.setProgrammeName(programme.getName());
            }
            if (institutionId != null) {
                departmentRepository.findByInstitutionIdAndIsDeletedFalse(institutionId).stream()
                        .filter(d -> d.getProgrammeIds() != null && d.getProgrammeIds().contains(contextProgrammeId))
                        .findFirst()
                        .ifPresent(d -> dashboard.setDepartmentName(d.getName()));
            }
        }
        dashboard.setAcademicYear(academicYear);
        dashboard.setSemester(semester);
    }

    private String resolveCourseTitle(UUID courseId, List<LearningModule> courseModules) {
        if (courseId != null) {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course != null && course.getTitle() != null && !course.getTitle().isBlank()) {
                return course.getTitle();
            }
        }
        return courseModules.isEmpty() ? "Course" : courseModules.get(0).getModuleTitle();
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

    private HigherEducationDashboardDTO.AcademicLoadDTO computeAcademicLoad(UUID studentId, AcademicRecord latestRecord) {
        List<StudentCourseEnrollment> allEnrollments = enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId);
        long activeCount = allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED || e.getStatus() == EnrollmentStatus.IN_PROGRESS).count();
        int totalCredits = allEnrollments.stream().filter(e -> e.getCreditHours() != null).mapToInt(StudentCourseEnrollment::getCreditHours).sum();
        int completedCredits = allEnrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED && e.getCreditHours() != null).mapToInt(StudentCourseEnrollment::getCreditHours).sum();
        OptionalDouble avgGpa = allEnrollments.stream().filter(e -> e.getGradePoints() != null).mapToDouble(StudentCourseEnrollment::getGradePoints).average();

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

    private StudyTaskDTO toStudyTaskDTO(StudyTask t) {
        return StudyTaskDTO.builder()
                .id(t.getId()).studentId(t.getStudentId()).title(t.getTitle())
                .description(t.getDescription()).taskType(t.getTaskType())
                .priority(t.getPriority()).scheduledDate(t.getScheduledDate())
                .scheduledTime(t.getScheduledTime()).durationMinutes(t.getDurationMinutes())
                .isCompleted(t.getIsCompleted()).notes(t.getNotes()).build();
    }
}
