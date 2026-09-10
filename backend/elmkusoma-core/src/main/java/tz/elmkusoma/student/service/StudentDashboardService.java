package tz.elmkusoma.student.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.attendance.domain.AttendanceRecord;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.domain.SubjectGrade;
import tz.elmkusoma.grading.repository.ReportCardRepository;
import tz.elmkusoma.grading.repository.SubjectGradeRepository;
import tz.elmkusoma.learning.domain.LessonProgress;
import tz.elmkusoma.learning.repository.LessonProgressRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentDashboardService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ReportCardRepository reportCardRepository;
    private final SubjectGradeRepository subjectGradeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LessonProgressRepository lessonProgressRepository;

    public Student getStudentByUserId(UUID userId) {
        return studentRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", userId));
    }

    public Map<String, Object> getDashboardSummary(UUID userId) {
        Student student = getStudentByUserId(userId);
        Map<String, Object> summary = new LinkedHashMap<>();

        summary.put("studentId", student.getId());
        summary.put("admissionNumber", student.getAdmissionNumber());
        summary.put("status", student.getStatus());

        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndIsDeletedFalse(student.getId());
        long activeEnrollments = enrollments.stream()
                .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ENROLLED)
                .count();
        summary.put("totalEnrollments", enrollments.size());
        summary.put("activeEnrollments", activeEnrollments);

        List<ReportCard> reportCards = reportCardRepository.findByStudentIdAndIsDeletedFalse(student.getId());
        summary.put("totalReportCards", reportCards.size());

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        List<AttendanceRecord> monthAttendance = attendanceRecordRepository.findByStudentAndDateRange(
                student.getId(), monthStart, now);
        long presentCount = monthAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT)
                .count();
        summary.put("monthAttendanceTotal", monthAttendance.size());
        summary.put("monthAttendancePresent", presentCount);
        double attendanceRate = monthAttendance.isEmpty() ? 0.0 : (presentCount * 100.0 / monthAttendance.size());
        summary.put("monthAttendanceRate", Math.round(attendanceRate * 10.0) / 10.0);

        List<LessonProgress> allProgress = lessonProgressRepository.findByStudentIdAndIsDeletedFalse(student.getId());
        long completedLessons = allProgress.stream()
                .filter(p -> p.getCompletionPercentage() != null && p.getCompletionPercentage() >= 100)
                .count();
        summary.put("totalLessonsStarted", allProgress.size());
        summary.put("completedLessons", completedLessons);

        if (!reportCards.isEmpty()) {
            double avg = reportCards.stream()
                    .filter(rc -> rc.getAverageMark() != null)
                    .mapToDouble(rc -> rc.getAverageMark().doubleValue())
                    .average()
                    .orElse(0.0);
            summary.put("overallAverage", Math.round(avg * 10.0) / 10.0);
        } else {
            summary.put("overallAverage", 0.0);
        }

        return summary;
    }

    public List<Map<String, Object>> getContinueLearning(UUID userId) {
        Student student = getStudentByUserId(userId);
        List<LessonProgress> progressList = lessonProgressRepository.findByStudentIdAndIsDeletedFalse(student.getId());

        return progressList.stream()
                .filter(p -> p.getCompletionPercentage() != null && p.getCompletionPercentage() < 100)
                .sorted(Comparator.comparing(LessonProgress::getCompletionPercentage).reversed())
                .limit(5)
                .map(p -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("lessonId", p.getLessonId());
                    item.put("completionPercentage", p.getCompletionPercentage());
                    item.put("startedAt", p.getStartedAt());
                    item.put("completedAt", p.getCompletedAt());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRecentActivity(UUID userId) {
        Student student = getStudentByUserId(userId);
        List<Map<String, Object>> activities = new ArrayList<>();

        List<LessonProgress> recentProgress = lessonProgressRepository.findByStudentIdAndIsDeletedFalse(student.getId());
        recentProgress.stream()
                .filter(p -> p.getCompletedAt() != null)
                .sorted(Comparator.comparing(LessonProgress::getCompletedAt).reversed())
                .limit(5)
                .forEach(p -> {
                    Map<String, Object> activity = new LinkedHashMap<>();
                    activity.put("type", "lesson_completed");
                    activity.put("lessonId", p.getLessonId());
                    activity.put("completedAt", p.getCompletedAt());
                    activities.add(activity);
                });

        List<AttendanceRecord> recentAttendance = attendanceRecordRepository.findByStudentAndDateRange(
                student.getId(), LocalDate.now().minusDays(7), LocalDate.now());
        recentAttendance.stream()
                .sorted(Comparator.comparing(AttendanceRecord::getAttendanceDate).reversed())
                .limit(3)
                .forEach(a -> {
                    Map<String, Object> activity = new LinkedHashMap<>();
                    activity.put("type", "attendance");
                    activity.put("date", a.getAttendanceDate());
                    activity.put("status", a.getStatus());
                    activities.add(activity);
                });

        return activities.stream().limit(10).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getStudentResults(UUID userId) {
        Student student = getStudentByUserId(userId);
        List<ReportCard> reportCards = reportCardRepository.findByStudentIdAndIsDeletedFalse(student.getId());

        // Sort by academic year desc, then term desc
        reportCards.sort((a, b) -> {
            int yearCmp = compareNullable(a.getAcademicYearId(), b.getAcademicYearId());
            if (yearCmp != 0) return yearCmp;
            return compareNullable(a.getTermId(), b.getTermId());
        });

        return reportCards.stream().map(rc -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", rc.getId());
            result.put("academicYearId", rc.getAcademicYearId());
            result.put("termId", rc.getTermId());
            result.put("totalMarks", rc.getTotalMarks());
            result.put("averageMark", rc.getAverageMark());
            result.put("classRank", rc.getClassRank());
            result.put("remarks", rc.getRemarks());
            result.put("overallGrade", rc.getOverallGrade());
            result.put("isPublished", rc.getStatus() == ReportCard.ReportCardStatus.PUBLISHED);

            List<SubjectGrade> subjectGrades = subjectGradeRepository.findByReportCardIdAndIsDeletedFalse(rc.getId());
            List<Map<String, Object>> grades = subjectGrades.stream().map(sg -> {
                Map<String, Object> gradeMap = new LinkedHashMap<>();
                gradeMap.put("subjectId", sg.getSubjectId());
                gradeMap.put("marksObtained", sg.getMarksObtained());
                gradeMap.put("grade", sg.getGrade());
                gradeMap.put("gradePoints", sg.getGradePoints());
                gradeMap.put("teacherRemarks", sg.getTeacherRemarks());
                return gradeMap;
            }).collect(Collectors.toList());
            result.put("subjectGrades", grades);

            return result;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getAttendanceSummary(UUID userId) {
        Student student = getStudentByUserId(userId);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        List<AttendanceRecord> monthRecords = attendanceRecordRepository.findByStudentAndDateRange(
                student.getId(), monthStart, today);

        long present = monthRecords.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT).count();
        long absent = monthRecords.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT).count();
        long late = monthRecords.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.LATE).count();
        long excused = monthRecords.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.EXCUSED).count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDays", monthRecords.size());
        summary.put("present", present);
        summary.put("absent", absent);
        summary.put("late", late);
        summary.put("excused", excused);
        double rate = monthRecords.isEmpty() ? 0.0 : (present * 100.0 / monthRecords.size());
        summary.put("attendanceRate", Math.round(rate * 10.0) / 10.0);

        List<Map<String, Object>> records = monthRecords.stream()
                .sorted(Comparator.comparing(AttendanceRecord::getAttendanceDate).reversed())
                .map(a -> {
                    Map<String, Object> rec = new LinkedHashMap<>();
                    rec.put("id", a.getId());
                    rec.put("date", a.getAttendanceDate());
                    rec.put("status", a.getStatus());
                    rec.put("checkInTime", a.getCheckInTime());
                    rec.put("checkOutTime", a.getCheckOutTime());
                    rec.put("remarks", a.getRemarks());
                    return rec;
                })
                .collect(Collectors.toList());

        summary.put("records", records);
        return summary;
    }

    public List<Map<String, Object>> getUpcomingLiveClasses(UUID userId) {
        // Placeholder — live class entity to be created
        return new ArrayList<>();
    }

    private int compareNullable(Comparable a, Comparable b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return b.compareTo(a);
    }
}
