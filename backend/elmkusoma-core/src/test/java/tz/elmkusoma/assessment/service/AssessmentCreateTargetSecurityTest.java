package tz.elmkusoma.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.dto.request.AssessmentRequest;
import tz.elmkusoma.assessment.dto.response.AssessmentResponse;
import tz.elmkusoma.assessment.repository.*;
import tz.elmkusoma.assessment.service.impl.AssessmentServiceImpl;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Create-time audience validation: client-supplied class group / subject ids are
 * existence- and institution-checked before anything is persisted (the
 * {@code assessments} table has no FK on them), and a TEACHER caller must hold a
 * teaching assignment for the target class while admins are not forced to.
 * Denies: missing target → {@link ResourceNotFoundException} (404), foreign
 * target → {@link OwnershipGuard}/{@link ForbiddenException} (403), unassigned
 * teacher → {@code SecurityException} (403).
 */
@ExtendWith(MockitoExtension.class)
class AssessmentCreateTargetSecurityTest {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private OptionRepository optionRepository;
    @Mock private AttemptRepository attemptRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private AssessmentResultRepository resultRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private ClassAccessGuard classAccessGuard;
    @Mock private OwnershipGuard ownershipGuard;
    @Mock private ClassGroupRepository classGroupRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;

    @InjectMocks
    private AssessmentServiceImpl service;

    private UUID institutionId;
    private UUID callerUserId;
    private UUID classGroupId;
    private UUID subjectId;
    private UUID assessmentId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        callerUserId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        assessmentId = UUID.randomUUID();
    }

    private AssessmentRequest request() {
        AssessmentRequest request = new AssessmentRequest();
        request.setClassGroupId(classGroupId);
        request.setSubjectId(subjectId);
        request.setTitle("Algebra quiz");
        request.setTotalMarks(50);
        request.setPassMarks(30);
        return request;
    }

    private ClassGroup classGroup(UUID ownerInstitutionId) {
        return ClassGroup.builder()
                .id(classGroupId)
                .institutionId(ownerInstitutionId)
                .name("Form 1A")
                .isDeleted(false)
                .build();
    }

    private Subject subject(UUID ownerInstitutionId) {
        return Subject.builder()
                .id(subjectId)
                .institutionId(ownerInstitutionId)
                .name("Mathematics")
                .isDeleted(false)
                .build();
    }

    private Teacher teacher(UUID teacherId) {
        Teacher teacher = new Teacher();
        teacher.setId(teacherId);
        teacher.setUserId(callerUserId);
        return teacher;
    }

    // ── createAssessment ──────────────────────────────────────────────────────

    @Test
    void createAssessment_rejectsClassGroupOwnedByAnotherInstitution() {
        UUID foreignInstitutionId = UUID.randomUUID();
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroup(foreignInstitutionId)));
        doThrow(new ForbiddenException("You do not have access to this resource"))
                .when(ownershipGuard).verifyInstitution(foreignInstitutionId, institutionId);

        assertThrows(ForbiddenException.class, () -> service.createAssessment(
                institutionId, request(), "admin@test.com", "INSTITUTION_ADMIN", callerUserId));

        verify(ownershipGuard).verifyInstitution(foreignInstitutionId, institutionId);
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void createAssessment_rejectsNonExistentClassGroup() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createAssessment(
                institutionId, request(), "admin@test.com", "INSTITUTION_ADMIN", callerUserId));

        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void createAssessment_rejectsTeacherWithoutTeachingAssignmentForClass() {
        UUID teacherId = UUID.randomUUID();
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroup(institutionId)));
        when(subjectRepository.findById(subjectId))
                .thenReturn(Optional.of(subject(institutionId)));
        when(teacherRepository.findByUserIdAndInstitutionId(callerUserId, institutionId))
                .thenReturn(Optional.of(teacher(teacherId)));
        when(teacherAssignmentRepository.existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(
                teacherId, classGroupId)).thenReturn(false);

        assertThrows(SecurityException.class, () -> service.createAssessment(
                institutionId, request(), "teacher@test.com", "TEACHER", callerUserId));

        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void createAssessment_acceptsInstitutionAdminForOwnInstitutionClassWithoutTeachingAssignment() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroup(institutionId)));
        when(subjectRepository.findById(subjectId))
                .thenReturn(Optional.of(subject(institutionId)));
        when(assessmentRepository.save(any(Assessment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssessmentResponse response = service.createAssessment(
                institutionId, request(), "admin@test.com", "INSTITUTION_ADMIN", callerUserId);

        assertNotNull(response);
        assertEquals("Algebra quiz", response.getTitle());
        ArgumentCaptor<Assessment> captor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository).save(captor.capture());
        assertEquals(institutionId, captor.getValue().getInstitutionId());
        assertEquals(classGroupId, captor.getValue().getClassGroupId());
        assertEquals(subjectId, captor.getValue().getSubjectId());
        // Admins satisfy institution containment only — never an assignment lookup.
        verifyNoInteractions(teacherRepository, teacherAssignmentRepository);
    }

    // ── updateAssessment (retarget) ──────────────────────────────────────────

    @Test
    void updateAssessment_rejectsRetargetToAnotherInstitutionsClassGroup() {
        UUID foreignInstitutionId = UUID.randomUUID();
        UUID foreignClassGroupId = UUID.randomUUID();
        Assessment existing = Assessment.builder()
                .id(assessmentId)
                .institutionId(institutionId)
                .subjectId(subjectId)
                .classGroupId(classGroupId)
                .title("Algebra quiz")
                .totalMarks(50)
                .passMarks(30)
                .isPublished(false)
                .status("DRAFT")
                .isDeleted(false)
                .build();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(existing));
        when(classGroupRepository.findById(foreignClassGroupId))
                .thenReturn(Optional.of(ClassGroup.builder()
                        .id(foreignClassGroupId)
                        .institutionId(foreignInstitutionId)
                        .name("Foreign class")
                        .isDeleted(false)
                        .build()));
        doThrow(new ForbiddenException("You do not have access to this resource"))
                .when(ownershipGuard).verifyInstitution(foreignInstitutionId, institutionId);

        AssessmentRequest request = request();
        request.setClassGroupId(foreignClassGroupId);

        assertThrows(ForbiddenException.class, () -> service.updateAssessment(
                assessmentId, request, institutionId, "teacher@test.com", "TEACHER", callerUserId));

        verify(ownershipGuard).verifyInstitution(foreignInstitutionId, institutionId);
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }
}
