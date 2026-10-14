package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.dto.request.AssignmentRequest;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.dto.response.LessonResponse;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.service.impl.LearningServiceImpl;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Audience-injection hardening of {@link LearningServiceImpl}: create/retarget
 * must reject class groups and subjects that do not exist or belong to another
 * institution, TEACHER callers must hold an active teaching assignment for the
 * target class, admins satisfy institution containment only, and the
 * institution-scoped lesson reads must not serve another tenant's class.
 */
@ExtendWith(MockitoExtension.class)
class LessonAudienceInjectionSecurityTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentSubmissionRepository submissionRepository;
    @Mock
    private StudentClassAssignmentRepository studentClassAssignmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClassGroupRepository classGroupRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private TeacherAssignmentRepository teacherAssignmentRepository;
    @Spy
    private OwnershipGuard ownershipGuard = new OwnershipGuard();

    @InjectMocks
    private LearningServiceImpl service;

    private UUID institutionId;
    private UUID otherInstitutionId;
    private UUID subjectId;
    private UUID classGroupId;
    private UUID teacherUserId;
    private UUID teacherId;
    private UUID assignmentId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        teacherUserId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();

        // The audience rule now lives in AudienceTargetGuard rather than as private helpers on the
        // service. Hand the service a real guard wired to the same collaborators - the spy
        // OwnershipGuard and these repository mocks - so these tests still execute the genuine
        // validation path and still assert ForbiddenException / SecurityException rather than
        // asserting against a stubbed-out no-op.
        org.springframework.test.util.ReflectionTestUtils.setField(service, "audienceTargetGuard",
                new tz.elmkusoma.common.AudienceTargetGuard(ownershipGuard, classGroupRepository,
                        subjectRepository, teacherRepository, teacherAssignmentRepository));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ClassGroup classGroup(UUID inst) {
        return ClassGroup.builder().id(classGroupId).institutionId(inst).isDeleted(false).build();
    }

    private Subject subject(UUID inst) {
        return Subject.builder().id(subjectId).institutionId(inst).isDeleted(false).build();
    }

    private Teacher teacher() {
        Teacher teacher = Teacher.builder().userId(teacherUserId).build();
        teacher.setId(teacherId);
        return teacher;
    }

    private LessonRequest lessonRequest() {
        LessonRequest request = new LessonRequest();
        request.setSubjectId(subjectId);
        request.setClassGroupId(classGroupId);
        request.setTitle("New lesson");
        request.setSortOrder(0);
        return request;
    }

    private AssignmentRequest assignmentRequest() {
        AssignmentRequest request = new AssignmentRequest();
        request.setSubjectId(subjectId);
        request.setClassGroupId(classGroupId);
        request.setTitle("New assignment");
        request.setTotalMarks(10);
        return request;
    }

    private Assignment assignment() {
        return Assignment.builder()
                .id(assignmentId)
                .institutionId(institutionId)
                .subjectId(subjectId)
                .classGroupId(classGroupId)
                .title("Existing assignment")
                .totalMarks(10)
                .status("DRAFT")
                .isDeleted(false)
                .createdBy("owner@example.com")
                .build();
    }

    // ── createLesson: cross-tenant targets ──

    @Test
    void createLesson_classGroupOwnedByAnotherInstitutionIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.createLesson(
                institutionId, lessonRequest(), teacherUserId, "TEACHER"));

        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void createLesson_unknownClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createLesson(
                institutionId, lessonRequest(), teacherUserId, "TEACHER"));

        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void createLesson_subjectOwnedByAnotherInstitutionIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(institutionId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.createLesson(
                institutionId, lessonRequest(), null, "INSTITUTION_ADMIN"));

        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void createLesson_unknownSubjectIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(institutionId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createLesson(
                institutionId, lessonRequest(), null, "INSTITUTION_ADMIN"));

        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    // ── createLesson: teacher scope ──

    @Test
    void createLesson_teacherWithoutTeachingAssignmentIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(institutionId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject(institutionId)));
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacher()));
        when(teacherAssignmentRepository.existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(
                teacherId, classGroupId)).thenReturn(false);

        SecurityException ex = assertThrows(SecurityException.class, () -> service.createLesson(
                institutionId, lessonRequest(), teacherUserId, "TEACHER"));

        assertTrue(ex.getMessage().contains("not assigned"));
        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void createLesson_teacherWithoutTeacherProfileIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(institutionId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject(institutionId)));
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> service.createLesson(
                institutionId, lessonRequest(), teacherUserId, "TEACHER"));

        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void createLesson_teacherWithActiveTeachingAssignmentIsAccepted() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(institutionId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject(institutionId)));
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacher()));
        when(teacherAssignmentRepository.existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(
                teacherId, classGroupId)).thenReturn(true);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LessonResponse response = service.createLesson(institutionId, lessonRequest(), teacherUserId, "TEACHER");

        assertNotNull(response);
        assertEquals(classGroupId, response.getClassGroupId());
    }

    @Test
    void createLesson_institutionAdminWithoutTeachingAssignmentIsAccepted() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(institutionId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject(institutionId)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LessonResponse response = service.createLesson(institutionId, lessonRequest(), null, "INSTITUTION_ADMIN");

        assertNotNull(response);
        verifyNoInteractions(teacherRepository, teacherAssignmentRepository);
    }

    // ── createAssignment ──

    @Test
    void createAssignment_classGroupOwnedByAnotherInstitutionIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.createAssignment(
                institutionId, assignmentRequest(), "admin@example.com", "INSTITUTION_ADMIN"));

        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void createAssignment_unknownClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createAssignment(
                institutionId, assignmentRequest(), "admin@example.com", "INSTITUTION_ADMIN"));

        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void createAssignment_teacherWithoutTeachingAssignmentIsRejected() {
        User user = new User();
        user.setId(teacherUserId);

        when(userRepository.findByEmailAndIsDeletedFalse("teacher@example.com")).thenReturn(Optional.of(user));
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(institutionId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject(institutionId)));
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacher()));
        when(teacherAssignmentRepository.existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(
                teacherId, classGroupId)).thenReturn(false);

        SecurityException ex = assertThrows(SecurityException.class, () -> service.createAssignment(
                institutionId, assignmentRequest(), "teacher@example.com", "TEACHER"));

        assertTrue(ex.getMessage().contains("not assigned"));
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    // ── updateAssignment: retarget guard (controller pre-flight) ──

    @Test
    void assertAssignmentRetargetInScope_retargetToForeignClassGroupIsRejected() {
        UUID newClassGroupId = UUID.randomUUID();
        AssignmentRequest request = assignmentRequest();
        request.setClassGroupId(newClassGroupId);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment()));
        when(classGroupRepository.findById(newClassGroupId))
                .thenReturn(Optional.of(ClassGroup.builder()
                        .id(newClassGroupId).institutionId(otherInstitutionId).isDeleted(false).build()));

        assertThrows(ForbiddenException.class, () -> service.assertAssignmentRetargetInScope(
                assignmentId, institutionId, request, "owner@example.com", "INSTITUTION_ADMIN"));

        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void assertAssignmentRetargetInScope_retargetToUnknownClassGroupIsRejected() {
        UUID newClassGroupId = UUID.randomUUID();
        AssignmentRequest request = assignmentRequest();
        request.setClassGroupId(newClassGroupId);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment()));
        when(classGroupRepository.findById(newClassGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.assertAssignmentRetargetInScope(
                assignmentId, institutionId, request, "owner@example.com", "INSTITUTION_ADMIN"));

        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void assertAssignmentRetargetInScope_retargetToForeignSubjectIsRejected() {
        UUID newSubjectId = UUID.randomUUID();
        AssignmentRequest request = assignmentRequest();
        request.setSubjectId(newSubjectId);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment()));
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(institutionId)));
        when(subjectRepository.findById(newSubjectId))
                .thenReturn(Optional.of(Subject.builder()
                        .id(newSubjectId).institutionId(otherInstitutionId).isDeleted(false).build()));

        assertThrows(ForbiddenException.class, () -> service.assertAssignmentRetargetInScope(
                assignmentId, institutionId, request, "owner@example.com", "INSTITUTION_ADMIN"));

        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void assertAssignmentRetargetInScope_unchangedTargetsSkipValidation() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment()));

        assertDoesNotThrow(() -> service.assertAssignmentRetargetInScope(
                assignmentId, institutionId, assignmentRequest(), "owner@example.com", "TEACHER"));

        verifyNoInteractions(classGroupRepository, subjectRepository, teacherRepository, teacherAssignmentRepository);
    }

    @Test
    void assertAssignmentRetargetInScope_institutionAdminRetargetToOwnClassWithoutTeachingAssignmentIsAccepted() {
        UUID newClassGroupId = UUID.randomUUID();
        AssignmentRequest request = assignmentRequest();
        request.setClassGroupId(newClassGroupId);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment()));
        when(classGroupRepository.findById(newClassGroupId))
                .thenReturn(Optional.of(ClassGroup.builder()
                        .id(newClassGroupId).institutionId(institutionId).isDeleted(false).build()));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject(institutionId)));

        assertDoesNotThrow(() -> service.assertAssignmentRetargetInScope(
                assignmentId, institutionId, request, "admin@example.com", "INSTITUTION_ADMIN"));

        verifyNoInteractions(teacherRepository, teacherAssignmentRepository);
    }

    // ── institution-scoped lesson reads ──

    @Test
    void getLessonsByClass_foreignClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getLessonsByClass(classGroupId, institutionId, false));

        verifyNoInteractions(lessonRepository);
    }

    @Test
    void getLessonsByClass_unknownClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getLessonsByClass(classGroupId, institutionId, true));

        verifyNoInteractions(lessonRepository);
    }

    @Test
    void getLessonsBySubjectAndClass_foreignClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getLessonsBySubjectAndClass(
                subjectId, classGroupId, institutionId, true));

        verifyNoInteractions(lessonRepository);
    }

    @Test
    void getLessonsByClass_ownInstitutionReturnsPublishedLessons() {
        Lesson published = Lesson.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .subjectId(subjectId)
                .classGroupId(classGroupId)
                .title("Published lesson")
                .sortOrder(0)
                .isPublished(true)
                .status("PUBLISHED")
                .isDeleted(false)
                .build();

        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(classGroup(institutionId)));
        when(lessonRepository.findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(classGroupId))
                .thenReturn(List.of(published));

        List<LessonResponse> response = service.getLessonsByClass(classGroupId, institutionId, false);

        assertEquals(1, response.size());
        assertEquals("Published lesson", response.get(0).getTitle());
    }
}
