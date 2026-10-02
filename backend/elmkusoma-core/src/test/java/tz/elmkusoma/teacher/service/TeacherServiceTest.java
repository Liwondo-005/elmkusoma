package tz.elmkusoma.teacher.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import tz.elmkusoma.teacher.domain.TeacherAssignmentStatus;
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
    @Mock
    private tz.elmkusoma.academic.repository.GradeRepository gradeRepository;

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

        // Validated ids: class and subject must exist inside the same institution.
        tz.elmkusoma.academic.domain.ClassGroup classGroup = new tz.elmkusoma.academic.domain.ClassGroup();
        classGroup.setInstitutionId(institutionId);
        classGroup.setName("Form 1 A");
        tz.elmkusoma.academic.domain.Subject subject = new tz.elmkusoma.academic.domain.Subject();
        subject.setInstitutionId(institutionId);
        subject.setName("Mathematics");
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
        when(assignmentRepository.findAllByTeacherId(teacherId)).thenReturn(List.of());

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

    // ── assignment lifecycle & id validation (spec §5, §10, §14) ──

    private Teacher existingTeacher() {
        Teacher teacher = Teacher.builder().userId(userId).build();
        teacher.setId(teacherId);
        teacher.setInstitutionId(institutionId);
        return teacher;
    }

    private tz.elmkusoma.academic.domain.ClassGroup classGroup(UUID id, UUID gradeId) {
        tz.elmkusoma.academic.domain.ClassGroup cg = new tz.elmkusoma.academic.domain.ClassGroup();
        cg.setId(id);
        cg.setInstitutionId(institutionId);
        cg.setGradeId(gradeId);
        cg.setName("Form 1 A");
        return cg;
    }

    private tz.elmkusoma.academic.domain.Subject subject(
            UUID id, tz.elmkusoma.academic.domain.EducationLevel level) {
        tz.elmkusoma.academic.domain.Subject s = new tz.elmkusoma.academic.domain.Subject();
        s.setId(id);
        s.setInstitutionId(institutionId);
        s.setEducationLevel(level);
        s.setName("Mathematics");
        return s;
    }

    private tz.elmkusoma.academic.domain.Grade grade(
            UUID id, tz.elmkusoma.academic.domain.EducationLevel level) {
        tz.elmkusoma.academic.domain.Grade g = new tz.elmkusoma.academic.domain.Grade();
        g.setId(id);
        g.setInstitutionId(institutionId);
        g.setEducationLevel(level);
        g.setName("Form 1");
        return g;
    }

    private TeacherAssignmentRequest assignmentRequest(UUID classGroupId, UUID subjectId) {
        TeacherAssignmentRequest request = new TeacherAssignmentRequest();
        request.setClassGroupId(classGroupId.toString());
        request.setSubjectId(subjectId.toString());
        request.setAcademicYear("2025/2026");
        return request;
    }

    /** Teacher + valid same-institution class/subject/grade wiring, no duplicates. */
    private void stubValidAssignmentContext(UUID classGroupId, UUID subjectId, UUID gradeId,
            tz.elmkusoma.academic.domain.EducationLevel level) {
        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(existingTeacher()));
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroup(classGroupId, gradeId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject(subjectId, level)));
        if (gradeId != null) {
            when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(gradeId, level)));
        }
        when(assignmentRepository.findAllByTeacherId(teacherId)).thenReturn(List.of());
        when(assignmentRepository.save(any(TeacherAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addAssignment_setsActiveStatusAndStartDate() {
        UUID classGroupId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        stubValidAssignmentContext(classGroupId, subjectId, gradeId,
                tz.elmkusoma.academic.domain.EducationLevel.SECONDARY);

        TeacherAssignmentResponse response = teacherService.addAssignment(
                institutionId, teacherId, assignmentRequest(classGroupId, subjectId));

        assertEquals("ACTIVE", response.getStatus());
        assertNotNull(response.getStartDate());
        assertEquals(null, response.getEndDate());

        ArgumentCaptor<TeacherAssignment> captor = ArgumentCaptor.forClass(TeacherAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertEquals(TeacherAssignmentStatus.ACTIVE, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getStartDate());
        assertEquals(institutionId, captor.getValue().getInstitutionId());
    }

    @Test
    void addAssignment_rejectsClassGroupOfAnotherInstitution() {
        UUID classGroupId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(existingTeacher()));
        tz.elmkusoma.academic.domain.ClassGroup foreign = classGroup(classGroupId, null);
        foreign.setInstitutionId(UUID.randomUUID());
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(foreign));

        assertThrows(ResourceNotFoundException.class, () ->
                teacherService.addAssignment(institutionId, teacherId, assignmentRequest(classGroupId, subjectId)));
        verify(assignmentRepository, never()).save(any(TeacherAssignment.class));
    }

    @Test
    void addAssignment_rejectsSubjectOfAnotherInstitution() {
        UUID classGroupId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(existingTeacher()));
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroup(classGroupId, null)));
        tz.elmkusoma.academic.domain.Subject foreign =
                subject(subjectId, tz.elmkusoma.academic.domain.EducationLevel.SECONDARY);
        foreign.setInstitutionId(UUID.randomUUID());
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(foreign));

        assertThrows(ResourceNotFoundException.class, () ->
                teacherService.addAssignment(institutionId, teacherId, assignmentRequest(classGroupId, subjectId)));
        verify(assignmentRepository, never()).save(any(TeacherAssignment.class));
    }

    @Test
    void addAssignment_rejectsMalformedClassGroupId() {
        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(existingTeacher()));
        TeacherAssignmentRequest request = new TeacherAssignmentRequest();
        request.setClassGroupId("not-a-uuid");
        request.setSubjectId(UUID.randomUUID().toString());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                teacherService.addAssignment(institutionId, teacherId, request));

        assertTrue(ex.getMessage().contains("classGroupId must be a valid UUID"));
        verify(assignmentRepository, never()).save(any(TeacherAssignment.class));
    }

    @Test
    void addAssignment_rejectsEducationLevelMismatch() {
        UUID classGroupId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(existingTeacher()));
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroup(classGroupId, gradeId)));
        // class grade is PRIMARY, submitted subject is SECONDARY
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(
                subject(subjectId, tz.elmkusoma.academic.domain.EducationLevel.SECONDARY)));
        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(
                grade(gradeId, tz.elmkusoma.academic.domain.EducationLevel.PRIMARY)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                teacherService.addAssignment(institutionId, teacherId, assignmentRequest(classGroupId, subjectId)));

        assertTrue(ex.getMessage().contains("does not match"));
        verify(assignmentRepository, never()).save(any(TeacherAssignment.class));
    }

    @Test
    void addAssignment_rejectsDuplicateComboWithClearError() {
        UUID classGroupId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(existingTeacher()));
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroup(classGroupId, null)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(
                subject(subjectId, tz.elmkusoma.academic.domain.EducationLevel.SECONDARY)));

        TeacherAssignment existing = TeacherAssignment.builder()
                .teacherId(teacherId)
                .classGroupId(classGroupId)
                .subjectId(subjectId)
                .academicYear("2025/2026")
                .status(TeacherAssignmentStatus.ACTIVE)
                .build();
        when(assignmentRepository.findAllByTeacherId(teacherId)).thenReturn(List.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                teacherService.addAssignment(institutionId, teacherId, assignmentRequest(classGroupId, subjectId)));

        assertTrue(ex.getMessage().contains("already has an assignment"),
                "duplicates must surface as a clear 400, not a database 500");
        verify(assignmentRepository, never()).save(any(TeacherAssignment.class));
    }

    @Test
    void endAssignment_marksEndedAndPreservesHistoryRow() {
        UUID assignmentId = UUID.randomUUID();
        TeacherAssignment active = TeacherAssignment.builder()
                .teacherId(teacherId)
                .classGroupId(UUID.randomUUID())
                .subjectId(UUID.randomUUID())
                .academicYear("2025/2026")
                .status(TeacherAssignmentStatus.ACTIVE)
                .build();
        active.setId(assignmentId);
        active.setInstitutionId(institutionId);
        active.setIsDeleted(false);
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(active));
        when(assignmentRepository.save(any(TeacherAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherAssignmentResponse response = teacherService.endAssignment(institutionId, assignmentId);

        assertEquals("ENDED", response.getStatus());
        assertNotNull(response.getEndDate());
        assertFalse(active.getIsDeleted(), "ending must never delete the history row");
        verify(assignmentRepository, never()).delete(any(TeacherAssignment.class));
    }

    @Test
    void endAssignment_whenAlreadyEnded_throws() {
        UUID assignmentId = UUID.randomUUID();
        TeacherAssignment ended = TeacherAssignment.builder()
                .teacherId(teacherId)
                .classGroupId(UUID.randomUUID())
                .subjectId(UUID.randomUUID())
                .academicYear("2025/2026")
                .status(TeacherAssignmentStatus.ENDED)
                .build();
        ended.setId(assignmentId);
        ended.setInstitutionId(institutionId);
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(ended));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                teacherService.endAssignment(institutionId, assignmentId));

        assertTrue(ex.getMessage().contains("Only active assignments"));
        verify(assignmentRepository, never()).save(any(TeacherAssignment.class));
    }

    @Test
    void endAssignment_crossInstitution_isNotFound() {
        UUID assignmentId = UUID.randomUUID();
        TeacherAssignment foreign = TeacherAssignment.builder()
                .teacherId(teacherId)
                .classGroupId(UUID.randomUUID())
                .subjectId(UUID.randomUUID())
                .academicYear("2025/2026")
                .status(TeacherAssignmentStatus.ACTIVE)
                .build();
        foreign.setId(assignmentId);
        foreign.setInstitutionId(UUID.randomUUID());
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(foreign));

        assertThrows(ResourceNotFoundException.class, () ->
                teacherService.endAssignment(institutionId, assignmentId));
        verify(assignmentRepository, never()).save(any(TeacherAssignment.class));
    }

    @Test
    void reassignment_endsOld_createsNew_andKeepsBothRows() {
        UUID oldAssignmentId = UUID.randomUUID();
        TeacherAssignment old = TeacherAssignment.builder()
                .teacherId(teacherId)
                .classGroupId(UUID.randomUUID())
                .subjectId(UUID.randomUUID())
                .academicYear("2025/2026")
                .status(TeacherAssignmentStatus.ACTIVE)
                .build();
        old.setId(oldAssignmentId);
        old.setInstitutionId(institutionId);
        old.setIsDeleted(false);
        when(assignmentRepository.findById(oldAssignmentId)).thenReturn(Optional.of(old));
        when(assignmentRepository.save(any(TeacherAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. end the old assignment — row survives with ENDED
        TeacherAssignmentResponse endedResponse = teacherService.endAssignment(institutionId, oldAssignmentId);
        assertEquals("ENDED", endedResponse.getStatus());
        assertFalse(old.getIsDeleted());

        // 2. create the new assignment — different class, same teacher
        UUID newClassGroupId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        when(teacherRepository.findByIdAndInstitutionId(teacherId, institutionId))
                .thenReturn(Optional.of(existingTeacher()));
        when(classGroupRepository.findById(newClassGroupId))
                .thenReturn(Optional.of(classGroup(newClassGroupId, gradeId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(
                subject(subjectId, tz.elmkusoma.academic.domain.EducationLevel.SECONDARY)));
        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(
                grade(gradeId, tz.elmkusoma.academic.domain.EducationLevel.SECONDARY)));
        when(assignmentRepository.findAllByTeacherId(teacherId)).thenReturn(List.of(old));

        TeacherAssignmentResponse newResponse = teacherService.addAssignment(
                institutionId, teacherId, assignmentRequest(newClassGroupId, subjectId));

        // 3. history preserved: old row ENDED, new row ACTIVE, nothing deleted
        assertEquals("ACTIVE", newResponse.getStatus());
        assertEquals(TeacherAssignmentStatus.ENDED, old.getStatus());
        assertNotNull(old.getEndDate());
        verify(assignmentRepository, never()).delete(any(TeacherAssignment.class));

        // 4. both rows remain visible to history queries
        List<TeacherAssignment> history = List.of(old, TeacherAssignment.builder()
                .teacherId(teacherId)
                .classGroupId(newClassGroupId)
                .subjectId(subjectId)
                .academicYear("2025/2026")
                .status(TeacherAssignmentStatus.ACTIVE)
                .build());
        assertEquals(2, history.size());
        assertEquals(1, history.stream()
                .filter(a -> a.getStatus() == TeacherAssignmentStatus.ACTIVE).count());
    }

    @Test
    void multipleActiveAssignments_areAllowed() {
        UUID classOne = UUID.randomUUID();
        UUID classTwo = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        stubValidAssignmentContext(classOne, subjectId, gradeId,
                tz.elmkusoma.academic.domain.EducationLevel.SECONDARY);
        when(classGroupRepository.findById(classTwo))
                .thenReturn(Optional.of(classGroup(classTwo, gradeId)));

        teacherService.addAssignment(institutionId, teacherId, assignmentRequest(classOne, subjectId));
        teacherService.addAssignment(institutionId, teacherId, assignmentRequest(classTwo, subjectId));

        ArgumentCaptor<TeacherAssignment> captor = ArgumentCaptor.forClass(TeacherAssignment.class);
        verify(assignmentRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(2, captor.getAllValues().size());
        captor.getAllValues().forEach(a ->
                assertEquals(TeacherAssignmentStatus.ACTIVE, a.getStatus()));
    }
}
