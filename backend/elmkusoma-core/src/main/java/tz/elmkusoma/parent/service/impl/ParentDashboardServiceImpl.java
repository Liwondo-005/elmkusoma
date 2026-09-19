package tz.elmkusoma.parent.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.domain.AcademicYear;
import tz.elmkusoma.academic.domain.Term;
import tz.elmkusoma.academic.repository.AcademicYearRepository;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.repository.TermRepository;
import tz.elmkusoma.attendance.domain.AttendanceRecord;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.attendance.repository.AttendanceSummaryRepository;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.domain.AssessmentResult;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.assessment.repository.AssessmentResultRepository;
import tz.elmkusoma.course.domain.Announcement;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.AnnouncementRepository;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.domain.LessonProgress;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.repository.ReportCardRepository;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.LessonProgressRepository;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.ParentStudentLink;
import tz.elmkusoma.parent.dto.response.*;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.parent.service.ParentDashboardService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentDashboardServiceImpl implements ParentDashboardService {

    private final ParentRepository parentRepository;
    private final ParentStudentLinkRepository studentLinkRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSummaryRepository attendanceSummaryRepository;
    private final AssignmentRepository assignmentRepository;
    private final ReportCardRepository reportCardRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final LiveClassRepository liveClassRepository;
    private final ClassGroupRepository classGroupRepository;
    private final LessonRepository lessonRepository;
    private final SubjectRepository subjectRepository;
    private final AnnouncementRepository announcementRepository;
    private final TermRepository termRepository;
    private final AcademicYearRepository academicYearRepository;

    private Parent resolveParentStrict(UUID userId) {
        List<Parent> allParents = parentRepository.findAllByUserId(userId);
        if (allParents.isEmpty()) {
            throw new ResourceNotFoundException("Parent profile", "userId", userId);
        }
        return allParents.get(0);
    }

    private boolean isAuthorizedChild(UUID userId, UUID studentId) {
        Parent parent = resolveParentStrict(userId);
        return studentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(parent.getId(), studentId);
    }

    private Student getStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .filter(s -> !s.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
    }

    private String getStudentName(Student student) {
        User user = userRepository.findById(student.getUserId()).orElse(null);
        if (user != null) {
            return user.getFullName();
        }
        return student.getAdmissionNumber();
    }

    @Override
    public FamilyOverviewResponse getFamilyOverview(UUID userId) {
        Parent parent = resolveParentStrict(userId);
        User parentUser = userRepository.findById(parent.getUserId()).orElse(null);
        String parentName = parentUser != null ? parentUser.getFullName() : "";

        List<ParentStudentLink> links = studentLinkRepository.findAllByParentId(parent.getId());
        List<FamilyOverviewResponse.ChildSummary> children = new ArrayList<>();
        List<FamilyOverviewResponse.ActionItem> todayActions = new ArrayList<>();

        for (ParentStudentLink link : links) {
            Student student = getStudent(link.getStudentId());
            String studentName = getStudentName(student);

            Double attendancePct = calculateAttendancePercentage(student.getId());
            String latestGrade = getLatestGrade(student.getId());

            String className = resolveStudentClassName(student.getId());

            children.add(FamilyOverviewResponse.ChildSummary.builder()
                .studentId(student.getId().toString())
                    .name(studentName)
                    .admissionNumber(student.getAdmissionNumber())
                    .className(className)
                    .educationLevel(className)
                    .relationshipType(link.getRelationshipType().name())
                    .attendancePercentage(attendancePct)
                    .latestGrade(latestGrade)
                    .isPrimary(link.getIsPrimary())
                    .build());

            List<Assignment> overdue = getOverdueAssignments(student.getId());
            for (Assignment a : overdue) {
                todayActions.add(FamilyOverviewResponse.ActionItem.builder()
                        .type("ASSIGNMENT_OVERDUE")
                        .title(a.getTitle())
                        .detail("Overdue assignment")
                        .childName(studentName)
                        .date(a.getDueDate() != null ? a.getDueDate().toLocalDate() : null)
                        .priority("HIGH")
                        .build());
            }

            if (attendancePct != null && attendancePct < 75) {
                todayActions.add(FamilyOverviewResponse.ActionItem.builder()
                        .type("ATTENDANCE_CONCERN")
                        .title("Attendance below 75%")
                        .detail(String.format("Current attendance: %.0f%%", attendancePct))
                        .childName(studentName)
                        .date(LocalDate.now())
                        .priority("HIGH")
                        .build());
            }
        }

        todayActions.sort(Comparator.comparing(
                (FamilyOverviewResponse.ActionItem a) -> "HIGH".equals(a.getPriority()) ? 0 : 1)
                .thenComparing(Comparator.comparing(FamilyOverviewResponse.ActionItem::getDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))));

        return FamilyOverviewResponse.builder()
                .parentName(parentName)
                .totalChildren(links.size())
                .children(children)
                .todayActions(todayActions)
                .build();
    }

    @Override
    public List<ChildOverviewResponse> getMyChildren(UUID userId) {
        Parent parent = resolveParentStrict(userId);
        List<ParentStudentLink> links = studentLinkRepository.findAllByParentId(parent.getId());

        return links.stream().map(link -> {
            Student student = getStudent(link.getStudentId());
            return buildChildOverview(student, link);
        }).toList();
    }

    @Override
    public ChildOverviewResponse getChildOverview(UUID userId, UUID studentId) {
        if (!isAuthorizedChild(userId, studentId)) {
            throw new ResourceNotFoundException("Student link", "studentId", studentId);
        }
        Parent parent = resolveParentStrict(userId);
        Student student = getStudent(studentId);
        ParentStudentLink link = studentLinkRepository.findAllByParentId(parent.getId()).stream()
                .filter(l -> l.getStudentId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Student link", "studentId", studentId));
        return buildChildOverview(student, link);
    }

    @Override
    public ParentAttendanceResponse getChildAttendance(UUID userId, UUID studentId) {
        if (!isAuthorizedChild(userId, studentId)) {
            throw new ResourceNotFoundException("Student link", "studentId", studentId);
        }
        Student student = getStudent(studentId);
        String studentName = getStudentName(student);

        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByStudentAndDateRange(studentId, threeMonthsAgo, LocalDate.now());

        long present = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT).count();
        long absent = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT).count();
        long late = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.LATE).count();
        long excused = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.EXCUSED).count();
        long total = records.size();
        double pct = total > 0 ? (present * 100.0 / total) : 0;

        List<ParentAttendanceResponse.AttendanceDay> recentDays = records.stream()
                .sorted(Comparator.comparing(AttendanceRecord::getAttendanceDate).reversed())
                .limit(30)
                .map(r -> ParentAttendanceResponse.AttendanceDay.builder()
                        .date(r.getAttendanceDate())
                        .status(r.getStatus().name())
                        .remarks(r.getRemarks())
                        .build())
                .toList();

        return ParentAttendanceResponse.builder()
                .studentName(studentName)
                .className(resolveStudentClassName(studentId))
                .attendancePercentage(java.math.BigDecimal.valueOf(pct))
                .totalDays(total)
                .presentDays(present)
                .absentDays(absent)
                .lateDays(late)
                .excusedDays(excused)
                .recentDays(recentDays)
                .build();
    }

    @Override
    public ParentAssignmentResponse getChildAssignments(UUID userId, UUID studentId) {
        if (!isAuthorizedChild(userId, studentId)) {
            throw new ResourceNotFoundException("Student link", "studentId", studentId);
        }
        Student student = getStudent(studentId);
        String studentName = getStudentName(student);
        List<UUID> classGroupIds = getStudentClassGroupIds(studentId);

        List<Assignment> allAssignments = new ArrayList<>();
        for (UUID cgId : classGroupIds) {
            allAssignments.addAll(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(cgId));
        }
        List<ParentAssignmentResponse.AssignmentItem> pending = new ArrayList<>();
        List<ParentAssignmentResponse.AssignmentItem> completed = new ArrayList<>();
        List<ParentAssignmentResponse.AssignmentItem> overdue = new ArrayList<>();

        for (Assignment a : allAssignments) {
            String subjectName = a.getClassGroupId() != null ? resolveClassName(a.getClassGroupId()) : "General";
            ParentAssignmentResponse.AssignmentItem item = ParentAssignmentResponse.AssignmentItem.builder()
                    .id(a.getId().toString())
                    .title(a.getTitle())
                    .subject(subjectName)
                    .dueDate(a.getDueDate())
                    .totalMarks(a.getTotalMarks())
                    .build();

            if (a.getDueDate() != null && a.getDueDate().isBefore(java.time.LocalDateTime.now())) {
                item.setStatus("OVERDUE");
                overdue.add(item);
            } else {
                item.setStatus("PENDING");
                pending.add(item);
            }
        }

        return ParentAssignmentResponse.builder()
                .studentName(studentName)
                .className(resolveStudentClassName(studentId))
                .pending(pending)
                .completed(completed)
                .overdue(overdue)
                .build();
    }

    @Override
    public ParentResultResponse getChildResults(UUID userId, UUID studentId) {
        if (!isAuthorizedChild(userId, studentId)) {
            throw new ResourceNotFoundException("Student link", "studentId", studentId);
        }
        Student student = getStudent(studentId);
        String studentName = getStudentName(student);

        List<ReportCard> reportCards = reportCardRepository.findByStudentIdAndIsDeletedFalse(studentId);
        String className = resolveStudentClassName(studentId);
        List<ParentResultResponse.ReportCardItem> items = reportCards.stream()
                .sorted(Comparator.comparing(ReportCard::getPublishedAt).reversed())
                .map(rc -> {
                    String termName = resolveTermName(rc.getTermId());
                    String yearName = resolveAcademicYearName(rc.getAcademicYearId());
                    return ParentResultResponse.ReportCardItem.builder()
                            .id(rc.getId().toString())
                            .term(termName)
                            .academicYear(yearName)
                            .overallGrade(rc.getOverallGrade())
                            .averageMark(rc.getAverageMark() != null ? rc.getAverageMark().doubleValue() : null)
                            .gpa(rc.getGpa() != null ? rc.getGpa().doubleValue() : null)
                            .classRank(rc.getClassRank())
                            .totalStudentsInClass(rc.getTotalStudentsInClass())
                            .remarks(rc.getRemarks())
                            .status(rc.getStatus().name())
                            .publishedAt(rc.getPublishedAt())
                            .build();
                })
                .toList();

        return ParentResultResponse.builder()
                .studentName(studentName)
                .className(className)
                .reportCards(items)
                .build();
    }

    private ChildOverviewResponse buildChildOverview(Student student, ParentStudentLink link) {
        String studentName = getStudentName(student);
        String className = resolveStudentClassName(student.getId());

        Double attendancePct = calculateAttendancePercentage(student.getId());
        long totalAttendance = attendanceRecordRepository.findByStudentIdAndIsDeletedFalse(student.getId()).size();

        long present = attendanceRecordRepository.findByStudentAndStatusAndDateRange(
                student.getId(), AttendanceRecord.AttendanceStatus.PRESENT,
                LocalDate.now().minusMonths(3), LocalDate.now()).size();
        long absent = attendanceRecordRepository.findByStudentAndStatusAndDateRange(
                student.getId(), AttendanceRecord.AttendanceStatus.ABSENT,
                LocalDate.now().minusMonths(3), LocalDate.now()).size();
        long late = attendanceRecordRepository.findByStudentAndStatusAndDateRange(
                student.getId(), AttendanceRecord.AttendanceStatus.LATE,
                LocalDate.now().minusMonths(3), LocalDate.now()).size();

        List<UUID> classGroupIds = getStudentClassGroupIds(student.getId());
        List<Assignment> assignments = new ArrayList<>();
        for (UUID cgId : classGroupIds) {
            assignments.addAll(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(cgId));
        }
        long pendingCount = assignments.stream().filter(a -> a.getDueDate() != null && a.getDueDate().isAfter(java.time.LocalDateTime.now())).count();
        long overdueCount = assignments.stream().filter(a -> a.getDueDate() != null && a.getDueDate().isBefore(java.time.LocalDateTime.now())).count();

        ReportCard latestReport = reportCardRepository.findByStudentIdAndIsDeletedFalse(student.getId()).stream()
                .sorted(Comparator.comparing(ReportCard::getPublishedAt).reversed())
                .findFirst().orElse(null);

        Double learningProgress = lessonProgressRepository.getAverageCompletionByStudent(student.getId());

        return ChildOverviewResponse.builder()
                .studentId(student.getId())
                .studentName(studentName)
                .admissionNumber(student.getAdmissionNumber())
                .relationshipType(link.getRelationshipType().name())
                .isPrimary(link.getIsPrimary())
                .className(className)
                .educationLevel(className)
                .totalDays(totalAttendance)
                .daysPresent(present)
                .daysAbsent(absent)
                .daysLate(late)
                .attendancePercentage(attendancePct)
                .totalAssignments((long) assignments.size())
                .completedAssignments((long) assignments.size() - overdueCount - pendingCount)
                .overdueAssignments(overdueCount)
                .pendingAssignments(pendingCount)
                .latestGrade(latestReport != null ? latestReport.getOverallGrade() : null)
                .latestAverage(latestReport != null && latestReport.getAverageMark() != null
                        ? latestReport.getAverageMark().doubleValue() : null)
                .classRank(latestReport != null ? latestReport.getClassRank() : null)
                .totalStudentsInClass(latestReport != null ? latestReport.getTotalStudentsInClass() : null)
                .learningProgress(learningProgress)
                .build();
    }

    private Double calculateAttendancePercentage(UUID studentId) {
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByStudentAndDateRange(studentId, threeMonthsAgo, LocalDate.now());
        if (records.isEmpty()) return null;
        long present = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT).count();
        return present * 100.0 / records.size();
    }

    private String getLatestGrade(UUID studentId) {
        return reportCardRepository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                .sorted(Comparator.comparing(ReportCard::getPublishedAt).reversed())
                .findFirst()
                .map(ReportCard::getOverallGrade)
                .orElse(null);
    }

    private List<Assignment> getOverdueAssignments(UUID studentId) {
        List<UUID> classGroupIds = getStudentClassGroupIds(studentId);
        List<Assignment> assignments = new ArrayList<>();
        for (UUID cgId : classGroupIds) {
            assignments.addAll(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(cgId));
        }
        return assignments.stream()
                .filter(a -> a.getDueDate() != null && a.getDueDate().isBefore(java.time.LocalDateTime.now()))
                .toList();
    }

    private List<UUID> getStudentClassGroupIds(UUID studentId) {
        return enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ENROLLED)
                .map(Enrollment::getClassGroupId)
                .distinct()
                .toList();
    }

    private String resolveClassName(UUID classGroupId) {
        return classGroupRepository.findById(classGroupId)
                .filter(cg -> !Boolean.TRUE.equals(cg.getIsDeleted()))
                .map(ClassGroup::getName)
                .orElse("N/A");
    }

    private String resolveTermName(UUID termId) {
        if (termId == null) return "N/A";
        return termRepository.findById(termId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .map(Term::getName)
                .orElse("N/A");
    }

    private String resolveAcademicYearName(UUID academicYearId) {
        if (academicYearId == null) return "N/A";
        return academicYearRepository.findById(academicYearId)
                .filter(ay -> !Boolean.TRUE.equals(ay.getIsDeleted()))
                .map(AcademicYear::getYearLabel)
                .orElse("N/A");
    }

    @Override
    public ParentAssessmentResponse getChildAssessments(UUID userId, UUID studentId) {
        if (!isAuthorizedChild(userId, studentId)) {
            throw new ResourceNotFoundException("Student link", "studentId", studentId);
        }
        Student student = getStudent(studentId);
        String studentName = getStudentName(student);
        List<UUID> classGroupIds = getStudentClassGroupIds(studentId);

        List<Assessment> allAssessments = new ArrayList<>();
        for (UUID cgId : classGroupIds) {
            allAssessments.addAll(assessmentRepository.findByClassGroupIdAndIsDeletedFalse(cgId));
        }

        LocalDateTime now = LocalDateTime.now();
        List<ParentAssessmentResponse.AssessmentItem> upcoming = new ArrayList<>();
        List<ParentAssessmentResponse.AssessmentItem> completed = new ArrayList<>();

        for (Assessment a : allAssessments) {
            AssessmentResult result = assessmentResultRepository
                    .findByAssessmentIdAndStudentIdAndIsDeletedFalse(a.getId(), studentId)
                    .orElse(null);

            ParentAssessmentResponse.AssessmentItem item = ParentAssessmentResponse.AssessmentItem.builder()
                    .id(a.getId().toString())
                    .title(a.getTitle())
                    .subject(resolveClassName(a.getClassGroupId()))
                    .startsAt(a.getStartsAt())
                    .endsAt(a.getEndsAt())
                    .totalMarks(a.getTotalMarks())
                    .passMarks(a.getPassMarks())
                    .isPublished(a.getIsPublished())
                    .score(result != null ? result.getTotalScore() : null)
                    .isPassed(result != null ? result.getIsPassed() : null)
                    .feedback(result != null ? result.getFeedback() : null)
                    .build();

            if (result != null) {
                completed.add(item);
            } else {
                upcoming.add(item);
            }
        }

        return ParentAssessmentResponse.builder()
                .studentName(studentName)
                .className(resolveStudentClassName(studentId))
                .upcoming(upcoming)
                .completed(completed)
                .build();
    }

    @Override
    public ParentLiveClassResponse getChildLiveClasses(UUID userId, UUID studentId) {
        if (!isAuthorizedChild(userId, studentId)) {
            throw new ResourceNotFoundException("Student link", "studentId", studentId);
        }
        Student student = getStudent(studentId);
        String studentName = getStudentName(student);
        List<UUID> classGroupIds = getStudentClassGroupIds(studentId);

        List<LiveClass> allClasses = liveClassRepository.findByInstitutionIdAndIsDeletedFalse(student.getInstitutionId()).stream()
                .filter(lc -> lc.getClassGroupId() != null && classGroupIds.contains(lc.getClassGroupId()))
                .toList();

        List<ParentLiveClassResponse.LiveClassItem> items = allClasses.stream()
                .sorted(Comparator.comparing(LiveClass::getScheduledAt).reversed())
                .map(lc -> {
                    User teacher = lc.getTeacherId() != null ? userRepository.findById(lc.getTeacherId()).orElse(null) : null;
                    return ParentLiveClassResponse.LiveClassItem.builder()
                            .id(lc.getId().toString())
                            .title(lc.getTitle())
                            .description(lc.getDescription())
                            .status(lc.getStatus())
                            .scheduledAt(lc.getScheduledAt())
                            .durationMinutes(lc.getDurationMinutes())
                            .teacherName(teacher != null ? teacher.getFullName() : "Unknown")
                            .build();
                })
                .toList();

        return ParentLiveClassResponse.builder()
                .studentName(studentName)
                .className(resolveStudentClassName(studentId))
                .liveClasses(items)
                .build();
    }

    @Override
    public ParentNotificationResponse getChildNotifications(UUID userId, UUID studentId) {
        if (!isAuthorizedChild(userId, studentId)) {
            throw new ResourceNotFoundException("Student link", "studentId", studentId);
        }
        Student student = getStudent(studentId);
        String studentName = getStudentName(student);

        List<ParentNotificationResponse.NotificationItem> notifications = new ArrayList<>();

        List<Assignment> overdueAssignments = getOverdueAssignments(studentId);
        for (Assignment a : overdueAssignments) {
            notifications.add(ParentNotificationResponse.NotificationItem.builder()
                    .id("assignment-" + a.getId())
                    .type("ASSIGNMENT")
                    .title("Assignment Overdue")
                    .detail(a.getTitle() + " is past due")
                    .childName(studentName)
                    .createdAt(a.getDueDate() != null ? a.getDueDate() : LocalDateTime.now())
                    .isRead(false)
                    .priority("HIGH")
                    .build());
        }

        Double attendancePct = calculateAttendancePercentage(studentId);
        if (attendancePct != null && attendancePct < 75) {
            notifications.add(ParentNotificationResponse.NotificationItem.builder()
                    .id("attendance-" + studentId)
                    .type("ATTENDANCE")
                    .title("Attendance Below 75%")
                    .detail(String.format("Current attendance: %.0f%%", attendancePct))
                    .childName(studentName)
                    .createdAt(LocalDateTime.now())
                    .isRead(false)
                    .priority("HIGH")
                    .build());
        }

        List<UUID> cachedCgIds = getStudentClassGroupIds(studentId);
        List<LiveClass> upcomingClasses = liveClassRepository
                .findByInstitutionIdAndStatusAndIsDeletedFalse(student.getInstitutionId(), "SCHEDULED")
                .stream()
                .filter(lc -> lc.getClassGroupId() != null && cachedCgIds.contains(lc.getClassGroupId()))
                .toList();

        for (LiveClass lc : upcomingClasses) {
            notifications.add(ParentNotificationResponse.NotificationItem.builder()
                    .id("liveclass-" + lc.getId())
                    .type("LIVE_CLASS")
                    .title("Upcoming Live Class")
                    .detail(lc.getTitle() + " scheduled for " + lc.getScheduledAt())
                    .childName(studentName)
                    .createdAt(lc.getScheduledAt())
                    .isRead(false)
                    .priority("NORMAL")
                    .build());
        }

        notifications.sort(Comparator.comparing(
                (ParentNotificationResponse.NotificationItem n) -> "HIGH".equals(n.getPriority()) ? 0 : 1)
                .thenComparing(Comparator.comparing(ParentNotificationResponse.NotificationItem::getCreatedAt).reversed()));

        return ParentNotificationResponse.builder()
                .notifications(notifications)
                .unreadCount(notifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count())
                .build();
    }

    private String resolveStudentClassName(UUID studentId) {
        List<UUID> cgIds = getStudentClassGroupIds(studentId);
        if (cgIds.isEmpty()) return "N/A";
        return resolveClassName(cgIds.get(0));
    }

    @Override
    public ParentLearningProgressResponse getChildLearningProgress(UUID userId, UUID studentId) {
        if (!isAuthorizedChild(userId, studentId)) {
            throw new ResourceNotFoundException("Student link", "studentId", studentId);
        }
        Student student = getStudent(studentId);
        String studentName = getStudentName(student);
        List<UUID> classGroupIds = getStudentClassGroupIds(studentId);

        List<Lesson> allLessons = new ArrayList<>();
        for (UUID cgId : classGroupIds) {
            allLessons.addAll(lessonRepository.findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(cgId));
        }

        List<LessonProgress> allProgress = lessonProgressRepository.findByStudentIdAndIsDeletedFalse(studentId);

        java.util.Map<UUID, List<Lesson>> lessonsBySubject = new java.util.LinkedHashMap<>();
        for (Lesson lesson : allLessons) {
            lessonsBySubject.computeIfAbsent(lesson.getSubjectId(), k -> new ArrayList<>()).add(lesson);
        }

        List<ParentLearningProgressResponse.CourseProgress> courses = new ArrayList<>();
        for (java.util.Map.Entry<UUID, List<Lesson>> entry : lessonsBySubject.entrySet()) {
            UUID subjectId = entry.getKey();
            List<Lesson> lessons = entry.getValue();

            Subject subject = subjectRepository.findById(subjectId).orElse(null);
            String subjectName = subject != null ? subject.getName() : "Unknown Subject";

            int totalLessons = lessons.size();
            int completedLessons = 0;
            double totalCompletion = 0;

            for (Lesson lesson : lessons) {
                LessonProgress progress = allProgress.stream()
                        .filter(p -> p.getLessonId().equals(lesson.getId()))
                        .findFirst().orElse(null);
                if (progress != null) {
                    totalCompletion += progress.getCompletionPercentage();
                    if (progress.getCompletionPercentage() >= 100) {
                        completedLessons++;
                    }
                }
            }

            double completionPct = totalLessons > 0 ? totalCompletion / totalLessons : 0;
            String status = completionPct >= 100 ? "COMPLETED" : completionPct > 0 ? "IN_PROGRESS" : "NOT_STARTED";

            courses.add(ParentLearningProgressResponse.CourseProgress.builder()
                    .subjectId(subjectId.toString())
                    .subjectName(subjectName)
                    .totalLessons(totalLessons)
                    .completedLessons(completedLessons)
                    .completionPercentage(Math.round(completionPct * 10.0) / 10.0)
                    .status(status)
                    .build());
        }

        double overallProgress = courses.stream()
                .mapToDouble(ParentLearningProgressResponse.CourseProgress::getCompletionPercentage)
                .average().orElse(0);

        return ParentLearningProgressResponse.builder()
                .studentName(studentName)
                .className(resolveStudentClassName(studentId))
                .overallProgress(Math.round(overallProgress * 10.0) / 10.0)
                .courses(courses)
                .build();
    }

    @Override
    public ParentAnnouncementResponse getChildAnnouncements(UUID userId, UUID studentId) {
        if (!isAuthorizedChild(userId, studentId)) {
            throw new ResourceNotFoundException("Student link", "studentId", studentId);
        }
        Student student = getStudent(studentId);
        List<UUID> classGroupIds = getStudentClassGroupIds(studentId);

        List<Announcement> announcements = new ArrayList<>();
        for (UUID cgId : classGroupIds) {
            announcements.addAll(announcementRepository
                    .findByInstitutionIdAndClassGroupIdAndIsDeletedFalse(student.getInstitutionId(), cgId));
        }
        announcements.addAll(announcementRepository
                .findInstitutionWideByInstitutionIdAndIsDeletedFalse(student.getInstitutionId()));

        List<ParentAnnouncementResponse.AnnouncementItem> items = announcements.stream()
                .distinct()
                .sorted(Comparator.comparing(Announcement::getCreatedAt).reversed())
                .limit(20)
                .map(a -> {
                    User author = userRepository.findById(a.getAuthorId()).orElse(null);
                    Subject subject = a.getSubjectId() != null ? subjectRepository.findById(a.getSubjectId()).orElse(null) : null;
                    ClassGroup cg = a.getClassGroupId() != null ? classGroupRepository.findById(a.getClassGroupId()).orElse(null) : null;

                    return ParentAnnouncementResponse.AnnouncementItem.builder()
                            .id(a.getId().toString())
                            .title(a.getTitle())
                            .content(a.getContent())
                            .priority(a.getPriority())
                            .authorName(author != null ? author.getFullName() : "Unknown")
                            .className(cg != null ? cg.getName() : null)
                            .subjectName(subject != null ? subject.getName() : null)
                            .createdAt(a.getCreatedAt())
                            .build();
                })
                .toList();

        return ParentAnnouncementResponse.builder()
                .announcements(items)
                .build();
    }

    @Override
    public ParentAllNotificationsResponse getAllChildrenNotifications(UUID userId) {
        Parent parent = resolveParentStrict(userId);
        List<ParentStudentLink> links = studentLinkRepository.findAllByParentId(parent.getId());
        List<ParentAllNotificationsResponse.NotificationItem> allNotifications = new ArrayList<>();

        for (ParentStudentLink link : links) {
            UUID studentId = link.getStudentId();
            Student student = getStudent(studentId);
            String studentName = getStudentName(student);

            List<Assignment> overdue = getOverdueAssignments(studentId);
            for (Assignment a : overdue) {
                allNotifications.add(ParentAllNotificationsResponse.NotificationItem.builder()
                        .id("assignment-" + a.getId())
                        .type("ASSIGNMENT")
                        .title("Assignment Overdue")
                        .detail(a.getTitle() + " is past due")
                        .childName(studentName)
                        .childId(studentId.toString())
                        .createdAt(a.getDueDate() != null ? a.getDueDate() : LocalDateTime.now())
                        .isRead(false)
                        .priority("HIGH")
                        .build());
            }

            Double attendancePct = calculateAttendancePercentage(studentId);
            if (attendancePct != null && attendancePct < 75) {
                allNotifications.add(ParentAllNotificationsResponse.NotificationItem.builder()
                        .id("attendance-" + studentId)
                        .type("ATTENDANCE")
                        .title("Attendance Below 75%")
                        .detail(String.format("%s attendance: %.0f%%", studentName, attendancePct))
                        .childName(studentName)
                        .childId(studentId.toString())
                        .createdAt(LocalDateTime.now())
                        .isRead(false)
                        .priority("HIGH")
                        .build());
            }

            List<UUID> cgIds = getStudentClassGroupIds(studentId);
            List<LiveClass> upcomingClasses = liveClassRepository
                    .findByInstitutionIdAndStatusAndIsDeletedFalse(student.getInstitutionId(), "SCHEDULED")
                    .stream()
                    .filter(lc -> lc.getClassGroupId() != null && cgIds.contains(lc.getClassGroupId()))
                    .toList();

            for (LiveClass lc : upcomingClasses) {
                allNotifications.add(ParentAllNotificationsResponse.NotificationItem.builder()
                        .id("liveclass-" + lc.getId())
                        .type("LIVE_CLASS")
                        .title("Upcoming Live Class")
                        .detail(lc.getTitle())
                        .childName(studentName)
                        .childId(studentId.toString())
                        .createdAt(lc.getScheduledAt())
                        .isRead(false)
                        .priority("NORMAL")
                        .build());
            }
        }

        allNotifications.sort(Comparator.comparing(
                (ParentAllNotificationsResponse.NotificationItem n) -> "HIGH".equals(n.getPriority()) ? 0 : 1)
                .thenComparing(Comparator.comparing(ParentAllNotificationsResponse.NotificationItem::getCreatedAt).reversed()));

        return ParentAllNotificationsResponse.builder()
                .notifications(allNotifications)
                .unreadCount((int) allNotifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count())
                .totalChildren(links.size())
                .build();
    }

    @Override
    public ParentUpcomingResponse getUpcomingActivities(UUID userId) {
        Parent parent = resolveParentStrict(userId);
        List<ParentStudentLink> links = studentLinkRepository.findAllByParentId(parent.getId());
        List<ParentUpcomingResponse.UpcomingItem> items = new ArrayList<>();

        for (ParentStudentLink link : links) {
            UUID studentId = link.getStudentId();
            Student student = getStudent(studentId);
            String studentName = getStudentName(student);
            List<UUID> cgIds = getStudentClassGroupIds(studentId);

            List<Assignment> allAssignments = new ArrayList<>();
            for (UUID cgId : cgIds) {
                allAssignments.addAll(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(cgId));
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoWeeksLater = now.plusWeeks(2);

            for (Assignment a : allAssignments) {
                if (a.getDueDate() != null && a.getDueDate().isAfter(now) && a.getDueDate().isBefore(twoWeeksLater)) {
                    items.add(ParentUpcomingResponse.UpcomingItem.builder()
                            .type("ASSIGNMENT")
                            .title(a.getTitle())
                            .childName(studentName)
                            .dateTime(a.getDueDate())
                            .detail("Assignment due")
                            .priority(a.getDueDate().isBefore(now.plusDays(3)) ? "HIGH" : "NORMAL")
                            .build());
                }
            }

            for (UUID cgId : cgIds) {
                List<Assessment> assessments = assessmentRepository.findByClassGroupIdAndIsDeletedFalse(cgId);
                for (Assessment a : assessments) {
                    if (a.getStartsAt() != null && a.getStartsAt().isAfter(now) && a.getStartsAt().isBefore(twoWeeksLater)) {
                        items.add(ParentUpcomingResponse.UpcomingItem.builder()
                                .type("ASSESSMENT")
                                .title(a.getTitle())
                                .childName(studentName)
                                .dateTime(a.getStartsAt())
                                .detail("Assessment starts")
                                .priority(a.getStartsAt().isBefore(now.plusDays(3)) ? "HIGH" : "NORMAL")
                                .build());
                    }
                }
            }

            List<LiveClass> liveClasses = liveClassRepository
                    .findByInstitutionIdAndStatusAndIsDeletedFalse(student.getInstitutionId(), "SCHEDULED")
                    .stream()
                    .filter(lc -> lc.getClassGroupId() != null && cgIds.contains(lc.getClassGroupId()))
                    .toList();

            for (LiveClass lc : liveClasses) {
                if (lc.getScheduledAt().isAfter(now) && lc.getScheduledAt().isBefore(twoWeeksLater)) {
                    items.add(ParentUpcomingResponse.UpcomingItem.builder()
                            .type("LIVE_CLASS")
                            .title(lc.getTitle())
                            .childName(studentName)
                            .dateTime(lc.getScheduledAt())
                            .detail("Live class")
                            .priority(lc.getScheduledAt().isBefore(now.plusDays(3)) ? "HIGH" : "NORMAL")
                            .build());
                }
            }
        }

        items.sort(Comparator.comparing(ParentUpcomingResponse.UpcomingItem::getDateTime));

        return ParentUpcomingResponse.builder()
                .items(items)
                .build();
    }
}
