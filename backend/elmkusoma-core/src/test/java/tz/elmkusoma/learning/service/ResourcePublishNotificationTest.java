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
import tz.elmkusoma.learning.dto.ResourceRequest;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.ResourceTagRepository;
import tz.elmkusoma.learning.repository.ResourceTaggingRepository;
import tz.elmkusoma.learning.repository.StudentSavedResourceRepository;
import tz.elmkusoma.learner.domain.LearnerEnrollment;
import tz.elmkusoma.learner.repository.LearnerEnrollmentRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Publish notifications for resources (gap E): reuse of the existing
 * NotificationService with a server-computed audience, mirroring
 * notifyLessonPublished. The audience is derived from the resource's own
 * audience model — never from client input — and hidden content is never
 * announced.
 *
 * <p>Matrix:</p>
 * <ul>
 *   <li>create PUBLIC → one institution-wide notification (eligible students,
 *       publisher excluded),</li>
 *   <li>create PRIVATE / DRAFT → silent,</li>
 *   <li>DRAFT → PUBLIC transition → announced (announcement only on becoming
 *       learner-visible),</li>
 *   <li>visibility already PUBLIC, metadata-only edit → no re-announcement,</li>
 *   <li>PUBLIC → PRIVATE → silent (hiding never announces),</li>
 *   <li>course-linked → each enrolled learner, never institution-wide; no
 *       entitlements yet → silent,</li>
 *   <li>lesson-linked → class members; empty class → same institution-wide
 *       fallback the lesson publisher uses,</li>
 *   <li>a broken notification channel never fails the resource save.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ResourcePublishNotificationTest {

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
    private UUID teacherId;
    private UUID learnerOneId;
    private UUID learnerTwoId;
    private UUID courseId;
    private UUID lessonId;
    private UUID classGroupId;

    @BeforeEach
    void setUp() {
        resourceId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        learnerOneId = UUID.randomUUID();
        learnerTwoId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        lessonId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
    }

    // ── helpers ──

    private ResourceRequest createRequest(String visibility) {
        return ResourceRequest.builder()
                .title("Notification test resource")
                .resourceType("PDF")
                .storageUrl("https://storage.test/notification.pdf")
                .visibility(visibility)
                .build();
    }

    private ResourceRequest updateRequest(String visibility) {
        return ResourceRequest.builder()
                .visibility(visibility)
                .build();
    }

    private Resource stored(Resource.ResourceVisibility visibility,
                            UUID courseId, UUID lessonId) {
        return Resource.builder()
                .id(resourceId)
                .institutionId(institutionId)
                .uploadedBy(teacherId)
                .title("Notification test resource")
                .resourceType(Resource.ResourceType.PDF)
                .storageUrl("https://storage.test/notification.pdf")
                .courseId(courseId)
                .lessonId(lessonId)
                .visibility(visibility)
                .sortOrder(0)
                .isDeleted(false)
                .build();
    }

    private void stubSave() {
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubStored(Resource resource) {
        when(resourceRepository.findById(resourceId)).thenReturn(java.util.Optional.of(resource));
    }

    private static LearnerEnrollment enrollmentOf(UUID userId) {
        LearnerEnrollment enrollment = new LearnerEnrollment();
        enrollment.setUserId(userId);
        return enrollment;
    }

    private void verifyNeverAnnounced() {
        verify(notificationService, never())
                .notifyInstitutionStudentsExcluding(any(), any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    // ── create ──

    @Test
    void createPublicResource_announcesEligibleStudentsInstitutionWide() {
        stubSave();

        service.createResource(createRequest("PUBLIC"), institutionId, teacherId, "TEACHER");

        verify(notificationService, times(1)).notifyInstitutionStudentsExcluding(
                eq(institutionId), eq(teacherId), eq("New resource available"),
                contains("Notification test resource"),
                eq("RESOURCE_PUBLISHED"), eq("resource"), any());
    }

    @Test
    void createPrivateResource_neverNotifies() {
        stubSave();

        service.createResource(createRequest("PRIVATE"), institutionId, teacherId, "TEACHER");

        verifyNoInteractions(notificationService);
    }

    @Test
    void createDraftResource_neverNotifies() {
        stubSave();

        service.createResource(createRequest("DRAFT"), institutionId, teacherId, "TEACHER");

        verifyNoInteractions(notificationService);
    }

    // ── visibility transitions ──

    @Test
    void updateDraftToPublic_announcesOnce() {
        stubStored(stored(Resource.ResourceVisibility.DRAFT, null, null));
        stubSave();

        service.updateResource(resourceId, updateRequest("PUBLIC"), institutionId, teacherId, "TEACHER");

        verify(notificationService, times(1)).notifyInstitutionStudentsExcluding(
                eq(institutionId), eq(teacherId), any(), contains("Notification test resource"),
                eq("RESOURCE_PUBLISHED"), eq("resource"), any());
    }

    @Test
    void updateWhileAlreadyVisible_doesNotReAnnounce() {
        stubStored(stored(Resource.ResourceVisibility.PUBLIC, null, null));
        stubSave();

        service.updateResource(resourceId, updateRequest(null), institutionId, teacherId, "TEACHER");

        verifyNoInteractions(notificationService);
    }

    @Test
    void updateToPrivate_neverNotifies() {
        stubStored(stored(Resource.ResourceVisibility.PUBLIC, null, null));
        stubSave();

        service.updateResource(resourceId, updateRequest("PRIVATE"), institutionId, teacherId, "TEACHER");

        verifyNoInteractions(notificationService);
    }

    // ── audience resolution: course-linked (strict enrollment) ──

    @Test
    void courseLinkedPublish_notifiesEachEnrolledLearnerAndNeverThePublisher() {
        stubStored(stored(Resource.ResourceVisibility.DRAFT, courseId, null));
        stubSave();
        when(learnerEnrollmentRepository.findByCourseIdAndIsDeletedFalse(courseId))
                .thenReturn(List.of(
                        enrollmentOf(learnerOneId),
                        enrollmentOf(learnerTwoId),
                        enrollmentOf(teacherId)));

        service.updateResource(resourceId, updateRequest("PUBLIC"), institutionId, teacherId, "TEACHER");

        verify(notificationService).notifyUser(eq(learnerOneId), any(), contains("Notification test resource"),
                eq("RESOURCE_PUBLISHED"), eq("resource"), any());
        verify(notificationService).notifyUser(eq(learnerTwoId), any(), contains("Notification test resource"),
                eq("RESOURCE_PUBLISHED"), eq("resource"), any());
        verify(notificationService, never()).notifyUser(eq(teacherId), any(), any(), any(), any(), any());
        verify(notificationService, never())
                .notifyInstitutionStudentsExcluding(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void courseLinkedPublish_withoutAnyEnrollment_staysSilent() {
        stubStored(stored(Resource.ResourceVisibility.DRAFT, courseId, null));
        stubSave();
        when(learnerEnrollmentRepository.findByCourseIdAndIsDeletedFalse(courseId))
                .thenReturn(List.of());

        service.updateResource(resourceId, updateRequest("PUBLIC"), institutionId, teacherId, "TEACHER");

        verifyNoInteractions(notificationService);
    }

    // ── audience resolution: lesson/class-linked ──

    @Test
    void lessonLinkedPublish_notifiesClassMembers() {
        stubStored(stored(Resource.ResourceVisibility.DRAFT, null, lessonId));
        stubSave();
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(
                Lesson.builder().classGroupId(classGroupId).title("Lesson").build()));
        when(classAccessGuard.resolveClassStudentUserIds(classGroupId))
                .thenReturn(List.of(learnerOneId));

        service.updateResource(resourceId, updateRequest("PUBLIC"), institutionId, teacherId, "TEACHER");

        verify(notificationService).notifyUser(eq(learnerOneId), any(), contains("Notification test resource"),
                eq("RESOURCE_PUBLISHED"), eq("resource"), any());
        verify(notificationService, never())
                .notifyInstitutionStudentsExcluding(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void lessonLinkedPublish_withoutClassAudience_fallsBackToInstitutionStudents() {
        stubStored(stored(Resource.ResourceVisibility.DRAFT, null, lessonId));
        stubSave();
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(
                Lesson.builder().classGroupId(classGroupId).title("Lesson").build()));
        when(classAccessGuard.resolveClassStudentUserIds(classGroupId)).thenReturn(List.of());

        service.updateResource(resourceId, updateRequest("PUBLIC"), institutionId, teacherId, "TEACHER");

        verify(notificationService, times(1)).notifyInstitutionStudentsExcluding(
                eq(institutionId), eq(teacherId), any(), any(),
                eq("RESOURCE_PUBLISHED"), eq("resource"), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void lessonLinkedPublish_missingLessonRecord_fallsBackToInstitutionStudents() {
        stubStored(stored(Resource.ResourceVisibility.DRAFT, null, lessonId));
        stubSave();
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.empty());

        service.updateResource(resourceId, updateRequest("PUBLIC"), institutionId, teacherId, "TEACHER");

        verify(notificationService, times(1)).notifyInstitutionStudentsExcluding(
                eq(institutionId), eq(teacherId), any(), any(),
                eq("RESOURCE_PUBLISHED"), eq("resource"), any());
    }

    // ── resilience ──

    @Test
    void brokenNotificationChannel_neverBlocksTheSave() {
        stubSave();
        doThrow(new RuntimeException("broker down"))
                .when(notificationService)
                .notifyInstitutionStudentsExcluding(any(), any(), any(), any(), any(), any(), any());

        assertDoesNotThrow(() ->
                service.createResource(createRequest("PUBLIC"), institutionId, teacherId, "TEACHER"));

        verify(resourceRepository).save(any(Resource.class));
    }

    // ── pre-existing guard: hidden content stays silent even mid-failure ──

    @Test
    void neverAnnounced_afterFailedOrHiddenUpdates() {
        stubStored(stored(Resource.ResourceVisibility.DRAFT, null, null));
        stubSave();

        // still DRAFT after the update (e.g. metadata-only edit)
        service.updateResource(resourceId, updateRequest(null), institutionId, teacherId, "TEACHER");

        verifyNeverAnnounced();
    }
}
