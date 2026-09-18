package tz.elmkusoma.highereducation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HigherEducationDashboardDTO {
    private String academicContext;
    private String programmeName;
    private String departmentName;
    private String academicYear;
    private String semester;
    private WhatsNextDTO whatsNext;
    private TodayDTO today;
    private ContinueLearningDTO continueLearning;
    private LiveCampusDTO liveCampus;
    private List<CourseSummaryDTO> myCourses;
    private AcademicLoadDTO academicLoad;
    private List<CourseSummaryDTO> projects;
    private List<ResearchProjectDTO> research;
    private MyProgressDTO myProgress;
    private MyEvidenceDTO myEvidence;
    private CareerWorldDTO careerWorld;
    private DayWeekViewDTO dayWeekView;
    private List<StudyTaskDTO> studyPlannerTasks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhatsNextDTO {
        private String title;
        private String description;
        private String type;
        private String url;
        private String deadline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayDTO {
        private int totalTasks;
        private int completedTasks;
        private int pendingTasks;
        private List<TodayItemDTO> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayItemDTO {
        private String title;
        private String type;
        private String time;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContinueLearningDTO {
        private String lastCourse;
        private String lastModule;
        private int progressPercent;
        private String courseId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveCampusDTO {
        private int liveNow;
        private int upcomingToday;
        private List<LiveSessionSummaryDTO> sessions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveSessionSummaryDTO {
        private String id;
        private String title;
        private String sessionType;
        private String startTime;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseSummaryDTO {
        private String id;
        private String title;
        private int progressPercent;
        private int totalModules;
        private int completedModules;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcademicLoadDTO {
        private int totalCreditHours;
        private int enrolledCourses;
        private int completedCreditHours;
        private int currentSemesterCourses;
        private Double currentSemesterGpa;
        private Double cumulativeGpa;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyProgressDTO {
        private Double cumulativeGpa;
        private Double semesterGpa;
        private int totalCourses;
        private int completedCourses;
        private int competenciesCompleted;
        private int competenciesTotal;
        private int projectsCompleted;
        private int projectsTotal;
        private String academicStanding;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyEvidenceDTO {
        private int portfolioItems;
        private int demonstrations;
        private int projectSubmissions;
        private int logbookEntries;
        private int competenciesRecorded;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CareerWorldDTO {
        private String careerObjective;
        private String targetIndustry;
        private String targetRole;
        private int skillsCount;
        private boolean hasProfile;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayWeekViewDTO {
        private List<DayItemDTO> todayItems;
        private List<DayItemDTO> weekItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayItemDTO {
        private String title;
        private String type;
        private String time;
        private LocalDate date;
        private String status;
    }
}
