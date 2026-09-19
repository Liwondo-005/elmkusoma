package tz.elmkusoma.student.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.dto.AssignClassRequest;
import tz.elmkusoma.student.dto.StudentRequest;
import tz.elmkusoma.student.dto.StudentResponse;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentClassAssignmentRepository assignmentRepository;
    @Mock
    private InstitutionRepository institutionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudentService studentService;

    private UUID institutionId;
    private UUID userId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        studentId = UUID.randomUUID();
    }

    @Test
    void createStudent_shouldGenerateAdmissionNumberAndSave() {
        StudentRequest request = new StudentRequest();
        request.setInstitutionId(institutionId);
        request.setUserId(userId);
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");

        Institution institution = Institution.builder()
                .id(institutionId)
                .name("Test School")
                .code("TST")
                .build();

        when(studentRepository.existsByUserIdAndIsDeletedFalse(userId)).thenReturn(false);
        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));
        when(studentRepository.countByInstitutionId(institutionId)).thenReturn(5L);

        Student savedStudent = Student.builder()
                .institutionId(institutionId)
                .userId(userId)
                .admissionNumber("TST-2026-00006")
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .build();
        savedStudent.setId(studentId);

        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().id(userId).firstName("John").lastName("Doe").email("john@example.com").build()
        ));

        StudentResponse response = studentService.createStudent(request);

        assertNotNull(response);
        assertEquals("TST-2026-00006", response.getAdmissionNumber());
        assertEquals(institutionId, response.getInstitutionId());
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void createStudent_whenDuplicateUser_shouldThrow() {
        StudentRequest request = new StudentRequest();
        request.setInstitutionId(institutionId);
        request.setUserId(userId);

        when(studentRepository.existsByUserIdAndIsDeletedFalse(userId)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> studentService.createStudent(request));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void getStudent_shouldReturnStudentWithUserDetails() {
        Student student = Student.builder()
                .institutionId(institutionId)
                .userId(userId)
                .admissionNumber("TST-2026-00001")
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .build();
        student.setId(studentId);

        User user = User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        StudentResponse response = studentService.getStudent(studentId);

        assertNotNull(response);
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("TST-2026-00001", response.getAdmissionNumber());
    }

    @Test
    void getStudent_whenNotFound_shouldThrow() {
        UUID nonExistentId = UUID.randomUUID();
        when(studentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.getStudent(nonExistentId));
    }

    @Test
    void updateStudent_shouldUpdateFieldsAndSave() {
        Student student = Student.builder()
                .institutionId(institutionId)
                .userId(userId)
                .admissionNumber("TST-2026-00001")
                .status(StudentStatus.ACTIVE)
                .gender("Male")
                .build();
        student.setId(studentId);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenReturn(student);
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().id(userId).firstName("John").lastName("Doe").build()
        ));

        StudentRequest request = new StudentRequest();
        request.setGender("Female");
        request.setCity("Dar es Salaam");
        request.setStatus(StudentStatus.ACTIVE);

        StudentResponse response = studentService.updateStudent(studentId, request);

        assertNotNull(response);
        assertEquals("Female", response.getGender());
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void assignToClass_shouldCreateNewAssignment() {
        Student student = Student.builder()
                .institutionId(institutionId)
                .userId(userId)
                .admissionNumber("TST-2026-00001")
                .status(StudentStatus.ACTIVE)
                .build();
        student.setId(studentId);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(assignmentRepository.findByStudentIdAndClassGroupIdAndIsActiveTrueAndIsDeletedFalse(
                eq(studentId), any(UUID.class))).thenReturn(Optional.empty());

        UUID classGroupId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();

        StudentClassAssignment savedAssignment = StudentClassAssignment.builder()
                .institutionId(institutionId)
                .studentId(studentId)
                .classGroupId(classGroupId)
                .academicYearId(academicYearId)
                .termId(termId)
                .assignedDate(LocalDateTime.now())
                .isActive(true)
                .build();

        when(assignmentRepository.save(any(StudentClassAssignment.class))).thenReturn(savedAssignment);

        AssignClassRequest request = new AssignClassRequest();
        request.setClassGroupId(classGroupId);
        request.setAcademicYearId(academicYearId);
        request.setTermId(termId);

        StudentClassAssignment result = studentService.assignToClass(studentId, request);

        assertNotNull(result);
        assertEquals(studentId, result.getStudentId());
        assertEquals(classGroupId, result.getClassGroupId());
        assertEquals(institutionId, result.getInstitutionId());
        verify(assignmentRepository).save(any(StudentClassAssignment.class));
    }

    @Test
    void countStudents_shouldReturnCorrectCount() {
        when(studentRepository.countByInstitutionId(institutionId)).thenReturn(150L);

        long count = studentService.countStudents(institutionId);

        assertEquals(150L, count);
        verify(studentRepository).countByInstitutionId(institutionId);
    }

    @Test
    void searchStudents_shouldReturnMatchingStudents() {
        Student student = Student.builder()
                .institutionId(institutionId)
                .userId(userId)
                .admissionNumber("TST-2026-00001")
                .status(StudentStatus.ACTIVE)
                .build();
        student.setId(studentId);

        when(studentRepository.search(institutionId, "John")).thenReturn(List.of(student));
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().id(userId).firstName("John").lastName("Doe").email("john@example.com").build()
        ));

        List<StudentResponse> results = studentService.searchStudents(institutionId, "John");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("John", results.get(0).getFirstName());
    }
}
