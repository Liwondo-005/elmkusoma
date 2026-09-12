package tz.elmkusoma.teacher.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.attendance.domain.AttendanceRecord;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.domain.AssignmentSubmission;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherAssignment;
import tz.elmkusoma.teacher.dto.response.TeacherAnalyticsResponse;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/teachers/me/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Analytics", description = "Analytics data for teachers")
public class TeacherAnalyticsController {

    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final LessonRepository lessonRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ClassGroupRepository classGroupRepository;
    private final SubjectRepository subjectRepository;

    @GetMapping
    @Operation(summary = "Get teacher analytics")
    public ResponseEntity<ApiResponse<TeacherAnalyticsResponse>> getAnalytics(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {

        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));

        List<TeacherAssignment> teacherAssignments = teacherAssignmentRepository.findAllByTeacherId(teacher.getId());
        Set<UUID> classGroupIds = teacherAssignments.stream()
                .map(TeacherAssignment::getClassGroupId)
                .collect(Collectors.toSet());

        long totalStudents = 0;
        long totalAssignments = 0;
        long totalLessons = 0;
        long pendingGrading = 0;
        int totalAttendanceRecords = 0;
        int presentRecords = 0;
        List<TeacherAnalyticsResponse.UpcomingDeadline> upcomingDeadlines = new ArrayList<>();
        List<TeacherAnalyticsResponse.RecentSubmission> recentSubmissions = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (UUID classGroupId : classGroupIds) {
            List<Enrollment> enrollments = enrollmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId);
            long activeStudents = enrollments.stream()
                    .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ENROLLED).count();
            totalStudents += activeStudents;

            List<Assignment> classAssignments = assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId);
            totalAssignments += classAssignments.size();

            List<Lesson> classLessons = lessonRepository.findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(classGroupId);
            totalLessons += classLessons.size();

            for (Assignment assignment : classAssignments) {
                List<AssignmentSubmission> submissions = submissionRepository.findByAssignmentIdAndIsDeletedFalse(assignment.getId());
                long ungraded = submissions.stream()
                        .filter(s -> s.getGrade() == null).count();
                pendingGrading += ungraded;

                if (assignment.getDueDate() != null && assignment.getDueDate().isAfter(now)) {
                    String subjectName = null;
                    String className = null;
                    if (assignment.getSubjectId() != null) {
                        subjectName = subjectRepository.findById(assignment.getSubjectId())
                                .map(Subject::getName).orElse(null);
                    }
                    className = classGroupRepository.findById(classGroupId)
                            .map(ClassGroup::getName).orElse(null);

                    upcomingDeadlines.add(TeacherAnalyticsResponse.UpcomingDeadline.builder()
                            .title(assignment.getTitle())
                            .dueDate(assignment.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                            .subjectName(subjectName)
                            .className(className)
                            .build());
                }

                for (AssignmentSubmission submission : submissions.stream().sorted(
                        Comparator.comparing(AssignmentSubmission::getSubmittedAt).reversed()).limit(10).toList()) {
                    Student student = studentRepository.findById(submission.getStudentId()).orElse(null);
                    String studentName = "Unknown";
                    if (student != null) {
                        User studentUser = userRepository.findById(student.getUserId()).orElse(null);
                        if (studentUser != null) {
                            studentName = studentUser.getFullName();
                        }
                    }
                    recentSubmissions.add(TeacherAnalyticsResponse.RecentSubmission.builder()
                            .studentName(studentName)
                            .assignmentTitle(assignment.getTitle())
                            .submittedAt(submission.getSubmittedAt())
                            .graded(submission.getGrade() != null)
                            .build());
                }
            }

            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            List<AttendanceRecord> attendanceRecords = attendanceRepository.findByClassGroupAndDateRange(
                    classGroupId, thirtyDaysAgo, LocalDate.now());
            totalAttendanceRecords += attendanceRecords.size();
            presentRecords += attendanceRecords.stream()
                    .filter(ar -> ar.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT).count();
        }

        double averageAttendance = totalAttendanceRecords > 0
                ? (double) presentRecords / totalAttendanceRecords * 100.0
                : 0.0;

        upcomingDeadlines.sort(Comparator.comparing(TeacherAnalyticsResponse.UpcomingDeadline::getDueDate));
        recentSubmissions = recentSubmissions.stream().limit(20).toList();

        TeacherAnalyticsResponse response = TeacherAnalyticsResponse.builder()
                .totalStudents(totalStudents)
                .totalAssignments(totalAssignments)
                .totalLessons(totalLessons)
                .averageAttendance(Math.round(averageAttendance * 100.0) / 100.0)
                .pendingGrading(pendingGrading)
                .classesCount(classGroupIds.size())
                .upcomingDeadlines(upcomingDeadlines)
                .recentSubmissions(recentSubmissions)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
