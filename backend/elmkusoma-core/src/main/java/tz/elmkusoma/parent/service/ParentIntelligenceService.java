package tz.elmkusoma.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.domain.AssessmentResult;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.assessment.repository.AssessmentResultRepository;
import tz.elmkusoma.attendance.domain.AttendanceRecord;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.domain.SubjectGrade;
import tz.elmkusoma.grading.repository.ReportCardRepository;
import tz.elmkusoma.grading.repository.SubjectGradeRepository;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.domain.AssignmentSubmission;
import tz.elmkusoma.learning.domain.LessonProgress;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.learning.repository.LessonProgressRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.parent.domain.Achievement;
import tz.elmkusoma.parent.domain.LearningGoal;
import tz.elmkusoma.parent.dto.ParentIntelligenceResponse;
import tz.elmkusoma.parent.dto.ParentIntelligenceResponse.*;
import tz.elmkusoma.parent.repository.AchievementRepository;
import tz.elmkusoma.parent.repository.LearningGoalRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentIntelligenceService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LiveClassRepository liveClassRepository;
    private final LiveClassParticipantRepository liveClassParticipantRepository;
    private final ReportCardRepository reportCardRepository;
    private final SubjectGradeRepository subjectGradeRepository;
    private final AchievementRepository achievementRepository;
    private final LearningGoalRepository learningGoalRepository;

    public ParentIntelligenceResponse getIntelligence(UUID childId, UUID institutionId) {
        List<AttentionItem> attention = new ArrayList<>();
        List<PositiveSignal> positive = new ArrayList<>();
        List<RecommendationItem> recommendations = new ArrayList<>();
        List<UpcomingItem> upcoming = new ArrayList<>();

        generateAttentionSignals(childId, institutionId, attention, recommendations);
        generatePositiveSignals(childId, institutionId, positive);
        generateUpcomingItems(childId, institutionId, upcoming);
        WeeklyBrief weeklyBrief = generateWeeklyBrief(childId, institutionId);

        return ParentIntelligenceResponse.builder()
                .childId(childId.toString())
                .needsAttention(attention)
                .doingWell(positive)
                .recommendations(recommendations)
                .weeklyBrief(weeklyBrief)
                .upcoming(upcoming)
                .build();
    }

    private void generateAttentionSignals(UUID childId, UUID institutionId,
                                          List<AttentionItem> attention, List<RecommendationItem> recommendations) {
        LocalDateTime now = LocalDateTime.now();

        List<Assignment> pendingAssignments = assignmentRepository
                .findByClassGroupIdAndIsDeletedFalse(null);
        List<UUID> submittedAssignmentIds = assignmentSubmissionRepository
                .findByStudentIdAndIsDeletedFalse(childId).stream()
                .map(AssignmentSubmission::getAssignmentId)
                .collect(Collectors.toList());

        List<Assignment> overdueAssignments = pendingAssignments.stream()
                .filter(a -> a.getDueDate() != null && a.getDueDate().isBefore(now)
                        && !submittedAssignmentIds.contains(a.getId())
                        && a.getStatus() == null || "ACTIVE".equals(a.getStatus()))
                .collect(Collectors.toList());

        List<Assignment> dueSoonAssignments = pendingAssignments.stream()
                .filter(a -> a.getDueDate() != null
                        && a.getDueDate().isAfter(now)
                        && a.getDueDate().isBefore(now.plusDays(3))
                        && !submittedAssignmentIds.contains(a.getId()))
                .collect(Collectors.toList());

        if (!overdueAssignments.isEmpty()) {
            Assignment oldest = overdueAssignments.get(0);
            attention.add(AttentionItem.builder()
                    .id(UUID.randomUUID().toString())
                    .type("ASSIGNMENT_OVERDUE")
                    .priority("CRITICAL")
                    .title(overdueAssignments.size() + " assignment" + (overdueAssignments.size() > 1 ? "s" : "") + " overdue")
                    .description("There " + (overdueAssignments.size() > 1 ? "are" : "is") + " " + overdueAssignments.size() + " overdue assignment" + (overdueAssignments.size() > 1 ? "s" : "") + ".")
                    .timestamp(now)
                    .relatedEntityType("assignment")
                    .relatedEntityId(oldest.getId().toString())
                    .actionLabel("View Assignments")
                    .actionUrl("/assignments")
                    .build());
        }

        if (!dueSoonAssignments.isEmpty()) {
            Assignment next = dueSoonAssignments.get(0);
            attention.add(AttentionItem.builder()
                    .id(UUID.randomUUID().toString())
                    .type("ASSIGNMENT_DUE_SOON")
                    .priority("IMPORTANT")
                    .title(dueSoonAssignments.size() + " assignment" + (dueSoonAssignments.size() > 1 ? "s" : "") + " due soon")
                    .description("Due within the next 3 days.")
                    .timestamp(now)
                    .relatedEntityType("assignment")
                    .relatedEntityId(next.getId().toString())
                    .actionLabel("View Assignments")
                    .actionUrl("/assignments")
                    .build());

            recommendations.add(RecommendationItem.builder()
                    .id(UUID.randomUUID().toString())
                    .type("ASSIGNMENT_ENCOURAGE")
                    .title("Encourage assignment completion")
                    .description(next.getTitle() + " is due soon. Encourage your child to complete it.")
                    .actionLabel("View Assignment")
                    .actionUrl("/assignments")
                    .evidenceSource("assignment_due_date")
                    .build());
        }

        List<AttendanceRecord> recentAttendance = attendanceRecordRepository
                .findByStudentAndDateRange(
                        childId, now.minusDays(30).toLocalDate(), now.toLocalDate());

        long recentAbsentCount = recentAttendance.stream()
                .filter(a -> "ABSENT".equals(a.getStatus()))
                .count();
        if (recentAbsentCount >= 3) {
            attention.add(AttentionItem.builder()
                    .id(UUID.randomUUID().toString())
                    .type("ATTENDANCE_PATTERN")
                    .priority("IMPORTANT")
                    .title("Attendance pattern noted")
                    .description(recentAbsentCount + " absences recorded in the last 30 days.")
                    .timestamp(now)
                    .relatedEntityType("attendance")
                    .actionLabel("View Attendance")
                    .actionUrl("/attendance")
                    .build());

            recommendations.add(RecommendationItem.builder()
                    .id(UUID.randomUUID().toString())
                    .type("ATTENDANCE_REVIEW")
                    .title("Review attendance")
                    .description("Your child has had " + recentAbsentCount + " absences recently. Review the attendance record.")
                    .actionLabel("View Attendance")
                    .actionUrl("/attendance")
                    .evidenceSource("attendance_records")
                    .build());
        }

        List<AssessmentResult> recentResults = assessmentResultRepository
                .findByStudentIdAndIsDeletedFalse(childId).stream()
                .sorted(Comparator.comparing(AssessmentResult::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());

        if (recentResults.size() >= 2) {
            Integer first = recentResults.get(recentResults.size() - 1).getTotalScore();
            Integer latest = recentResults.get(0).getTotalScore();
            if (first != null && latest != null && latest < first - 10) {
                attention.add(AttentionItem.builder()
                        .id(UUID.randomUUID().toString())
                        .type("ASSESSMENT_DECLINE")
                        .priority("IMPORTANT")
                        .title("Assessment performance change noted")
                        .description("Latest score: " + latest + "%. Previous score: " + first + "%.")
                        .timestamp(now)
                        .relatedEntityType("assessment")
                        .relatedEntityId(recentResults.get(0).getAssessmentId().toString())
                        .actionLabel("View Assessments")
                        .actionUrl("/assessments")
                        .build());

                recommendations.add(RecommendationItem.builder()
                        .id(UUID.randomUUID().toString())
                        .type("ASSESSMENT_REVIEW")
                        .title("Review recent assessments")
                        .description("Assessment scores have changed. Consider reviewing the relevant learning materials with your child.")
                        .actionLabel("View Details")
                        .actionUrl("/assessments")
                        .evidenceSource("assessment_results")
                        .build());
            }
        }

        if (!attention.isEmpty() && recommendations.isEmpty()) {
            recommendations.add(RecommendationItem.builder()
                    .id(UUID.randomUUID().toString())
                    .type("GENERAL_REVIEW")
                    .title("Review your child's learning activity")
                    .description("Check upcoming assignments and recent performance.")
                    .actionLabel("View Dashboard")
                    .actionUrl("/")
                    .evidenceSource("general")
                    .build());
        }
    }

    private void generatePositiveSignals(UUID childId, UUID institutionId,
                                         List<PositiveSignal> positive) {
        LocalDateTime now = LocalDateTime.now();

        List<AssignmentSubmission> recentSubmissions = assignmentSubmissionRepository
                .findByStudentIdAndIsDeletedFalse(childId).stream()
                .filter(s -> s.getSubmittedAt() != null && s.getSubmittedAt().isAfter(now.minusDays(7)))
                .collect(Collectors.toList());

        if (!recentSubmissions.isEmpty()) {
            positive.add(PositiveSignal.builder()
                    .id(UUID.randomUUID().toString())
                    .type("ASSIGNMENTS_SUBMITTED")
                    .title(recentSubmissions.size() + " assignment" + (recentSubmissions.size() > 1 ? "s" : "") + " submitted this week")
                    .description("Assignments have been submitted on time.")
                    .timestamp(now)
                    .relatedEntityType("assignment")
                    .build());
        }

        List<LessonProgress> completedLessons = lessonProgressRepository
                .findByStudentIdAndIsDeletedFalse(childId).stream()
                .filter(lp -> lp.getCompletedAt() != null && lp.getCompletedAt().isAfter(now.minusDays(7)))
                .collect(Collectors.toList());

        if (!completedLessons.isEmpty()) {
            positive.add(PositiveSignal.builder()
                    .id(UUID.randomUUID().toString())
                    .type("LESSONS_COMPLETED")
                    .title(completedLessons.size() + " lesson" + (completedLessons.size() > 1 ? "s" : "") + " completed")
                    .description("Lessons have been completed recently.")
                    .timestamp(now)
                    .relatedEntityType("lesson")
                    .build());
        }

        List<LiveClassParticipant> attendedLive = liveClassParticipantRepository
                .findByUserIdAndIsDeletedFalse(childId).stream()
                .filter(p -> p.getJoinedAt() != null && p.getJoinedAt().isAfter(now.minusDays(7)))
                .collect(Collectors.toList());

        if (!attendedLive.isEmpty()) {
            positive.add(PositiveSignal.builder()
                    .id(UUID.randomUUID().toString())
                    .type("LIVE_CLASSES_ATTENDED")
                    .title(attendedLive.size() + " live class" + (attendedLive.size() > 1 ? "es" : "") + " attended")
                    .description("Active participation in live learning sessions.")
                    .timestamp(now)
                    .relatedEntityType("live_class")
                    .build());
        }

        List<Achievement> recentAchievements = achievementRepository
                .findByStudentIdAndIsDeletedFalseOrderByAchievedAtDesc(childId).stream()
                .filter(a -> a.getAchievedAt() != null && a.getAchievedAt().isAfter(now.minusDays(30)))
                .collect(Collectors.toList());

        for (Achievement ach : recentAchievements) {
            positive.add(PositiveSignal.builder()
                    .id(ach.getId().toString())
                    .type("ACHIEVEMENT")
                    .title(ach.getTitle())
                    .description(ach.getDescription())
                    .timestamp(ach.getAchievedAt())
                    .relatedEntityType(ach.getRelatedEntityType())
                    .relatedEntityId(ach.getRelatedEntityId() != null ? ach.getRelatedEntityId().toString() : null)
                    .build());
        }

        List<LessonProgress> allCompleted = lessonProgressRepository
                .findByStudentIdAndIsDeletedFalse(childId).stream()
                .filter(lp -> lp.getCompletedAt() != null)
                .collect(Collectors.toList());

        if (allCompleted.size() >= 10 && allCompleted.size() % 10 == 0) {
            positive.add(PositiveSignal.builder()
                    .id(UUID.randomUUID().toString())
                    .type("MILESTONE")
                    .title(allCompleted.size() + " learning milestones reached")
                    .description("Consistent learning activity demonstrated.")
                    .timestamp(now)
                    .relatedEntityType("milestone")
                    .build());
        }

        List<ReportCard> reportCards = reportCardRepository
                .findByStudentIdAndIsDeletedFalse(childId).stream()
                .filter(rc -> rc.getAverageMark() != null)
                .sorted(Comparator.comparing(ReportCard::getCreatedAt).reversed())
                .limit(2)
                .collect(Collectors.toList());

        if (reportCards.size() >= 2) {
            java.math.BigDecimal previous = reportCards.get(1).getAverageMark();
            java.math.BigDecimal latest = reportCards.get(0).getAverageMark();
            if (previous != null && latest != null && latest.compareTo(previous) > 0) {
                positive.add(PositiveSignal.builder()
                        .id(UUID.randomUUID().toString())
                        .type("PERFORMANCE_IMPROVED")
                        .title("Overall performance improved")
                        .description("Average mark increased from " + previous + "% to " + latest + "%.")
                        .timestamp(reportCards.get(0).getCreatedAt())
                        .relatedEntityType("report_card")
                        .relatedEntityId(reportCards.get(0).getId().toString())
                        .build());
            }
        }

        List<LearningGoal> completedGoals = learningGoalRepository
                .findByStudentIdAndStatusAndIsDeletedFalse(childId, "COMPLETED");

        if (!completedGoals.isEmpty()) {
            positive.add(PositiveSignal.builder()
                    .id(UUID.randomUUID().toString())
                    .type("GOALS_COMPLETED")
                    .title(completedGoals.size() + " learning goal" + (completedGoals.size() > 1 ? "s" : "") + " completed")
                    .description("Learning goals have been achieved.")
                    .timestamp(now)
                    .relatedEntityType("learning_goal")
                    .build());
        }
    }

    private void generateUpcomingItems(UUID childId, UUID institutionId,
                                       List<UpcomingItem> upcoming) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekEnd = now.plusDays(7);

        List<Assignment> upcomingAssignments = assignmentRepository
                .findByClassGroupIdAndIsDeletedFalse(null).stream()
                .filter(a -> a.getDueDate() != null
                        && a.getDueDate().isAfter(now)
                        && a.getDueDate().isBefore(weekEnd))
                .sorted(Comparator.comparing(Assignment::getDueDate))
                .limit(5)
                .collect(Collectors.toList());

        for (Assignment a : upcomingAssignments) {
            upcoming.add(UpcomingItem.builder()
                    .id(a.getId().toString())
                    .type("ASSIGNMENT")
                    .title(a.getTitle())
                    .description("Due: " + a.getDueDate())
                    .timestamp(a.getDueDate())
                    .relatedEntityType("assignment")
                    .relatedEntityId(a.getId().toString())
                    .actionLabel("View Assignment")
                    .build());
        }

        List<LiveClass> upcomingLive = liveClassRepository
                .findByInstitutionIdAndIsDeletedFalse(institutionId != null ? institutionId : null)
                .stream()
                .filter(lc -> lc.getScheduledAt() != null
                        && lc.getScheduledAt().isAfter(now)
                        && lc.getScheduledAt().isBefore(weekEnd)
                        && !"CANCELLED".equals(lc.getStatus()))
                .sorted(Comparator.comparing(LiveClass::getScheduledAt))
                .limit(5)
                .collect(Collectors.toList());

        for (LiveClass lc : upcomingLive) {
            upcoming.add(UpcomingItem.builder()
                    .id(lc.getId().toString())
                    .type("LIVE_CLASS")
                    .title(lc.getTitle())
                    .description("Scheduled: " + lc.getScheduledAt())
                    .timestamp(lc.getScheduledAt())
                    .relatedEntityType("live_class")
                    .relatedEntityId(lc.getId().toString())
                    .actionLabel("View Class")
                    .build());
        }

        List<Assessment> upcomingAssessments = assessmentRepository
                .findAll().stream()
                .filter(a -> a.getCreatedAt() != null
                        && a.getCreatedAt().isAfter(now.minusDays(7))
                        && a.getCreatedAt().isBefore(weekEnd))
                .sorted(Comparator.comparing(Assessment::getCreatedAt))
                .limit(3)
                .collect(Collectors.toList());

        for (Assessment a : upcomingAssessments) {
            upcoming.add(UpcomingItem.builder()
                    .id(a.getId().toString())
                    .type("ASSESSMENT")
                    .title(a.getTitle())
                    .description("Assessment upcoming")
                    .timestamp(a.getCreatedAt())
                    .relatedEntityType("assessment")
                    .relatedEntityId(a.getId().toString())
                    .actionLabel("View Assessment")
                    .build());
        }

        upcoming.sort(Comparator.comparing(UpcomingItem::getTimestamp));
    }

    private WeeklyBrief generateWeeklyBrief(UUID childId, UUID institutionId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate weekStart = now.toLocalDate().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        long completedLessons = lessonProgressRepository
                .findByStudentIdAndIsDeletedFalse(childId).stream()
                .filter(lp -> lp.getCompletedAt() != null
                        && lp.getCompletedAt().toLocalDate().isAfter(weekStart.minusDays(1))
                        && lp.getCompletedAt().toLocalDate().isBefore(weekEnd.plusDays(1)))
                .count();

        List<AssignmentSubmission> weekSubmissions = assignmentSubmissionRepository
                .findByStudentIdAndIsDeletedFalse(childId).stream()
                .filter(s -> s.getSubmittedAt() != null
                        && s.getSubmittedAt().toLocalDate().isAfter(weekStart.minusDays(1))
                        && s.getSubmittedAt().toLocalDate().isBefore(weekEnd.plusDays(1)))
                .collect(Collectors.toList());

        long pendingAssignments = assignmentRepository.findByClassGroupIdAndIsDeletedFalse(null).stream()
                .filter(a -> a.getDueDate() != null && a.getDueDate().isAfter(now))
                .count();

        long overdueAssignments = assignmentRepository.findByClassGroupIdAndIsDeletedFalse(null).stream()
                .filter(a -> a.getDueDate() != null && a.getDueDate().isBefore(now))
                .count();

        long attendedLive = liveClassParticipantRepository
                .findByUserIdAndIsDeletedFalse(childId).stream()
                .filter(p -> p.getJoinedAt() != null
                        && p.getJoinedAt().toLocalDate().isAfter(weekStart.minusDays(1))
                        && p.getJoinedAt().toLocalDate().isBefore(weekEnd.plusDays(1)))
                .count();

        List<AssessmentResult> weekAssessments = assessmentResultRepository
                .findByStudentIdAndIsDeletedFalse(childId).stream()
                .filter(r -> r.getCreatedAt() != null
                        && r.getCreatedAt().toLocalDate().isAfter(weekStart.minusDays(1))
                        && r.getCreatedAt().toLocalDate().isBefore(weekEnd.plusDays(1)))
                .collect(Collectors.toList());

        List<AttendanceRecord> weekAttendance = attendanceRecordRepository
                .findByStudentAndDateRange(childId, weekStart, weekEnd);

        long presentDays = weekAttendance.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long absentDays = weekAttendance.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        long lateDays = weekAttendance.stream().filter(a -> "LATE".equals(a.getStatus())).count();

        List<String> highlights = new ArrayList<>();
        if (!weekSubmissions.isEmpty()) {
            highlights.add(weekSubmissions.size() + " assignment" + (weekSubmissions.size() > 1 ? "s" : "") + " submitted");
        }
        if (completedLessons > 0) {
            highlights.add(completedLessons + " lesson" + (completedLessons > 1 ? "s" : "") + " completed");
        }
        if (attendedLive > 0) {
            highlights.add(attendedLive + " live class" + (attendedLive > 1 ? "es" : "") + " attended");
        }

        List<String> focusNextWeek = new ArrayList<>();
        if (overdueAssignments > 0) {
            focusNextWeek.add("Complete " + overdueAssignments + " overdue assignment" + (overdueAssignments > 1 ? "s" : ""));
        }
        if (pendingAssignments > 0) {
            focusNextWeek.add(pendingAssignments + " upcoming assignment" + (pendingAssignments > 1 ? "s" : "") + " to complete");
        }

        List<UpcomingItem> upcomingThisWeek = new ArrayList<>();
        List<LiveClass> nextWeekLive = liveClassRepository
                .findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .filter(lc -> lc.getScheduledAt() != null
                        && lc.getScheduledAt().toLocalDate().isAfter(now.toLocalDate())
                        && lc.getScheduledAt().toLocalDate().isBefore(now.toLocalDate().plusDays(8)))
                .sorted(Comparator.comparing(LiveClass::getScheduledAt))
                .limit(5)
                .collect(Collectors.toList());

        for (LiveClass lc : nextWeekLive) {
            upcomingThisWeek.add(UpcomingItem.builder()
                    .id(lc.getId().toString())
                    .type("LIVE_CLASS")
                    .title(lc.getTitle())
                    .description("Scheduled: " + lc.getScheduledAt())
                    .timestamp(lc.getScheduledAt())
                    .relatedEntityType("live_class")
                    .relatedEntityId(lc.getId().toString())
                    .actionLabel("View Class")
                    .build());
        }

        return WeeklyBrief.builder()
                .lessonsCompleted((int) completedLessons)
                .assignmentsCompleted(weekSubmissions.size())
                .assignmentsPending((int) pendingAssignments)
                .assignmentsOverdue((int) overdueAssignments)
                .liveClassesAttended((int) attendedLive)
                .assessmentsCompleted(weekAssessments.size())
                .presentDays((int) presentDays)
                .absentDays((int) absentDays)
                .lateDays((int) lateDays)
                .highlights(highlights)
                .focusNextWeek(focusNextWeek)
                .upcomingThisWeek(upcomingThisWeek)
                .build();
    }
}
