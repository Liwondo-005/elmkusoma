package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.assessment.repository.AssessmentResultRepository;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.grading.service.impl.GradebookServiceImpl;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.controller.StudentController;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.dto.StudentRequest;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.service.StudentService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Institution membership is a tenant boundary, not a hint.
 *
 * <p>Four places took an identifier the client chose - a body field, a query parameter, a path
 * parameter - and treated it as authoritative, so a caller could write into or read out of another
 * school. Each is pinned here with both a denial and a positive control, so a fix that merely
 * empties every response would fail just as loudly as the hole it replaced.</p>
 */
class TenantBoundaryRegressionTest {

    private UUID myInstitution;
    private UUID foreignInstitution;
    private UUID myClass;
    private UUID foreignClass;
    private UUID learnerId;

    @BeforeEach
    void setUp() {
        myInstitution = UUID.randomUUID();
        foreignInstitution = UUID.randomUUID();
        myClass = UUID.randomUUID();
        foreignClass = UUID.randomUUID();
        learnerId = UUID.randomUUID();
    }

    // ---- create: the tenant is never the client's choice ---------------------------------

    @Test
    void createStudent_ignoresTheInstitutionTheClientAskedFor() throws Exception {
        StudentService studentService = mock(StudentService.class);
        StudentController controller = new StudentController(
                studentService, mock(ParentRepository.class), mock(ParentStudentLinkRepository.class));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String body = """
                {
                  "institutionId": "%s",
                  "userId": "%s",
                  "firstName": "Amina",
                  "lastName": "Juma",
                  "email": "amina@example.com"
                }
                """.formatted(foreignInstitution, learnerId);

        mockMvc.perform(post("/v1/students")
                        .requestAttr("institutionId", myInstitution)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<StudentRequest> captor = ArgumentCaptor.forClass(StudentRequest.class);
        verify(studentService).createStudent(captor.capture());

        assertEquals(myInstitution, captor.getValue().getInstitutionId(),
                "the stored tenant must come from the caller's resolved membership, not the body - "
                        + "otherwise any TEACHER or INSTITUTION_ADMIN could enrol a student into "
                        + "another school");
    }

    // ---- read by a client-supplied class id ----------------------------------------------

    @Test
    void getStudents_byAnotherInstitutionsClassId_returnsNoRoster() {
        StudentService service = newStudentService();
        StudentClassAssignment assignment = assignmentIn(foreignClass, foreignStudent());

        when(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(foreignClass))
                .thenReturn(List.of(assignment));

        assertTrue(service.getStudents(myInstitution, foreignClass).isEmpty(),
                "the class id proves nothing about the caller's tenant, so it must not return "
                        + "another school's roster");
    }

    @Test
    void getStudents_byOwnClassId_returnsTheRoster() {
        StudentService service = newStudentService();
        StudentClassAssignment assignment = assignmentIn(myClass, ownStudent());

        when(assignmentRepository.findByClassGroupIdAndIsDeletedFalse(myClass))
                .thenReturn(List.of(assignment));

        assertEquals(1, service.getStudents(myInstitution, myClass).size(),
                "the filter must narrow to the tenant, not empty the endpoint");
    }

    // ---- a client-supplied class id on the gradebook --------------------------------------

    @Test
    void getGradebook_ofAnotherInstitutionsClass_isForbidden() {
        GradebookServiceImpl service = newGradebookService();

        ClassGroup foreign = new ClassGroup();
        foreign.setId(foreignClass);
        foreign.setInstitutionId(foreignInstitution);
        foreign.setName("Foreign class");
        when(classGroupRepository.findById(foreignClass)).thenReturn(Optional.of(foreign));

        assertThrows(ForbiddenException.class,
                () -> service.getGradebook(foreignClass, myInstitution),
                "a foreign class id must be refused before its name or roster is resolved");
    }

    @Test
    void getGradebook_ofOwnClass_isServed() {
        GradebookServiceImpl service = newGradebookService();

        ClassGroup own = new ClassGroup();
        own.setId(myClass);
        own.setInstitutionId(myInstitution);
        own.setName("Form 2A");
        when(classGroupRepository.findById(myClass)).thenReturn(Optional.of(own));

        assertNotNull(service.getGradebook(myClass, myInstitution),
                "the tenant check must not refuse a class the caller's institution owns");
    }

    // ---- fixtures -------------------------------------------------------------------------

    private StudentRepository studentRepository;
    private StudentClassAssignmentRepository assignmentRepository;
    private ClassGroupRepository classGroupRepository;
    private ClassAccessGuard classAccessGuard;

    private StudentService newStudentService() {
        studentRepository = mock(StudentRepository.class);
        assignmentRepository = mock(StudentClassAssignmentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        return new StudentService(studentRepository, assignmentRepository,
                mock(InstitutionRepository.class), userRepository);
    }

    private GradebookServiceImpl newGradebookService() {
        classAccessGuard = mock(ClassAccessGuard.class);
        classGroupRepository = mock(ClassGroupRepository.class);
        when(classAccessGuard.resolveClassStudentIds(any())).thenReturn(List.of());
        return new GradebookServiceImpl(
                classAccessGuard,
                mock(AssignmentRepository.class),
                mock(AssignmentSubmissionRepository.class),
                mock(AssessmentRepository.class),
                mock(AssessmentResultRepository.class),
                mock(StudentRepository.class),
                mock(UserRepository.class),
                classGroupRepository);
    }

    private Student studentIn(UUID institutionId) {
        Student student = new Student();
        student.setId(UUID.randomUUID());
        student.setUserId(UUID.randomUUID());
        student.setInstitutionId(institutionId);
        student.setAdmissionNumber("ADM-" + institutionId.toString().substring(0, 8));
        student.setIsDeleted(false);
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        return student;
    }

    private Student foreignStudent() {
        return studentIn(foreignInstitution);
    }

    private Student ownStudent() {
        return studentIn(myInstitution);
    }

    private StudentClassAssignment assignmentIn(UUID classGroupId, Student student) {
        StudentClassAssignment assignment = new StudentClassAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setClassGroupId(classGroupId);
        assignment.setStudentId(student.getId());
        assignment.setIsDeleted(false);
        return assignment;
    }
}
