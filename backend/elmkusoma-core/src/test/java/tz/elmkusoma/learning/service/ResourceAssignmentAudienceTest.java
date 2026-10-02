package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.ResourceTagRepository;
import tz.elmkusoma.learning.repository.ResourceTaggingRepository;
import tz.elmkusoma.learning.repository.StudentSavedResourceRepository;
import tz.elmkusoma.learner.repository.LearnerEnrollmentRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherAssignment;
import tz.elmkusoma.teacher.domain.TeacherAssignmentStatus;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Read-side audience for assignment-targeted (class) resources — spec §9.
 *
 * <p>Verification as appropriate: authenticated user, same institution (via the
 * existing {@code requireReadAccess} chain), learner's current academic/class
 * context through the existing {@link ClassAccessGuard} (student_class_assignments
 * ∪ ENROLLED enrollments — membership must be proven; the shared guard's
 * "no data → allow" fallback never opens a class-targeted resource),
 * visibility, not deleted. A URL parameter such as
 * {@code ?classGroupId=} is never consulted — the target comes from the stored
 * row only. Teachers outside the target class are denied; the uploader, admins
 * and jurisdiction governance keep their existing access. ENDED assignments
 * still serve their class so historical resources survive reassignment, and
 * legacy resources without a target behave exactly as before.</p>
 */
@ExtendWith(MockitoExtension.class)
class ResourceAssignmentAudienceTest {

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

    private UUID resourceId;
    private UUID institutionId;
    private UUID uploaderUserId;
    private UUID learnerId;
    private UUID otherTeacherUserId;
    private UUID teacherProfileId;
    private UUID classGroupId;
    private UUID assignmentId;

    @BeforeEach
    void setUp() {
        resourceId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        uploaderUserId = UUID.randomUUID();
        learnerId = UUID.randomUUID();
        otherTeacherUserId = UUID.randomUUID();
        teacherProfileId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();
    }

    // ── helpers ──

    private Resource targetedResource() {
        Resource resource = Resource.builder()
                .institutionId(institutionId)
                .uploadedBy(uploaderUserId)
                .title("Class-only resource")
                .resourceType(Resource.ResourceType.PDF)
                .storageUrl("https://storage.test/class.pdf")
                .visibility(Resource.ResourceVisibility.CLASS_ONLY)
                .teacherAssignmentId(assignmentId)
                .sortOrder(0)
                .build();
        resource.setId(resourceId);
        resource.setIsDeleted(false);
        return resource;
    }

    private void stubStored(Resource resource) {
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
    }

    private void stubAssignment() {
        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacherId(teacherProfileId)
                .classGroupId(classGroupId)
                .academicYear("2025/2026")
                .status(TeacherAssignmentStatus.ACTIVE)
                .build();
        assignment.setId(assignmentId);
        assignment.setInstitutionId(institutionId);
        when(teacherAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    }

    private void stubAssignmentStatus(TeacherAssignmentStatus status) {
        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacherId(teacherProfileId)
                .classGroupId(classGroupId)
                .academicYear("2025/2026")
                .status(status)
                .build();
        assignment.setId(assignmentId);
        assignment.setInstitutionId(institutionId);
        when(teacherAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    }

    private void stubLearnerEmail() {
        User user = new User();
        user.setEmail("learner@test.com");
        when(userRepository.findById(learnerId)).thenReturn(Optional.of(user));
    }

    private void stubCallerTeacherProfile() {
        Teacher teacher = Teacher.builder().userId(otherTeacherUserId).build();
        teacher.setId(teacherProfileId);
        teacher.setInstitutionId(institutionId);
        when(teacherRepository.findByUserIdAndInstitutionId(otherTeacherUserId, institutionId))
                .thenReturn(Optional.of(teacher));
    }

    // ── learners: current class context decides ──

    @Test
    void learner_memberOfTargetClass_canRead() {
        stubStored(targetedResource());
        stubAssignment();
        stubLearnerEmail();
        when(classAccessGuard.isLearnerInClass("learner@test.com", classGroupId)).thenReturn(true);

        assertDoesNotThrow(() -> service.requireVisible(resourceId, institutionId, learnerId, "STUDENT"));

        verify(classAccessGuard).isLearnerInClass("learner@test.com", classGroupId);
    }

    @Test
    void learner_ofAnotherClass_deniedByExistingClassGuard() {
        Resource stored = targetedResource();
        stubStored(stored);
        stubAssignment();
        stubLearnerEmail();
        when(classAccessGuard.isLearnerInClass("learner@test.com", classGroupId)).thenReturn(false);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.requireVisible(resourceId, institutionId, learnerId, "STUDENT"));

        assertTrue(ex.getMessage().contains("member of this class"));
        assertFalse(service.canRead(stored, learnerId, "STUDENT"),
                "the list predicate must deny the same resource");
    }

    @Test
    void learnerWithoutAnyClassMembership_denied_noDataFallbackNeverOpens() {
        // Regression: a learner with no student profile (e.g. OTHER_LEARNER)
        // must not fall through the shared guard's "no data → allow" fallback
        // for assignment-targeted resources.
        Resource stored = targetedResource();
        stubStored(stored);
        stubAssignment();
        stubLearnerEmail();
        when(classAccessGuard.isLearnerInClass("learner@test.com", classGroupId)).thenReturn(false);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.requireVisible(resourceId, institutionId, learnerId, "OTHER_LEARNER"));

        assertTrue(ex.getMessage().contains("member of this class"));
        assertFalse(service.canRead(stored, learnerId, "OTHER_LEARNER"),
                "the list predicate must deny learners without proven class membership");
    }

    @Test
    void list_forLearner_excludesResourcesTargetingOtherClasses() {
        Resource forOtherClass = targetedResource();
        Resource plain = Resource.builder()
                .institutionId(institutionId)
                .uploadedBy(uploaderUserId)
                .title("Institution-wide resource")
                .resourceType(Resource.ResourceType.PDF)
                .storageUrl("https://storage.test/plain.pdf")
                .visibility(Resource.ResourceVisibility.PUBLIC)
                .sortOrder(0)
                .build();
        plain.setId(UUID.randomUUID());
        plain.setIsDeleted(false);

        when(resourceRepository.findByInstitutionIdAndVisibilities(
                eq(institutionId), any()))
                .thenReturn(List.of(forOtherClass, plain));
        stubAssignment();
        stubLearnerEmail();
        when(classAccessGuard.isLearnerInClass("learner@test.com", classGroupId)).thenReturn(false);

        var result = service.listResources(
                institutionId, learnerId, "STUDENT", null, null, null, null, null, 0, 20);

        assertEquals(1, result.size(), "other-class targets must be filtered server-side");
        assertEquals("Institution-wide resource", result.get(0).getTitle());
    }

    // ── teachers: same class or uploader only ──

    @Test
    void teacherOutsideTargetClass_denied() {
        stubStored(targetedResource());
        stubAssignment();
        stubCallerTeacherProfile();
        when(teacherAssignmentRepository
                .existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(teacherProfileId, classGroupId))
                .thenReturn(false);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.requireVisible(resourceId, institutionId, otherTeacherUserId, "TEACHER"));

        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    void teacherHoldingAssignmentForTargetClass_allowed() {
        stubStored(targetedResource());
        stubAssignment();
        stubCallerTeacherProfile();
        when(teacherAssignmentRepository
                .existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(teacherProfileId, classGroupId))
                .thenReturn(true);

        assertDoesNotThrow(() ->
                service.requireVisible(resourceId, institutionId, otherTeacherUserId, "TEACHER"));
    }

    @Test
    void uploadingTeacher_keepsAccessWithoutExtraLookups() {
        stubStored(targetedResource());
        stubAssignment();

        assertDoesNotThrow(() ->
                service.requireVisible(resourceId, institutionId, uploaderUserId, "TEACHER"));

        verify(teacherAssignmentRepository, never())
                .existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(any(), any());
        verifyNoInteractions(classAccessGuard);
    }

    // ── governance and legacy compatibility ──

    @Test
    void institutionAdmin_bypassesAssignmentGate() {
        stubStored(targetedResource());

        assertDoesNotThrow(() ->
                service.requireVisible(resourceId, institutionId, UUID.randomUUID(), "INSTITUTION_ADMIN"));

        verifyNoInteractions(classAccessGuard);
        verify(teacherAssignmentRepository, never()).findById(any());
    }

    @Test
    void regionalAdmin_jurisdictionRead_bypassesAssignmentGate() {
        stubStored(targetedResource());
        UUID regionId = UUID.randomUUID();
        Institution institution = new Institution();
        institution.setRegionId(regionId);
        when(institutionRepository.findByIdAndIsDeletedFalse(institutionId))
                .thenReturn(Optional.of(institution));
        User user = new User();
        user.setRegionId(regionId);
        UUID regionalUserId = UUID.randomUUID();
        when(userRepository.findById(regionalUserId)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() ->
                service.requireVisible(resourceId, institutionId, regionalUserId, "REGIONAL_ADMIN"));

        verifyNoInteractions(classAccessGuard);
        verify(teacherAssignmentRepository, never()).findById(any());
    }

    @Test
    void endedAssignment_stillServesItsClass_historyPreserved() {
        stubStored(targetedResource());
        stubAssignmentStatus(TeacherAssignmentStatus.ENDED);
        stubLearnerEmail();

        // Reassignment ends the assignment but never strands its resources.
        when(classAccessGuard.isLearnerInClass("learner@test.com", classGroupId)).thenReturn(true);
        assertDoesNotThrow(() -> service.requireVisible(resourceId, institutionId, learnerId, "STUDENT"));
        verify(classAccessGuard).isLearnerInClass("learner@test.com", classGroupId);
    }

    @Test
    void legacyResourceWithoutTarget_isUntouchedByNewGate() {
        Resource legacy = targetedResource();
        legacy.setTeacherAssignmentId(null);
        stubStored(legacy);

        assertDoesNotThrow(() -> service.requireVisible(resourceId, institutionId, learnerId, "STUDENT"));

        verifyNoInteractions(classAccessGuard);
        verify(teacherAssignmentRepository, never()).findById(any());
    }

    @Test
    void learnerCannotReceiveTargetedResource_throughListWithoutGuardApproval() {
        Resource forOtherClass = targetedResource();
        when(resourceRepository.findByInstitutionIdAndVisibilities(
                eq(institutionId), any()))
                .thenReturn(List.of(forOtherClass));
        stubAssignment();
        stubLearnerEmail();
        when(classAccessGuard.isLearnerInClass("learner@test.com", classGroupId)).thenReturn(false);

        var result = service.listResources(
                institutionId, learnerId, "STUDENT", null, null, null, null, null, 0, 20);

        assertTrue(result.isEmpty(), "a learner must never receive other-class targets");
    }
}
