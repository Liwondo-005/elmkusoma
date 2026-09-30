package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.course.repository.CourseModuleRepository;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.domain.ResourceAnnotation;
import tz.elmkusoma.learning.domain.StudentSavedResource;
import tz.elmkusoma.learning.dto.ResourceRequest;
import tz.elmkusoma.learning.dto.ResourceResponse;
import tz.elmkusoma.learning.repository.*;
import tz.elmkusoma.liveclass.service.MediaProxyService;
import tz.elmkusoma.shared.repository.UserRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Teacher → student resource lifecycle: automatic ordering, URL/file type
 * rules, real metadata extraction, processing states, bookmark and annotation
 * access control, and the separated view/download capabilities.
 */
@ExtendWith(MockitoExtension.class)
class ResourceManagementFlowTest {

    private static final UUID INSTITUTION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_INSTITUTION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEACHER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID STUDENT = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID RESOURCE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock private ResourceRepository resourceRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private ResourceTagRepository tagRepository;
    @Mock private ResourceTaggingRepository taggingRepository;
    @Mock private StudentSavedResourceRepository savedResourceRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private MediaProxyService mediaProxyService;
    @Mock private AuditService auditService;
    @Mock private tz.elmkusoma.learning.repository.ResourceAnnotationRepository annotationRepository;
    @Mock private tz.elmkusoma.common.ClassAccessGuard classAccessGuard;
    @Mock private tz.elmkusoma.learner.repository.LearnerEnrollmentRepository learnerEnrollmentRepository;
    @Mock private tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository;
    @Mock private tz.elmkusoma.learner.service.NotificationService notificationService;

    private ResourceService service;
    private ResourceAnnotationService annotationService;

    @BeforeEach
    void setUp() {
        // Real extractor: metadata assertions run against actual encoded bytes.
        service = new ResourceService(resourceRepository, lessonRepository, tagRepository, taggingRepository,
                savedResourceRepository, userRepository, courseRepository, courseModuleRepository,
                mediaProxyService, new ResourceMetadataExtractor(), auditService,
                classAccessGuard, learnerEnrollmentRepository, institutionRepository, notificationService);
        annotationService = new ResourceAnnotationService(annotationRepository, resourceRepository, userRepository, service);
        lenient().when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── create: ordering & URL types ──────────────────────────────────────

    @Test
    void createResource_appendsNextSortOrderInContext() {
        Resource existing = baseResource(Resource.ResourceType.DOCUMENT);
        existing.setSortOrder(5);
        when(resourceRepository.findTopByInstitutionIdAndIsDeletedFalseOrderBySortOrderDesc(INSTITUTION))
                .thenReturn(Optional.of(existing));

        ResourceResponse response = service.createResource(
                linkOrFileRequest("PDF", "https://files.test/a.pdf", null), INSTITUTION, TEACHER, "TEACHER");

        assertEquals(6, response.getSortOrder(), "sort order must advance automatically, never stay manual");
    }

    @Test
    void createResource_startsAtZeroWhenContextIsEmpty() {
        ResourceResponse response = service.createResource(
                linkOrFileRequest("PDF", "https://files.test/a.pdf", null), INSTITUTION, TEACHER, "TEACHER");
        assertEquals(0, response.getSortOrder());
    }

    @Test
    void createLinkResource_isReadyAndNeedsNoStoredBytes() {
        ResourceResponse response = service.createResource(
                linkOrFileRequest("LINK", null, "https://example.org/reading"), INSTITUTION, TEACHER, "TEACHER");
        assertEquals("READY", response.getProcessingStatus());
        assertEquals("https://example.org/reading", response.getExternalUrl());
        assertNull(response.getStorageUrl());
    }

    @Test
    void createLinkResource_requiresExternalUrl() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createResource(linkOrFileRequest("EXTERNAL_LINK", null, null),
                        INSTITUTION, TEACHER, "TEACHER"));
        assertTrue(ex.getMessage().contains("externalUrl"));
    }

    @Test
    void createResource_rejectsNonHttpTargets() {
        for (String evil : List.of("javascript:alert(1)", "data:text/html,x", "file:///etc/passwd", "notaurl")) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.createResource(linkOrFileRequest("LINK", null, evil),
                            INSTITUTION, TEACHER, "TEACHER"),
                    "must reject: " + evil);
        }
    }

    @Test
    void createResource_rejectsUnknownTypeAndMissingFile() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createResource(linkOrFileRequest("YOUTUBE", null, null),
                        INSTITUTION, TEACHER, "TEACHER"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createResource(linkOrFileRequest("VIDEO", null, null),
                        INSTITUTION, TEACHER, "TEACHER"));
        assertTrue(ex.getMessage().contains("file"), "file types must demand stored bytes on the JSON path");
    }

    // ── create with file: processing & real metadata ──────────────────────

    @Test
    void createWithFile_storesRealBytesAndExtractsRealMetadata() throws Exception {
        MockMultipartFile png = pngFile("chart.png", 200, 120);
        when(mediaProxyService.uploadFile(any(), anyString()))
                .thenReturn(Map.of("id", 42L, "url", "http://localhost:8083/api/v1/media/local-content?key=inst%2Fx"));

        ResourceResponse response = service.createResourceWithFile(
                linkOrFileRequest("IMAGE", null, null), png, INSTITUTION, TEACHER, "TEACHER", "Bearer token");

        assertEquals("READY", response.getProcessingStatus());
        assertEquals(200, response.getWidth());
        assertEquals(120, response.getHeight());
        assertEquals("image/png", response.getMimeType());
        assertEquals(png.getSize(), response.getFileSize());
        assertNull(response.getPageCount(), "unknown values stay null, never zero");
        assertNull(response.getDurationSeconds());
        assertEquals("http://localhost:8083/api/v1/media/local-content?key=inst%2Fx", response.getStorageUrl());
        verify(mediaProxyService).uploadFile(any(), anyString());
    }

    @Test
    void createWithFile_marksFailedWhenStorageIsUnavailable() throws Exception {
        MockMultipartFile png = pngFile("chart.png", 32, 32);
        when(mediaProxyService.uploadFile(any(), anyString()))
                .thenThrow(new RuntimeException("Media service unavailable"));

        ResourceResponse response = service.createResourceWithFile(
                linkOrFileRequest("IMAGE", null, null), png, INSTITUTION, TEACHER, "TEACHER", "Bearer token");

        assertEquals("FAILED", response.getProcessingStatus(), "failure must never be reported as READY");
        assertNotNull(response.getProcessingError());
        assertFalse(response.getProcessingStatus().equals("READY"));
        verify(resourceRepository, atLeastOnce()).save(any(Resource.class));
    }

    @Test
    void createWithFile_rejectsSpoofedContentBeforeAnythingIsPersisted() {
        MockMultipartFile spoof;
        try {
            spoof = pngFile("movie.mp4", 64, 64);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.createResourceWithFile(linkOrFileRequest("VIDEO", null, null),
                        spoof, INSTITUTION, TEACHER, "TEACHER", "Bearer token"));
        verify(resourceRepository, never()).save(any(Resource.class));
    }

    // ── bookmarks never grant access ─────────────────────────────────────

    @Test
    void saveResource_rejectsForeignInstitutionTarget() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.saveResource(STUDENT, RESOURCE_ID, INSTITUTION, "STUDENT"));
        verify(savedResourceRepository, never()).save(any(StudentSavedResource.class));
    }

    @Test
    void saveResource_rejectsInvisibleTarget() {
        Resource othersPrivate = baseResource(Resource.ResourceType.DOCUMENT);
        othersPrivate.setUploadedBy(TEACHER);
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(othersPrivate));

        assertThrows(SecurityException.class,
                () -> service.saveResource(STUDENT, RESOURCE_ID, INSTITUTION, "STUDENT"));
        verify(savedResourceRepository, never()).save(any(StudentSavedResource.class));
    }

    @Test
    void getSavedResources_filtersInvisibleAndForeignRows() {
        UUID visibleId = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
        UUID invisibleId = UUID.fromString("dddddddd-0000-0000-0000-000000000002");
        UUID foreignId = UUID.fromString("dddddddd-0000-0000-0000-000000000003");

        Resource visible = baseResource(visibleId, Resource.ResourceType.DOCUMENT);
        visible.setVisibility(Resource.ResourceVisibility.PUBLIC);
        Resource invisible = baseResource(invisibleId, Resource.ResourceType.DOCUMENT); // PRIVATE by teacher
        invisible.setVisibility(Resource.ResourceVisibility.PRIVATE);
        Resource foreign = baseResource(foreignId, Resource.ResourceType.DOCUMENT);    // other institution
        foreign.setInstitutionId(OTHER_INSTITUTION);
        foreign.setVisibility(Resource.ResourceVisibility.PUBLIC);

        when(savedResourceRepository.findByStudentIdOrderBySavedAtDesc(STUDENT)).thenReturn(List.of(
                savedRow(visibleId), savedRow(invisibleId), savedRow(foreignId)));
        when(resourceRepository.findById(visibleId)).thenReturn(Optional.of(visible));
        when(resourceRepository.findById(invisibleId)).thenReturn(Optional.of(invisible));
        when(resourceRepository.findById(foreignId)).thenReturn(Optional.of(foreign));

        List<ResourceResponse> saved = service.getSavedResources(STUDENT, INSTITUTION, "STUDENT");

        assertEquals(1, saved.size(), "invisible/foreign bookmarks must be filtered out");
        assertEquals(visibleId, saved.get(0).getId(), "only the legitimately visible bookmark survives");
    }

    // ── tags follow ownership ─────────────────────────────────────────────

    @Test
    void removeTag_deniesNonOwnerInsideInstitution() {
        Resource notMine = baseResource(Resource.ResourceType.DOCUMENT);
        notMine.setUploadedBy(TEACHER);
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(notMine));

        assertThrows(SecurityException.class,
                () -> service.removeTag(RESOURCE_ID, UUID.randomUUID(), INSTITUTION, STUDENT, "STUDENT"));
        verify(taggingRepository, never()).deleteByResourceIdAndTagId(any(), any());
    }

    @Test
    void removeTag_deniesCrossInstitutionProbing() {
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.removeTag(RESOURCE_ID, UUID.randomUUID(), OTHER_INSTITUTION, STUDENT, "TEACHER"));
    }

    // ── authorized view vs download ───────────────────────────────────────

    @Test
    void contentUrl_forLinkResourceReturnsItsTarget() {
        Resource link = baseResource(Resource.ResourceType.LINK);
        link.setVisibility(Resource.ResourceVisibility.PUBLIC);
        link.setExternalUrl("https://example.org/doc");
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(link));

        String url = service.resolveContentUrl(RESOURCE_ID, INSTITUTION, STUDENT, "STUDENT", "Bearer t");
        assertEquals("https://example.org/doc", url);
    }

    @Test
    void contentUrl_neverServesAProcessingOrFailedResource() {
        Resource processing = baseResource(Resource.ResourceType.VIDEO);
        processing.setVisibility(Resource.ResourceVisibility.PUBLIC);
        processing.setProcessingStatus("PROCESSING");
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(processing));
        assertThrows(IllegalStateException.class,
                () -> service.resolveContentUrl(RESOURCE_ID, INSTITUTION, STUDENT, "STUDENT", "Bearer t"));

        processing.setProcessingStatus("FAILED");
        assertThrows(IllegalStateException.class,
                () -> service.resolveContentUrl(RESOURCE_ID, INSTITUTION, STUDENT, "STUDENT", "Bearer t"));
    }

    @Test
    void download_isDeniedWhenTheOwnerMarkedItNotDownloadable() {
        Resource locked = baseResource(Resource.ResourceType.PDF);
        locked.setVisibility(Resource.ResourceVisibility.PUBLIC);
        locked.setIsDownloadable(false);
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(locked));

        assertThrows(SecurityException.class,
                () -> service.resolveDownload(RESOURCE_ID, INSTITUTION, STUDENT, "STUDENT", "Bearer t"));
    }

    @Test
    void download_returnsRealBytesForAllowedCaller() {
        Resource open = baseResource(Resource.ResourceType.PDF);
        open.setVisibility(Resource.ResourceVisibility.PUBLIC);
        open.setIsDownloadable(true);
        open.setMediaId(7L);
        open.setMimeType("application/pdf");
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(open));
        when(mediaProxyService.getDownloadUrl("7", "Bearer t"))
                .thenReturn(Map.of("downloadUrl", "http://localhost:8083/api/v1/media/local-content?key=a&exp=1&sig=s"));
        when(mediaProxyService.fetchBytes(anyString())).thenReturn(new byte[]{1, 2, 3});

        ResourceService.ResourceDownload download =
                service.resolveDownload(RESOURCE_ID, INSTITUTION, STUDENT, "STUDENT", "Bearer t");

        assertEquals(3, download.bytes().length);
        assertEquals("application/pdf", download.contentType());
        assertTrue(download.fileName().endsWith(".pdf"));
    }

    // ── annotations: same gate as reads + private notes stay private ─────

    @Test
    void annotations_hiddenFromThoseWhoCannotSeeTheResource() {
        Resource document = baseResource(Resource.ResourceType.DOCUMENT);
        document.setVisibility(Resource.ResourceVisibility.PRIVATE);
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(document));

        assertThrows(SecurityException.class,
                () -> annotationService.listAnnotations(RESOURCE_ID, INSTITUTION, STUDENT, "STUDENT"));
    }

    @Test
    void annotations_privateNotesVisibleOnlyToTheirAuthor() {
        Resource document = baseResource(Resource.ResourceType.DOCUMENT);
        document.setVisibility(Resource.ResourceVisibility.PUBLIC);
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(document));
        when(annotationRepository.findByResourceIdAndIsDeletedFalseOrderByCreatedAtDesc(RESOURCE_ID))
                .thenReturn(List.of(
                        annotation(UUID.randomUUID(), STUDENT, true),   // my private note
                        annotation(UUID.randomUUID(), TEACHER, true),    // someone else's private note
                        annotation(UUID.randomUUID(), TEACHER, false))); // public highlight
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        List<?> visible = annotationService.listAnnotations(RESOURCE_ID, INSTITUTION, STUDENT, "STUDENT");

        assertEquals(2, visible.size(), "another user's private note must never be listed");
    }

    // ── fixtures ──────────────────────────────────────────────────────────

    private static Resource baseResource(Resource.ResourceType type) {
        return baseResource(RESOURCE_ID, type);
    }

    private static Resource baseResource(UUID id, Resource.ResourceType type) {
        return Resource.builder()
                .id(id)
                .institutionId(INSTITUTION)
                .uploadedBy(TEACHER)
                .title("Existing")
                .resourceType(type)
                .visibility(Resource.ResourceVisibility.DRAFT)
                .sortOrder(0)
                .isDownloadable(true)
                .isPreviewable(true)
                .isDeleted(false)
                .processingStatus("READY")
                .build();
    }

    private static ResourceRequest linkOrFileRequest(String type, String storageUrl, String externalUrl) {
        return ResourceRequest.builder()
                .title("Unit resource")
                .resourceType(type)
                .storageUrl(storageUrl)
                .externalUrl(externalUrl)
                .visibility("PUBLIC")
                .build();
    }

    private static StudentSavedResource savedRow(UUID resourceId) {
        return StudentSavedResource.builder().studentId(STUDENT).resourceId(resourceId).build();
    }

    private static ResourceAnnotation annotation(UUID id, UUID author, boolean isPrivate) {
        return ResourceAnnotation.builder()
                .id(id)
                .resourceId(RESOURCE_ID)
                .institutionId(INSTITUTION)
                .studentId(author)
                .content(isPrivate ? "private" : "public")
                .isPrivate(isPrivate)
                .build();
    }

    private static MockMultipartFile pngFile(String name, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", name, "image/png", out.toByteArray());
    }
}
