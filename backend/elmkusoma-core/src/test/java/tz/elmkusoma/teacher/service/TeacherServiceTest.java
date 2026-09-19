package tz.elmkusoma.teacher.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherAssignment;
import tz.elmkusoma.teacher.domain.TeacherQualification;
import tz.elmkusoma.teacher.domain.TeacherStatus;
import tz.elmkusoma.teacher.dto.request.TeacherAssignmentRequest;
import tz.elmkusoma.teacher.dto.request.TeacherQualificationRequest;
import tz.elmkusoma.teacher.dto.request.TeacherRequest;
import tz.elmkusoma.teacher.dto.response.TeacherAssignmentResponse;
import tz.elmkusoma.teacher.dto.response.TeacherQualificationResponse;
import tz.elmkusoma.teacher.dto.response.TeacherResponse;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherQualificationRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.teacher.service.impl.TeacherServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private TeacherAssignmentRepository assignmentRepository;
    @Mock
    private TeacherQualificationRepository qualificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AssignmentRepository assignmentRepo;
    @Mock
    private AssignmentSubmissionRepository submissionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private AttendanceRecordRepository attendanceRepository;
    @Mock
    private ClassGroupRepository classGroupRepository;
    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private TeacherServiceImpl teacherService;

    private UUID institutionId;
    private UUID userId;
    private UUID teacherId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
    }

    @Test
    void createTeacher_shouldSaveTeacherAndReturnResponse() {
        TeacherRequest request = new TeacherRequest();
        request.setUserId(userId.toString());
        request.setEmployeeNumber("EMP001");
        request.setSpecialization("Mathematics");
        request.setHireDate(LocalDate.of(2025, 1, 1));
        request.setBio("Experienced teacher");

        User user = User.builder()
                .id(userId)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .isActive(true)
                .isDeleted(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(teacherRepository.existsByUserIdAndInstitutionIdAndIsDeletedFalse(userId, institutionId)).thenReturn(false);

        Teacher savedTeacher = Teacher.builder()
                .userId(userId)
                .employeeNumber("EMP001")
                .status(TeacherStatus.ACTIVE)
                .specialization("Mathematics")
                .hireDate(LocalDate.of(2025, 1, 1))
                .bio("Experienced teacher")
                .build();
        savedTeacher.setId(teacherId);
        savedTeacher.setInstitutionId(institutionId);

        when(teacherRepository.save(any(Teacher.class))).thenReturn(savedTeacher);

        TeacherResponse response = teacherService.createTeacher(institutionId, request);

        assertNotNull(response);
        assertEquals("EMP001", response.getEmployeeNumber());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("Jane Smith", response.getFullName());
        assertEquals(institutionId, response.getId() != null ? institutionId : null);
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    void createTeacher_whenProfileExists_shouldThrow() {
        TeacherRequest request = new TeacherRequest();
        request.setUserId(userId.toString());

        User user = User.builder()
                .id(userId)
                .firstName("Jane")
                .lastName("Smith")
                .isActive(true)
                .isDeleted(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(teacherRepository.existsByUserIdAndInstitutionIdAndIsDeletedFalse(userId, institutionId)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> teacherService.createTeacher(institutionId, request));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void createTeacher_whenUserNotFound_shouldThrow() {
        TeacherRequest request = new TeacherRequest();
        request.setUserId(userId.toString());

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teacherService.createTeacher(institutionId, request));
    }

    @Test
    void getTeacher_shouldReturnScopedTeacher() {
        Teacher teacher = Teacher.builder()
                .userId(userId)
                .employeeNumber("EMP001")
                .status(TeacherStatus.ACTIVE)
                .specialization("Mathematics")
                .build();
        teacher.setId(teacherId);
        teacher.setInstitutionId(institutionId);

        User user = User.builder()
                .id(userId)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .build();

        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(teacher));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        TeacherResponse response = teacherService.getTeacher(institutionId, teacherId);

        assertNotNull(response);
        assertEquals("EMP001", response.getEmployeeNumber());
        assertEquals("Jane Smith", response.getFullName());
    }

    @Test
    void getTeacher_whenNotFound_shouldThrow() {
        UUID nonExistentId = UUID.randomUUID();
        when(teacherRepository.findByIdAndInstitutionId(nonExistentId, institutionId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teacherService.getTeacher(institutionId, nonExistentId));
    }

    @Test
    void deleteTeacher_shouldSoftDelete() {
        Teacher teacher = Teacher.builder()
                .userId(userId)
                .employeeNumber("EMP001")
                .status(TeacherStatus.ACTIVE)
                .build();
        teacher.setId(teacherId);
        teacher.setInstitutionId(institutionId);
        teacher.setIsDeleted(false);

        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(teacher);

        teacherService.deleteTeacher(institutionId, teacherId);

        assertTrue(teacher.getIsDeleted());
        verify(teacherRepository).save(teacher);
    }

    @Test
    void addAssignment_shouldSaveAssignmentToTeacher() {
        Teacher teacher = Teacher.builder()
                .userId(userId)
                .employeeNumber("EMP001")
                .status(TeacherStatus.ACTIVE)
                .specialization("Mathematics")
                .build();
        teacher.setId(teacherId);
        teacher.setInstitutionId(institutionId);

        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(teacher));

        UUID classGroupId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        TeacherAssignment savedAssignment = TeacherAssignment.builder()
                .teacherId(teacherId)
                .classGroupId(classGroupId)
                .subjectId(subjectId)
                .academicYear("2025/2026")
                .build();
        savedAssignment.setId(UUID.randomUUID());
        savedAssignment.setInstitutionId(institutionId);

        when(assignmentRepository.save(any(TeacherAssignment.class))).thenReturn(savedAssignment);

        TeacherAssignmentRequest request = new TeacherAssignmentRequest();
        request.setClassGroupId(classGroupId.toString());
        request.setSubjectId(subjectId.toString());
        request.setAcademicYear("2025/2026");

        TeacherAssignmentResponse response = teacherService.addAssignment(institutionId, teacherId, request);

        assertNotNull(response);
        assertEquals(teacherId, response.getTeacherId());
        assertEquals(classGroupId, response.getClassGroupId());
        assertEquals(subjectId, response.getSubjectId());
        verify(assignmentRepository).save(any(TeacherAssignment.class));
    }

    @Test
    void addAssignment_whenTeacherNotFound_shouldThrow() {
        UUID nonExistentTeacherId = UUID.randomUUID();
        when(teacherRepository.findByIdAndInstitutionId(nonExistentTeacherId, institutionId))
                .thenReturn(Optional.empty());

        TeacherAssignmentRequest request = new TeacherAssignmentRequest();
        request.setClassGroupId(UUID.randomUUID().toString());
        request.setSubjectId(UUID.randomUUID().toString());

        assertThrows(ResourceNotFoundException.class,
                () -> teacherService.addAssignment(institutionId, nonExistentTeacherId, request));
    }

    @Test
    void addQualification_shouldSaveQualificationToTeacher() {
        when(teacherRepository.existsById(teacherId)).thenReturn(true);

        TeacherQualification savedQualification = TeacherQualification.builder()
                .teacherId(teacherId)
                .qualificationName("Bachelor of Education")
                .institutionName("University of Dar es Salaam")
                .fieldOfStudy("Mathematics")
                .yearObtained(2020)
                .build();
        savedQualification.setId(UUID.randomUUID());
        savedQualification.setInstitutionId(institutionId);

        when(qualificationRepository.save(any(TeacherQualification.class))).thenReturn(savedQualification);

        TeacherQualificationRequest request = new TeacherQualificationRequest();
        request.setQualificationName("Bachelor of Education");
        request.setInstitutionName("University of Dar es Salaam");
        request.setFieldOfStudy("Mathematics");
        request.setYearObtained(2020);

        TeacherQualificationResponse response = teacherService.addQualification(institutionId, teacherId, request);

        assertNotNull(response);
        assertEquals(teacherId, response.getTeacherId());
        assertEquals("Bachelor of Education", response.getQualificationName());
        verify(qualificationRepository).save(any(TeacherQualification.class));
    }

    @Test
    void getAssignments_shouldReturnTeacherAssignments() {
        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacherId(teacherId)
                .classGroupId(UUID.randomUUID())
                .subjectId(UUID.randomUUID())
                .academicYear("2025/2026")
                .build();
        assignment.setId(UUID.randomUUID());

        when(assignmentRepository.findAllByTeacherId(teacherId)).thenReturn(List.of(assignment));

        List<TeacherAssignmentResponse> responses = teacherService.getAssignments(institutionId, teacherId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(teacherId, responses.get(0).getTeacherId());
    }

    @Test
    void updateTeacher_shouldUpdateFieldsAndSave() {
        Teacher teacher = Teacher.builder()
                .userId(userId)
                .employeeNumber("EMP001")
                .status(TeacherStatus.ACTIVE)
                .specialization("Mathematics")
                .bio("Old bio")
                .build();
        teacher.setId(teacherId);
        teacher.setInstitutionId(institutionId);

        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(teacher);
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().id(userId).firstName("Jane").lastName("Smith").build()
        ));

        TeacherRequest request = new TeacherRequest();
        request.setBio("Updated bio");
        request.setSpecialization("Physics");

        TeacherResponse response = teacherService.updateTeacher(institutionId, teacherId, request);

        assertNotNull(response);
        assertEquals("Physics", response.getSpecialization());
        verify(teacherRepository).save(any(Teacher.class));
    }
}
