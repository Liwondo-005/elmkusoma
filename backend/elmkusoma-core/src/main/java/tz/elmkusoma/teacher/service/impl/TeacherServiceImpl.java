package tz.elmkusoma.teacher.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.attendance.domain.AttendanceRecord;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherAssignment;
import tz.elmkusoma.teacher.domain.TeacherQualification;
import tz.elmkusoma.teacher.domain.TeacherStatus;
import tz.elmkusoma.teacher.dto.request.TeacherAssignmentRequest;
import tz.elmkusoma.teacher.dto.request.TeacherQualificationRequest;
import tz.elmkusoma.teacher.dto.request.TeacherRequest;
import tz.elmkusoma.teacher.dto.response.*;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherQualificationRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.teacher.service.TeacherService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final TeacherQualificationRepository qualificationRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepo;
    private final LessonRepository lessonRepository;
    private final AssessmentRepository assessmentRepository;
    private final AttendanceRecordRepository attendanceRepository;

    @Override
    public TeacherResponse createTeacher(UUID institutionId, TeacherRequest request) {
        UUID userId = UUID.fromString(request.getUserId());
        User user = userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (teacherRepository.existsByUserIdAndInstitutionIdAndIsDeletedFalse(userId, institutionId)) {
            throw new IllegalArgumentException("Teacher profile already exists for this user in this institution");
        }

        Teacher teacher = Teacher.builder()
                .userId(userId)
                .employeeNumber(request.getEmployeeNumber())
                .status(TeacherStatus.ACTIVE)
                .specialization(request.getSpecialization())
                .hireDate(request.getHireDate())
                .bio(request.getBio())
                .build();
        teacher.setInstitutionId(institutionId);

        Teacher saved = teacherRepository.save(teacher);
        return mapToResponse(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getTeacher(UUID institutionId, UUID teacherId) {
        Teacher teacher = teacherRepository.findByIdAndInstitutionId(teacherId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));
        User user = userRepository.findById(teacher.getUserId()).orElse(null);
        return mapToResponse(teacher, user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> listTeachers(UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Teacher> teachers = teacherRepository.findAllByInstitutionId(institutionId)
                .stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(
                                list.subList(Math.min(page * size, list.size()),
                                        Math.min((page + 1) * size, list.size())),
                                pageable, list.size())
                ));

        List<TeacherResponse> content = teachers.getContent().stream()
                .map(t -> {
                    User user = userRepository.findById(t.getUserId()).orElse(null);
                    return mapToResponse(t, user);
                })
                .toList();

        return new PageResponse<>(
                content,
                teachers.getNumber(),
                teachers.getSize(),
                teachers.getTotalElements(),
                teachers.getTotalPages(),
                teachers.isFirst(),
                teachers.isLast()
        );
    }

    @Override
    public TeacherResponse updateTeacher(UUID institutionId, UUID teacherId, TeacherRequest request) {
        Teacher teacher = teacherRepository.findByIdAndInstitutionId(teacherId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));

        if (request.getEmployeeNumber() != null) teacher.setEmployeeNumber(request.getEmployeeNumber());
        if (request.getSpecialization() != null) teacher.setSpecialization(request.getSpecialization());
        if (request.getHireDate() != null) teacher.setHireDate(request.getHireDate());
        if (request.getBio() != null) teacher.setBio(request.getBio());

        Teacher saved = teacherRepository.save(teacher);
        User user = userRepository.findById(saved.getUserId()).orElse(null);
        return mapToResponse(saved, user);
    }

    @Override
    public void deleteTeacher(UUID institutionId, UUID teacherId) {
        Teacher teacher = teacherRepository.findByIdAndInstitutionId(teacherId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));
        teacher.setIsDeleted(true);
        teacherRepository.save(teacher);
    }

    @Override
    public TeacherAssignmentResponse addAssignment(UUID institutionId, UUID teacherId, TeacherAssignmentRequest request) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }

        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacherId(teacherId)
                .classGroupId(UUID.fromString(request.getClassGroupId()))
                .subjectId(UUID.fromString(request.getSubjectId()))
                .academicYear(request.getAcademicYear())
                .build();
        assignment.setInstitutionId(institutionId);

        TeacherAssignment saved = assignmentRepository.save(assignment);
        return mapToAssignmentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> getAssignments(UUID institutionId, UUID teacherId) {
        return assignmentRepository.findAllByTeacherId(teacherId).stream()
                .map(this::mapToAssignmentResponse)
                .toList();
    }

    @Override
    public void removeAssignment(UUID institutionId, UUID assignmentId) {
        TeacherAssignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> a.getInstitutionId().equals(institutionId))
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        assignment.setIsDeleted(true);
        assignmentRepository.save(assignment);
    }

    @Override
    public TeacherQualificationResponse addQualification(UUID institutionId, UUID teacherId, TeacherQualificationRequest request) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }

        TeacherQualification qualification = TeacherQualification.builder()
                .teacherId(teacherId)
                .qualificationName(request.getQualificationName())
                .institutionName(request.getInstitutionName())
                .fieldOfStudy(request.getFieldOfStudy())
                .yearObtained(request.getYearObtained())
                .certificateUrl(request.getCertificateUrl())
                .build();
        qualification.setInstitutionId(institutionId);

        TeacherQualification saved = qualificationRepository.save(qualification);
        return mapToQualificationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherQualificationResponse> getQualifications(UUID institutionId, UUID teacherId) {
        return qualificationRepository.findAllByTeacherId(teacherId).stream()
                .map(this::mapToQualificationResponse)
                .toList();
    }

    @Override
    public void removeQualification(UUID institutionId, UUID qualificationId) {
        TeacherQualification qualification = qualificationRepository.findById(qualificationId)
                .filter(q -> q.getInstitutionId().equals(institutionId))
                .orElseThrow(() -> new ResourceNotFoundException("Qualification", "id", qualificationId));
        qualification.setIsDeleted(true);
        qualificationRepository.save(qualification);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getTeacherByUserId(UUID userId, UUID institutionId) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));
        User user = userRepository.findById(teacher.getUserId()).orElse(null);
        return mapToResponse(teacher, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassResponse> getTeacherClasses(UUID userId, UUID institutionId) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));

        List<TeacherAssignment> assignments = assignmentRepository.findAllByTeacherId(teacher.getId());

        return assignments.stream().map(assignment -> {
            long enrolledCount = enrollmentRepository.findByClassGroupIdAndIsDeletedFalse(assignment.getClassGroupId())
                    .stream().filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ENROLLED).count();
            long assignmentCount = assignmentRepo.findByClassGroupIdAndIsDeletedFalse(assignment.getClassGroupId()).size();
            long lessonCount = lessonRepository.findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(assignment.getClassGroupId()).size();

            return TeacherClassResponse.builder()
                    .classGroupId(assignment.getClassGroupId())
                    .className("Class Group")
                    .subjectId(assignment.getSubjectId())
                    .subjectName("Subject")
                    .academicYear(assignment.getAcademicYear())
                    .enrolledStudents(enrolledCount)
                    .totalAssignments(assignmentCount)
                    .totalLessons(lessonCount)
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherStudentResponse> getTeacherStudents(UUID userId, UUID institutionId) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));

        List<TeacherAssignment> assignments = assignmentRepository.findAllByTeacherId(teacher.getId());
        Set<UUID> classGroupIds = assignments.stream()
                .map(TeacherAssignment::getClassGroupId)
                .collect(Collectors.toSet());

        List<TeacherStudentResponse> students = new ArrayList<>();
        for (UUID classGroupId : classGroupIds) {
            List<Enrollment> enrollments = enrollmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId);
            for (Enrollment enrollment : enrollments) {
                if (enrollment.getStatus() == Enrollment.EnrollmentStatus.ENROLLED) {
                    Student student = studentRepository.findById(enrollment.getStudentId()).orElse(null);
                    if (student != null) {
                        User studentUser = userRepository.findById(student.getUserId()).orElse(null);
                        TeacherAssignment matchedAssignment = assignments.stream()
                                .filter(a -> a.getClassGroupId().equals(classGroupId))
                                .findFirst().orElse(null);

                        students.add(TeacherStudentResponse.builder()
                                .studentId(student.getId())
                                .fullName(studentUser != null ? studentUser.getFullName() : "Unknown")
                                .email(studentUser != null ? studentUser.getEmail() : "")
                                .admissionNumber(student.getAdmissionNumber())
                                .className("Class Group")
                                .subjectName(matchedAssignment != null ? "Subject" : "")
                                .gender(student.getGender())
                                .status(student.getStatus().name())
                                .classGroupId(classGroupId)
                                .build());
                    }
                }
            }
        }
        return students;
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherDashboardResponse getTeacherDashboard(UUID userId, UUID institutionId) {
        Teacher teacher = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile", "userId", userId));

        List<TeacherAssignment> assignments = assignmentRepository.findAllByTeacherId(teacher.getId());
        Set<UUID> classGroupIds = assignments.stream()
                .map(TeacherAssignment::getClassGroupId)
                .collect(Collectors.toSet());

        long totalStudents = 0;
        long totalAssignments = 0;
        long totalAssessments = 0;
        long pendingSubmissions = 0;
        List<TeacherDashboardResponse.TeacherClassSummary> classSummaries = new ArrayList<>();
        List<TeacherDashboardResponse.RecentActivity> activities = new ArrayList<>();

        for (UUID classGroupId : classGroupIds) {
            List<Enrollment> enrollments = enrollmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId);
            long activeStudents = enrollments.stream()
                    .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ENROLLED).count();
            totalStudents += activeStudents;

            List<Assignment> classAssignments = assignmentRepo.findByClassGroupIdAndIsDeletedFalse(classGroupId);
            totalAssignments += classAssignments.size();

            List<Assessment> classAssessments = assessmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId);
            totalAssessments += classAssessments.size();

            for (Assignment a : classAssignments) {
                if (a.getDueDate() != null && a.getDueDate().isAfter(java.time.LocalDateTime.now())) {
                    pendingSubmissions++;
                }
            }

            TeacherAssignment matchedAssignment = assignments.stream()
                    .filter(a -> a.getClassGroupId().equals(classGroupId))
                    .findFirst().orElse(null);

            classSummaries.add(TeacherDashboardResponse.TeacherClassSummary.builder()
                    .classGroupId(classGroupId.toString())
                    .className("Class Group")
                    .subjectName(matchedAssignment != null ? "Subject" : "")
                    .enrolledStudents(activeStudents)
                    .build());

            for (Assignment a : classAssignments.stream().limit(3).toList()) {
                activities.add(TeacherDashboardResponse.RecentActivity.builder()
                        .type("assignment")
                        .title(a.getTitle())
                        .description("Due: " + (a.getDueDate() != null ? a.getDueDate().toLocalDate() : "No date"))
                        .timestamp(a.getCreatedAt())
                        .build());
            }
        }

        return TeacherDashboardResponse.builder()
                .totalStudents(totalStudents)
                .totalClasses(classGroupIds.size())
                .totalAssignments(totalAssignments)
                .totalAssessments(totalAssessments)
                .pendingSubmissions(pendingSubmissions)
                .pendingGrading(0)
                .classes(classSummaries)
                .recentActivity(activities.stream().limit(10).toList())
                .build();
    }

    private TeacherResponse mapToResponse(Teacher teacher, User user) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .userId(teacher.getUserId())
                .fullName(user != null ? user.getFullName() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .employeeNumber(teacher.getEmployeeNumber())
                .status(teacher.getStatus().name())
                .specialization(teacher.getSpecialization())
                .hireDate(teacher.getHireDate())
                .bio(teacher.getBio())
                .createdAt(teacher.getCreatedAt())
                .build();
    }

    private TeacherAssignmentResponse mapToAssignmentResponse(TeacherAssignment assignment) {
        return TeacherAssignmentResponse.builder()
                .id(assignment.getId())
                .teacherId(assignment.getTeacherId())
                .classGroupId(assignment.getClassGroupId())
                .subjectId(assignment.getSubjectId())
                .academicYear(assignment.getAcademicYear())
                .createdAt(assignment.getCreatedAt())
                .build();
    }

    private TeacherQualificationResponse mapToQualificationResponse(TeacherQualification qualification) {
        return TeacherQualificationResponse.builder()
                .id(qualification.getId())
                .teacherId(qualification.getTeacherId())
                .qualificationName(qualification.getQualificationName())
                .institutionName(qualification.getInstitutionName())
                .fieldOfStudy(qualification.getFieldOfStudy())
                .yearObtained(qualification.getYearObtained())
                .certificateUrl(qualification.getCertificateUrl())
                .createdAt(qualification.getCreatedAt())
                .build();
    }
}
