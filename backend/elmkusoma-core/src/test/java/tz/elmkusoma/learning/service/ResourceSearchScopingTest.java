package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.learning.domain.Resource;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Server-side list/search scoping (gap C): the lesson/module/course branches
 * previously trusted the context id alone, so a crafted id could list another
 * tenant's rows; and there was no server-side query filter at all.
 *
 * <p>Verifications:</p>
 * <ul>
 *   <li>every context branch applies the caller's institution predicate — a
 *       foreign institution's rows returned by the context query are dropped,</li>
 *   <li>the {@code q} query runs through the institution-scoped repository
 *       method (the institution id is threaded server-side) and re-applies
 *       visibility + eligibility like the unfiltered listing.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ResourceSearchScopingTest {

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

    private UUID ownInstitutionId;
    private UUID foreignInstitutionId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ownInstitutionId = UUID.randomUUID();
        foreignInstitutionId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ── helpers ──

    private Resource row(UUID institutionId, String title,
                         UUID lessonId, UUID moduleId, UUID courseId,
                         Resource.ResourceVisibility visibility) {
        return Resource.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .uploadedBy(UUID.randomUUID())
                .title(title)
                .resourceType(Resource.ResourceType.PDF)
                .storageUrl("https://storage.test/" + title + ".pdf")
                .lessonId(lessonId)
                .moduleId(moduleId)
                .courseId(courseId)
                .visibility(visibility)
                .sortOrder(0)
                .isDeleted(false)
                .build();
    }

    private static List<String> titles(List<ResourceResponse> responses) {
        return responses.stream().map(ResourceResponse::getTitle).collect(Collectors.toList());
    }

    // ── context branches apply the institution predicate ──

    @Test
    void lessonBranch_dropsRowsOfForeignInstitution() {
        UUID lessonId = UUID.randomUUID();
        when(resourceRepository.findVisibleByLessonId(eq(lessonId), any()))
                .thenReturn(List.of(
                        row(ownInstitutionId, "own-lesson-row", lessonId, null, null,
                                Resource.ResourceVisibility.PUBLIC),
                        row(foreignInstitutionId, "foreign-lesson-row", lessonId, null, null,
                                Resource.ResourceVisibility.PUBLIC)));

        List<ResourceResponse> result = service.listResources(
                ownInstitutionId, userId, "TEACHER",
                lessonId, null, null, null, null, 0, 20);

        assertEquals(List.of("own-lesson-row"), titles(result),
                "another tenant's rows must never leave the lesson branch");
    }

    @Test
    void moduleBranch_dropsRowsOfForeignInstitution() {
        UUID moduleId = UUID.randomUUID();
        when(resourceRepository.findVisibleByModuleId(eq(moduleId), any()))
                .thenReturn(List.of(
                        row(ownInstitutionId, "own-module-row", null, moduleId, null,
                                Resource.ResourceVisibility.PUBLIC),
                        row(foreignInstitutionId, "foreign-module-row", null, moduleId, null,
                                Resource.ResourceVisibility.PUBLIC)));

        List<ResourceResponse> result = service.listResources(
                ownInstitutionId, userId, "TEACHER",
                null, moduleId, null, null, null, 0, 20);

        assertEquals(List.of("own-module-row"), titles(result));
    }

    @Test
    void courseBranch_dropsRowsOfForeignInstitution() {
        UUID courseId = UUID.randomUUID();
        when(resourceRepository.findVisibleByCourseId(eq(courseId), any()))
                .thenReturn(List.of(
                        row(foreignInstitutionId, "foreign-course-row", null, null, courseId,
                                Resource.ResourceVisibility.PUBLIC),
                        row(ownInstitutionId, "own-course-row", null, null, courseId,
                                Resource.ResourceVisibility.PUBLIC)));

        List<ResourceResponse> result = service.listResources(
                ownInstitutionId, userId, "TEACHER",
                null, null, courseId, null, null, 0, 20);

        assertEquals(List.of("own-course-row"), titles(result));
    }

    // ── query search stays institution- and visibility-scoped ──

    @Test
    void querySearch_runsAgainstTheCallersInstitutionAndKeepsVisibilityRules() {
        when(resourceRepository.searchByInstitutionIdAndIsDeletedFalse(
                eq(ownInstitutionId), eq("algebra")))
                .thenReturn(List.of(
                        row(ownInstitutionId, "algebra basics", null, null, null,
                                Resource.ResourceVisibility.PUBLIC),
                        row(ownInstitutionId, "algebra answer key", null, null, null,
                                Resource.ResourceVisibility.DRAFT),
                        row(ownInstitutionId, "algebra class sheet", null, null, null,
                                Resource.ResourceVisibility.CLASS_ONLY)));

        List<ResourceResponse> result = service.listResources(
                ownInstitutionId, userId, "STUDENT",
                null, null, null, null, null, 0, 20, "algebra");

        List<String> visible = titles(result);
        assertTrue(visible.contains("algebra basics"), "public match must be returned");
        assertTrue(visible.contains("algebra class sheet"),
                "class-scoped visibility stays inside the learner's allowed set");
        assertTrue(!visible.contains("algebra answer key"),
                "DRAFT rows must be filtered out server-side");

        // the institution id is threaded into the repository query itself
        verify(resourceRepository)
                .searchByInstitutionIdAndIsDeletedFalse(eq(ownInstitutionId), eq("algebra"));
    }

    @Test
    void querySearch_caseInsensitiveOnTitle() {
        when(resourceRepository.searchByInstitutionIdAndIsDeletedFalse(
                eq(ownInstitutionId), eq("photos")))
                .thenReturn(List.of(
                        row(ownInstitutionId, "Photos of Kilimanjaro", null, null, null,
                                Resource.ResourceVisibility.PUBLIC)));

        // repository receives the caller's raw query; the in-memory title
        // match lowercases both sides
        List<ResourceResponse> result = service.listResources(
                ownInstitutionId, userId, "STUDENT",
                null, null, null, null, null, 0, 20, "photos");

        assertEquals(1, result.size());
        assertEquals("Photos of Kilimanjaro", result.get(0).getTitle());
    }

    @Test
    void querySearch_dropsHiddenRowsEvenWhenTheRepositoryReturnsThem() {
        // defense in depth: if the repository ever widens its match, visibility
        // + eligibility still apply before anything reaches the client
        when(resourceRepository.searchByInstitutionIdAndIsDeletedFalse(
                eq(ownInstitutionId), eq("secret")))
                .thenReturn(List.of(
                        row(ownInstitutionId, "secret draft", null, null, null,
                                Resource.ResourceVisibility.DRAFT),
                        row(ownInstitutionId, "secret plan", null, null, null,
                                Resource.ResourceVisibility.PRIVATE)));

        List<ResourceResponse> result = service.listResources(
                ownInstitutionId, userId, "STUDENT",
                null, null, null, null, null, 0, 20, "secret");

        assertEquals(List.of(), titles(result));
    }

    @Test
    void querySearch_neverRunsAgainstAnotherInstitution() {
        assertThrows(SecurityException.class, () -> service.listResources(
                null, userId, "TEACHER", null, null, null, null, null, 0, 20, "anything"));

        verify(resourceRepository, never())
                .searchByInstitutionIdAndIsDeletedFalse(any(), any());
    }

    // ── unfiltered listing keeps eligibility ──

    @Test
    void unfilteredListing_appliesTheCombinedReadPredicate() {
        when(resourceRepository.findByInstitutionIdAndVisibilities(eq(ownInstitutionId), any()))
                .thenReturn(List.of(
                        row(ownInstitutionId, "open row", null, null, null,
                                Resource.ResourceVisibility.PUBLIC),
                        row(ownInstitutionId, "teacher only row", null, null, null,
                                Resource.ResourceVisibility.PRIVATE)));

        List<ResourceResponse> learnerView = service.listResources(
                ownInstitutionId, userId, "STUDENT",
                null, null, null, null, null, 0, 20);

        assertEquals(List.of("open row"), titles(learnerView),
                "learners only receive rows inside their visibility set");
    }
}
