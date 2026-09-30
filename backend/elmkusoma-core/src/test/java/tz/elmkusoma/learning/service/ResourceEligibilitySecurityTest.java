package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.ResourceTagRepository;
import tz.elmkusoma.learning.repository.ResourceTaggingRepository;
import tz.elmkusoma.learning.repository.StudentSavedResourceRepository;
import tz.elmkusoma.learner.domain.LearnerEnrollment;
import tz.elmkusoma.learner.repository.LearnerEnrollmentRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Learner eligibility on the resource read path — closing the gap where a
 * member of the institution could read resources attached to courses/classes
 * they do not belong to.
 *
 * <p>Enforcement reuses the existing domain model only:</p>
 * <ul>
 *   <li>course link → {@code learner_enrollments} (strict: no enrollment, no
 *       read),</li>
 *   <li>lesson link → class membership via the existing
 *       {@link ClassAccessGuard} (its own fail-open rule when a class models
 *       no members is preserved),</li>
 *   <li>standalone resources stay governed by institution + visibility alone
 *       (no behavior change),</li>
 *   <li>teachers/admins/owners are never subject to the gate.</li>
 * </ul>
 *
 * <p>Matrix: authorized learner allowed · unauthorized learner denied · same
 * institution but wrong course/class denied · wrong institution denied
 * (existing) · random id not found (existing).</p>
 */
@ExtendWith(MockitoExtension.class)
class ResourceEligibilitySecurityTest {

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

    @InjectMocks
    private ResourceService service;

    private UUID resourceId;
    private UUID institutionId;
    private UUID learnerId;
    private UUID otherLearnerId;
    private UUID teacherId;
    private UUID enrolledCourseId;
    private UUID otherCourseId;
    private UUID lessonId;
    private UUID classGroupId;

    @BeforeEach
    void setUp() {
        resourceId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        learnerId = UUID.randomUUID();
        otherLearnerId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        enrolledCourseId = UUID.randomUUID();
        otherCourseId = UUID.randomUUID();
        lessonId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
    }

    // ── helpers ──

    private Resource standaloneResource() {
        return linkableResource(null, null, Resource.ResourceVisibility.PUBLIC);
    }

    private Resource linkableResource(UUID courseId, UUID lessonId, Resource.ResourceVisibility visibility) {
        return Resource.builder()
                .id(resourceId)
                .institutionId(institutionId)
                .uploadedBy(teacherId)
                .title("Linked resource")
                .resourceType(Resource.ResourceType.PDF)
                .storageUrl("https://storage.test/linked.pdf")
                .courseId(courseId)
                .lessonId(lessonId)
                .visibility(visibility)
                .sortOrder(0)
                .isDeleted(false)
                .build();
    }

    private void stubStored(Resource resource) {
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
    }

    private void stubLearnerEmail() {
        User user = new User();
        user.setEmail("learner@test.com");
        when(userRepository.findById(learnerId)).thenReturn(Optional.of(user));
    }

    private void stubCourseEnrollment(UUID userId, UUID courseId) {
        when(learnerEnrollmentRepository.findByUserIdAndCourseIdAndIsDeletedFalse(userId, courseId))
                .thenReturn(Optional.of(new LearnerEnrollment()));
    }

    // ── course-linked eligibility ──

    @Test
    void learner_enrolledInLinkedCourse_allowed() {
        stubStored(linkableResource(enrolledCourseId, null, Resource.ResourceVisibility.PUBLIC));
        stubCourseEnrollment(learnerId, enrolledCourseId);

        assertDoesNotThrow(() -> service.requireVisible(
                resourceId, institutionId, learnerId, "STUDENT"));
    }

    @Test
    void learner_notEnrolledInLinkedCourse_denied() {
        stubStored(linkableResource(otherCourseId, null, Resource.ResourceVisibility.PUBLIC));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.requireVisible(resourceId, institutionId, learnerId, "STUDENT"));

        assertTrue(ex.getMessage().contains("not enrolled"),
                "denial must name the missing course entitlement");
    }

    @Test
    void learner_enrolledInAnotherCourse_stillDeniedForThisResource() {
        // the resource needs an entitlement for otherCourseId; the learner's
        // enrollment elsewhere in the institution never gets queried
        stubStored(linkableResource(otherCourseId, null, Resource.ResourceVisibility.PUBLIC));
        when(learnerEnrollmentRepository.findByUserIdAndCourseIdAndIsDeletedFalse(learnerId, otherCourseId))
                .thenReturn(Optional.empty());

        assertThrows(SecurityException.class,
                () -> service.requireVisible(resourceId, institutionId, learnerId, "STUDENT"));
    }

    // ── lesson/class-linked eligibility ──

    @Test
    void learner_withoutClassMembership_deniedByExistingClassGuard() {
        stubStored(linkableResource(null, lessonId, Resource.ResourceVisibility.PUBLIC));
        Lesson lesson = Lesson.builder().classGroupId(classGroupId).title("Lesson").build();
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        stubLearnerEmail();
        doThrow(new SecurityException("Access denied to class"))
                .when(classAccessGuard).assertLearnerCanAccessClass("learner@test.com", classGroupId);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.requireVisible(resourceId, institutionId, learnerId, "STUDENT"));

        assertTrue(ex.getMessage().contains("class"));
    }

    @Test
    void learner_withClassMembership_allowed() {
        stubStored(linkableResource(null, lessonId, Resource.ResourceVisibility.PUBLIC));
        Lesson lesson = Lesson.builder().classGroupId(classGroupId).title("Lesson").build();
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        stubLearnerEmail();

        assertDoesNotThrow(() -> service.requireVisible(
                resourceId, institutionId, learnerId, "STUDENT"));
        verify(classAccessGuard).assertLearnerCanAccessClass("learner@test.com", classGroupId);
    }

    @Test
    void learner_lessonWithoutClassGroup_hasNoClassGate() {
        stubStored(linkableResource(null, lessonId, Resource.ResourceVisibility.PUBLIC));
        Lesson lesson = Lesson.builder().title("Course lesson").build();
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        assertDoesNotThrow(() -> service.requireVisible(
                resourceId, institutionId, learnerId, "STUDENT"));
        verify(classAccessGuard, never()).assertLearnerCanAccessClass(any(), any());
    }

    // ── roles and states the gate must NOT affect ──

    @Test
    void standaloneResource_remainsReadableForEveryVisibleRole() {
        stubStored(standaloneResource());

        assertDoesNotThrow(() -> service.requireVisible(
                resourceId, institutionId, learnerId, "STUDENT"));
        assertDoesNotThrow(() -> service.requireVisible(
                resourceId, institutionId, otherLearnerId, "OTHER_LEARNER"));
        verify(learnerEnrollmentRepository, never())
                .findByUserIdAndCourseIdAndIsDeletedFalse(any(), any());
    }

    @Test
    void teacher_isNeverSubjectToLearnerEligibility() {
        stubStored(linkableResource(otherCourseId, null, Resource.ResourceVisibility.PUBLIC));

        assertDoesNotThrow(() -> service.requireVisible(
                resourceId, institutionId, teacherId, "TEACHER"));
    }

    @Test
    void teacher_asOwner_readsOwnLinkedResourceWithoutEnrollment() {
        Resource owned = linkableResource(otherCourseId, null, Resource.ResourceVisibility.PRIVATE);
        owned.setUploadedBy(teacherId);
        stubStored(owned);

        assertDoesNotThrow(() -> service.requireVisible(
                resourceId, institutionId, teacherId, "TEACHER"));
    }

    // ── combined read predicate used by list/search/materials ──

    @Test
    void canRead_filtersIneligibleLearnersFromListings() {
        Resource courseResource = linkableResource(otherCourseId, null, Resource.ResourceVisibility.PUBLIC);

        assertFalse(service.canRead(courseResource, learnerId, "STUDENT"),
                "ineligible learner must be filtered out of listings");
        assertTrue(service.canRead(courseResource, teacherId, "TEACHER"),
                "teachers keep their existing read scope");
        assertTrue(service.canRead(standaloneResource(), learnerId, "STUDENT"),
                "standalone resources are unaffected by eligibility");
    }

    @Test
    void canRead_respectsVisibilityBeforeEligibility() {
        Resource hidden = linkableResource(enrolledCourseId, null, Resource.ResourceVisibility.DRAFT);

        assertFalse(service.canRead(hidden, otherLearnerId, "STUDENT"),
                "DRAFT stays hidden even when course enrollment would pass");
        // && short-circuits: visibility denial happens before any eligibility
        // lookup, so a DRAFT row never reaches the enrollment check
    }

    // ── annotations ride on the same gate ──

    @Test
    void annotations_deniedForLearnerNotEntitledToTheResource() {
        ResourceAnnotationService annotationService =
                new ResourceAnnotationService(null, resourceRepository, userRepository, service);
        stubStored(linkableResource(otherCourseId, null, Resource.ResourceVisibility.PUBLIC));

        assertThrows(SecurityException.class,
                () -> annotationService.listAnnotations(resourceId, institutionId, learnerId, "STUDENT"));
    }

    @Test
    void annotations_allowedForEntitledLearner() {
        tz.elmkusoma.learning.repository.ResourceAnnotationRepository annotationRepository =
                org.mockito.Mockito.mock(tz.elmkusoma.learning.repository.ResourceAnnotationRepository.class);
        ResourceAnnotationService annotationService =
                new ResourceAnnotationService(annotationRepository, resourceRepository, userRepository, service);
        stubStored(linkableResource(enrolledCourseId, null, Resource.ResourceVisibility.PUBLIC));
        stubCourseEnrollment(learnerId, enrolledCourseId);

        assertDoesNotThrow(() -> annotationService.listAnnotations(
                resourceId, institutionId, learnerId, "STUDENT"));
        verify(annotationRepository)
                .findByResourceIdAndIsDeletedFalseOrderByCreatedAtDesc(resourceId);
    }

    // ── analytics scope (owner-or-admin) ──

    @Test
    void analytics_ownerTeacher_allowed() {
        stubStored(standaloneResource()); // uploadedBy = teacherId

        assertDoesNotThrow(() -> service.requireAnalyticsAccess(
                resourceId, institutionId, teacherId, "TEACHER"));
    }

    @Test
    void analytics_otherTeacherOnVisibleResource_denied() {
        stubStored(standaloneResource()); // uploadedBy = teacherId, PUBLIC

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.requireAnalyticsAccess(
                        resourceId, institutionId, otherLearnerId, "TEACHER"));

        assertTrue(ex.getMessage().contains("analytics"));
    }

    @Test
    void analytics_institutionAdmin_allowed() {
        stubStored(standaloneResource());

        assertDoesNotThrow(() -> service.requireAnalyticsAccess(
                resourceId, institutionId, otherLearnerId, "INSTITUTION_ADMIN"));
    }

    @Test
    void analytics_crossInstitutionAdmin_deniedBeforeAnyRead() {
        stubStored(standaloneResource());

        assertThrows(Exception.class,
                () -> service.requireAnalyticsAccess(
                        resourceId, UUID.randomUUID(), otherLearnerId, "INSTITUTION_ADMIN"));
    }
}
