package tz.elmkusoma.parent.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.attendance.domain.AttendanceRecord;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.attendance.repository.AttendanceSummaryRepository;
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

            children.add(FamilyOverviewResponse.ChildSummary.builder()
                .studentId(student.getId().toString())
                    .name(studentName)
                    .admissionNumber(student.getAdmissionNumber())
                    .className("N/A")
                    .educationLevel("N/A")
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
                .className("N/A")
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

        List<Assignment> allAssignments = assignmentRepository.findByInstitutionIdAndIsDeletedFalse(student.getInstitutionId());
        List<ParentAssignmentResponse.AssignmentItem> pending = new ArrayList<>();
        List<ParentAssignmentResponse.AssignmentItem> completed = new ArrayList<>();
        List<ParentAssignmentResponse.AssignmentItem> overdue = new ArrayList<>();

        for (Assignment a : allAssignments) {
            ParentAssignmentResponse.AssignmentItem item = ParentAssignmentResponse.AssignmentItem.builder()
                    .id(a.getId().toString())
                    .title(a.getTitle())
                    .subject("N/A")
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
                .className("N/A")
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
        List<ParentResultResponse.ReportCardItem> items = reportCards.stream()
                .sorted(Comparator.comparing(ReportCard::getPublishedAt).reversed())
                .map(rc -> ParentResultResponse.ReportCardItem.builder()
                        .id(rc.getId().toString())
                        .term("N/A")
                        .academicYear("N/A")
                        .overallGrade(rc.getOverallGrade())
                        .averageMark(rc.getAverageMark() != null ? rc.getAverageMark().doubleValue() : null)
                        .gpa(rc.getGpa() != null ? rc.getGpa().doubleValue() : null)
                        .classRank(rc.getClassRank())
                        .totalStudentsInClass(rc.getTotalStudentsInClass())
                        .remarks(rc.getRemarks())
                        .status(rc.getStatus().name())
                        .publishedAt(rc.getPublishedAt())
                        .build())
                .toList();

        return ParentResultResponse.builder()
                .studentName(studentName)
                .className("N/A")
                .reportCards(items)
                .build();
    }

    private ChildOverviewResponse buildChildOverview(Student student, ParentStudentLink link) {
        String studentName = getStudentName(student);

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

        List<Assignment> assignments = assignmentRepository.findByInstitutionIdAndIsDeletedFalse(student.getInstitutionId());
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
                .className("N/A")
                .educationLevel("N/A")
                .totalDays(totalAttendance)
                .daysPresent(present)
                .daysAbsent(absent)
                .daysLate(late)
                .attendancePercentage(attendancePct)
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
        Student student = getStudent(studentId);
        return assignmentRepository.findByInstitutionIdAndIsDeletedFalse(student.getInstitutionId()).stream()
                .filter(a -> a.getDueDate() != null && a.getDueDate().isBefore(java.time.LocalDateTime.now()))
                .toList();
    }
}
