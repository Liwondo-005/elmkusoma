package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.dto.ResourceRequest;
import tz.elmkusoma.learning.dto.ResourceResponse;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.ResourceTagRepository;
import tz.elmkusoma.learning.repository.ResourceTaggingRepository;
import tz.elmkusoma.learning.repository.StudentSavedResourceRepository;
import tz.elmkusoma.learner.repository.LearnerEnrollmentRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherAssignment;
import tz.elmkusoma.teacher.domain.TeacherAssignmentStatus;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teacher-created class resources must target a verified teaching assignment.
 *
 * <p>Enforcement reuses the existing {@link TeacherAssignment} model — no
 * parallel targeting system. Rules verified here (spec §8): the target must
 * exist, be non-deleted, belong to the caller's institution, belong to the
 * caller's own teacher profile when the caller is a TEACHER, and be ACTIVE for
 * new or changed targets. CLASS_ONLY writes must carry an authoritative target
 * (assignment or lesson). Client-supplied ids are never trusted.</p>
 */
@ExtendWith(MockitoExtension.class)
class ResourceAssignmentTargetingTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private ResourceTagRepository tagRepository;
    @Mock private ResourceTaggingRepository taggingRepository;
    @Mock private StudentSavedResourceRepository savedResourceRepository;
    @Mock private UserRepository userRepository;
    @Mock private tz.elmkusoma.course.repository.CourseRepository courseRepository;
    @Mock private tz.elmkusoma.course.repository.CourseModuleRepository courseModuleRepository;
    @Mock private tz.elmkusoma.liveclass.service.MediaProxyService mediaProxyService;
    @Mock private ResourceMetadataExtractor metadataExtractor;
    @Mock private tz.elmkusoma.audit.service.AuditService auditService;
    @Mock private ClassAccessGuard classAccessGuard;
    @Mock private LearnerEnrollmentRepository learnerEnrollmentRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private NotificationService notificationService;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private TeacherRepository teacherRepository;

    @InjectMocks
    private ResourceService service;

    private UUID institutionId;
    private UUID otherInstitutionId;
    private UUID teacherUserId;
    private UUID teacherProfileId;
    private UUID otherTeacherProfileId;
    private UUID classGroupId;
    private UUID assignmentId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
        teacherUserId = UUID.randomUUID();
        teacherProfileId = UUID.randomUUID();
        otherTeacherProfileId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();
    }

    // ── helpers ──

    private TeacherAssignment activeOwnAssignment() {
        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacherId(teacherProfileId)
                .classGroupId(classGroupId)
                .academicYear("2025/2026")
                .status(TeacherAssignmentStatus.ACTIVE)
                .build();
        assignment.setId(assignmentId);
        assignment.setInstitutionId(institutionId);
        return assignment;
    }

    private Teacher callerTeacher() {
        Teacher teacher = Teacher.builder().userId(teacherUserId).build();
        teacher.setId(teacherProfileId);
        teacher.setInstitutionId(institutionId);
        return teacher;
    }

    private ResourceRequest classOnlyRequest() {
        return ResourceRequest.builder()
                .title("Class notes")
                .resourceType("PDF")
                .storageUrl("https://storage.test/notes.pdf")
                .visibility("CLASS_ONLY")
                .teacherAssignmentId(assignmentId)
                .build();
    }

    private void stubAssignment(TeacherAssignment assignment) {
        when(teacherAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    }

    private void stubCallerTeacher() {
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(callerTeacher()));
    }

    private void stubSave() {
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── create: assignment target validation ──

    @Test
    void create_targetingOwnActiveAssignment_isPersisted() {
        stubAssignment(activeOwnAssignment());
        stubCallerTeacher();
        stubSave();

        ResourceResponse response = service.createResource(
                classOnlyRequest(), institutionId, teacherUserId, "TEACHER");

        assertNotNull(response);
        assertEquals(assignmentId, response.getTeacherAssignmentId());

        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceRepository).save(captor.capture());
        assertEquals(assignmentId, captor.getValue().getTeacherAssignmentId());
        assertEquals(institutionId, captor.getValue().getInstitutionId());
    }

    @Test
    void create_targetingAnotherTeachersAssignment_isRejected() {
        TeacherAssignment foreign = activeOwnAssignment();
        foreign.setTeacherId(otherTeacherProfileId);
        stubAssignment(foreign);
        stubCallerTeacher();

        SecurityException ex = assertThrows(SecurityException.class, () ->
                service.createResource(classOnlyRequest(), institutionId, teacherUserId, "TEACHER"));

        assertTrue(ex.getMessage().contains("own teaching assignments"),
                "denial must state ownership is required");
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void create_targetingAssignmentOfAnotherInstitution_isNotFound() {
        TeacherAssignment foreign = activeOwnAssignment();
        foreign.setInstitutionId(otherInstitutionId);
        stubAssignment(foreign);

        assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class, () ->
                service.createResource(classOnlyRequest(), institutionId, teacherUserId, "TEACHER"));
        verify(resourceRepository, never()).save(any());
        // cross-institution ids must never reach the ownership lookup
        verify(teacherRepository, never()).findByUserIdAndInstitutionId(any(), any());
    }

    @Test
    void create_targetingMissingAssignment_isNotFound() {
        when(teacherAssignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class, () ->
                service.createResource(classOnlyRequest(), institutionId, teacherUserId, "TEACHER"));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void create_targetingEndedAssignment_isRejected() {
        TeacherAssignment ended = activeOwnAssignment();
        ended.setStatus(TeacherAssignmentStatus.ENDED);
        stubAssignment(ended);
        stubCallerTeacher();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.createResource(classOnlyRequest(), institutionId, teacherUserId, "TEACHER"));

        assertTrue(ex.getMessage().contains("ENDED"),
                "an ended assignment must never host new resources");
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void create_softDeletedAssignment_isNotFound() {
        TeacherAssignment deleted = activeOwnAssignment();
        deleted.setIsDeleted(true);
        stubAssignment(deleted);

        assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class, () ->
                service.createResource(classOnlyRequest(), institutionId, teacherUserId, "TEACHER"));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void create_learnerRole_cannotTargetAssignment() {
        stubAssignment(activeOwnAssignment());

        SecurityException ex = assertThrows(SecurityException.class, () ->
                service.createResource(classOnlyRequest(), institutionId, teacherUserId, "STUDENT"));

        assertTrue(ex.getMessage().contains("Learners cannot"));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void create_adminMayTargetOwnInstitutionAssignment_withoutTeacherOwnershipCheck() {
        stubAssignment(activeOwnAssignment());
        stubSave();

        assertDoesNotThrow(() -> service.createResource(
                classOnlyRequest(), institutionId, UUID.randomUUID(), "INSTITUTION_ADMIN"));

        verify(teacherRepository, never()).findByUserIdAndInstitutionId(any(), any());
        verify(resourceRepository).save(any(Resource.class));
    }

    // ── create: CLASS_ONLY needs an authoritative target ──

    @Test
    void create_classOnlyWithoutAnyTarget_isRejected() {
        ResourceRequest request = ResourceRequest.builder()
                .title("Untargeted class resource")
                .resourceType("PDF")
                .storageUrl("https://storage.test/x.pdf")
                .visibility("CLASS_ONLY")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.createResource(request, institutionId, teacherUserId, "TEACHER"));

        assertTrue(ex.getMessage().contains("teaching assignment or a lesson"),
                "CLASS_ONLY without a class target must be rejected");
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void create_classOnlyLinkedToOwnLesson_isAllowed() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = Lesson.builder().institutionId(institutionId).title("Lesson").build();
        lesson.setIsDeleted(false);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        stubSave();

        ResourceRequest request = ResourceRequest.builder()
                .title("Lesson material")
                .resourceType("PDF")
                .storageUrl("https://storage.test/l.pdf")
                .visibility("CLASS_ONLY")
                .lessonId(lessonId)
                .build();

        assertDoesNotThrow(() ->
                service.createResource(request, institutionId, teacherUserId, "TEACHER"));
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void create_publicResourceWithoutTarget_isUnchanged() {
        stubSave();

        ResourceRequest request = ResourceRequest.builder()
                .title("Open resource")
                .resourceType("PDF")
                .storageUrl("https://storage.test/open.pdf")
                .visibility("PUBLIC")
                .build();

        assertDoesNotThrow(() ->
                service.createResource(request, institutionId, teacherUserId, "TEACHER"));

        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceRepository).save(captor.capture());
        assertEquals(null, captor.getValue().getTeacherAssignmentId());
    }

    // ── update: retargeting and visibility flips ──

    private Resource ownedPublicResource() {
        Resource resource = Resource.builder()
                .institutionId(institutionId)
                .uploadedBy(teacherUserId)
                .title("Owned")
                .resourceType(Resource.ResourceType.PDF)
                .storageUrl("https://storage.test/owned.pdf")
                .visibility(Resource.ResourceVisibility.PUBLIC)
                .sortOrder(0)
                .build();
        resource.setId(UUID.randomUUID());
        resource.setIsDeleted(false);
        return resource;
    }

    @Test
    void update_retargetToOwnActiveAssignment_isApplied() {
        Resource resource = ownedPublicResource();
        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
        stubAssignment(activeOwnAssignment());
        stubCallerTeacher();
        stubSave();

        ResourceRequest request = ResourceRequest.builder()
                .title("Retargeted")
                .teacherAssignmentId(assignmentId)
                .build();

        ResourceResponse response = service.updateResource(
                resource.getId(), request, institutionId, teacherUserId, "TEACHER");

        assertNotNull(response);
        assertEquals(assignmentId, response.getTeacherAssignmentId());
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void update_retargetToEndedAssignment_isRejected() {
        Resource resource = ownedPublicResource();
        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
        TeacherAssignment ended = activeOwnAssignment();
        ended.setStatus(TeacherAssignmentStatus.ENDED);
        stubAssignment(ended);
        stubCallerTeacher();

        ResourceRequest request = ResourceRequest.builder()
                .title("Retarget to ended")
                .teacherAssignmentId(assignmentId)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                service.updateResource(resource.getId(), request, institutionId, teacherUserId, "TEACHER"));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void update_retargingToForeignAssignment_isNotFound() {
        Resource resource = ownedPublicResource();
        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
        TeacherAssignment foreign = activeOwnAssignment();
        foreign.setInstitutionId(otherInstitutionId);
        stubAssignment(foreign);

        ResourceRequest request = ResourceRequest.builder()
                .title("Cross tenant")
                .teacherAssignmentId(assignmentId)
                .build();

        assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class, () ->
                service.updateResource(resource.getId(), request, institutionId, teacherUserId, "TEACHER"));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void update_flippingToClassOnlyWithoutTarget_isRejected() {
        Resource resource = ownedPublicResource();
        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));

        ResourceRequest request = ResourceRequest.builder()
                .visibility("CLASS_ONLY")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.updateResource(resource.getId(), request, institutionId, teacherUserId, "TEACHER"));

        assertTrue(ex.getMessage().contains("teaching assignment or a lesson"));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void update_legacyClassOnlyTitleEdit_withoutVisibilityChange_stillAllowed() {
        // Legacy rows (CLASS_ONLY without any target) must remain editable.
        Resource resource = ownedPublicResource();
        resource.setVisibility(Resource.ResourceVisibility.CLASS_ONLY);
        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
        stubSave();

        ResourceRequest request = ResourceRequest.builder().title("Legacy edit").build();

        assertDoesNotThrow(() ->
                service.updateResource(resource.getId(), request, institutionId, teacherUserId, "TEACHER"));
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void update_legacyClassOnlyEdit_echoingCurrentVisibility_stillAllowed() {
        // The edit form always resends visibility — echoing the stored value
        // is a no-op and must not trip the CLASS_ONLY target requirement.
        Resource resource = ownedPublicResource();
        resource.setVisibility(Resource.ResourceVisibility.CLASS_ONLY);
        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
        stubSave();

        ResourceRequest request = ResourceRequest.builder()
                .title("Legacy edit")
                .visibility("CLASS_ONLY")
                .build();

        assertDoesNotThrow(() ->
                service.updateResource(resource.getId(), request, institutionId, teacherUserId, "TEACHER"));
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void update_resendingSameTargetOnTitleEdit_isNoOp_evenIfAssignmentEnded() {
        // The resource already carries this target; editing its title must not
        // be blocked because the assignment ended in the meantime.
        Resource resource = ownedPublicResource();
        resource.setTeacherAssignmentId(assignmentId);
        TeacherAssignment ended = activeOwnAssignment();
        ended.setStatus(TeacherAssignmentStatus.ENDED);
        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
        stubSave();

        ResourceRequest request = ResourceRequest.builder()
                .title("Edited title")
                .teacherAssignmentId(assignmentId)
                .build();

        assertDoesNotThrow(() ->
                service.updateResource(resource.getId(), request, institutionId, teacherUserId, "TEACHER"));
        verify(resourceRepository).save(any(Resource.class));
        verify(teacherAssignmentRepository, never()).findById(any());
    }

    @Test
    void update_classOnlyLinkedToLesson_withoutAssignment_isAllowed() {
        Resource resource = ownedPublicResource();
        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = Lesson.builder().institutionId(institutionId).title("Lesson").build();
        lesson.setIsDeleted(false);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        stubSave();

        ResourceRequest request = ResourceRequest.builder()
                .visibility("CLASS_ONLY")
                .lessonId(lessonId)
                .build();

        assertDoesNotThrow(() ->
                service.updateResource(resource.getId(), request, institutionId, teacherUserId, "TEACHER"));
        verify(resourceRepository).save(any(Resource.class));
    }
}
