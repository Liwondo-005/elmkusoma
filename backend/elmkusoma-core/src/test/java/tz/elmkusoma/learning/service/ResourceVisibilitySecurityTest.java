package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.dto.ResourceRequest;
import tz.elmkusoma.learning.dto.ResourceResponse;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.ResourceTagRepository;
import tz.elmkusoma.learning.repository.ResourceTaggingRepository;
import tz.elmkusoma.learning.repository.StudentSavedResourceRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Visibility filtering and ownership enforcement of {@link ResourceService}.
 */
@ExtendWith(MockitoExtension.class)
class ResourceVisibilitySecurityTest {

    private static final List<String> PUBLIC_SCOPED =
            List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION");

    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private ResourceTagRepository tagRepository;
    @Mock
    private ResourceTaggingRepository taggingRepository;
    @Mock
    private StudentSavedResourceRepository savedResourceRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResourceService service;

    private UUID resourceId;
    private UUID institutionId;
    private UUID otherInstitutionId;
    private UUID uploaderId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        resourceId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
        uploaderId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
    }

    // ── getAllowedVisibilities ──

    @Test
    void getAllowedVisibilities_adminRolesSeeEveryVisibility() {
        for (String role : List.of("ADMIN", "INSTITUTION_ADMIN", "NATIONAL_ADMIN")) {
            List<String> allowed = ResourceService.getAllowedVisibilities(role);

            assertEquals(7, allowed.size(), "role " + role + " must see every visibility");
            assertTrue(allowed.contains("PRIVATE"));
            assertTrue(allowed.contains("DRAFT"));
        }
    }

    @Test
    void getAllowedVisibilities_teacherRolesAddPrivateAndDraft() {
        for (String role : List.of("TEACHER", "INSTRUCTOR", "LECTURER")) {
            List<String> allowed = ResourceService.getAllowedVisibilities(role);

            assertTrue(allowed.containsAll(PUBLIC_SCOPED), "role " + role + " must see scoped visibilities");
            assertTrue(allowed.contains("PRIVATE"));
            assertTrue(allowed.contains("DRAFT"));
            assertEquals(7, allowed.size());
        }
    }

    @Test
    void getAllowedVisibilities_otherRolesNeverSeePrivateOrDraft() {
        for (String role : List.of("STUDENT", "PARENT", "OTHER_LEARNER", "LEARNER", "unknown-role", "")) {
            List<String> allowed = ResourceService.getAllowedVisibilities(role);

            assertEquals(PUBLIC_SCOPED, allowed, "role " + role + " must be limited to public scoping");
            assertFalse(allowed.contains("PRIVATE"));
            assertFalse(allowed.contains("DRAFT"));
        }
        assertEquals(PUBLIC_SCOPED, ResourceService.getAllowedVisibilities(null),
                "a missing role must be limited to public scoping");
    }

    @Test
    void getAllowedVisibilities_everyRoleKeepsPublicScopedVisibilities() {
        for (String role : List.of("ADMIN", "NATIONAL_ADMIN", "INSTITUTION_ADMIN", "TEACHER", "STUDENT")) {
            assertTrue(ResourceService.getAllowedVisibilities(role).containsAll(PUBLIC_SCOPED));
        }
        assertTrue(ResourceService.getAllowedVisibilities(null).containsAll(PUBLIC_SCOPED));
    }

    @Test
    void isAdminRole_matchesAdminRolesOnly() {
        for (String role : List.of("ADMIN", "INSTITUTION_ADMIN", "NATIONAL_ADMIN")) {
            assertTrue(ResourceService.isAdminRole(role));
        }
        for (String role : List.of("TEACHER", "STUDENT", "PARENT", "LECTURER", "unknown", "")) {
            assertFalse(ResourceService.isAdminRole(role), "role " + role + " must not be an admin role");
        }
        assertFalse(ResourceService.isAdminRole(null));
    }

    // ── helpers ──

    private Resource resource(UUID uploadedBy, boolean deleted) {
        return Resource.builder()
                .id(resourceId)
                .institutionId(institutionId)
                .uploadedBy(uploadedBy)
                .title("Existing resource")
                .resourceType(Resource.ResourceType.PDF)
                .visibility(Resource.ResourceVisibility.PRIVATE)
                .sortOrder(0)
                .isDeleted(deleted)
                .createdBy("uploader@example.com")
                .build();
    }

    private void stubResource(Resource resource) {
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
    }

    private void stubSave() {
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ResourceRequest requestWithTitle(String title) {
        return ResourceRequest.builder()
                .title(title)
                .resourceType("PDF")
                .visibility("PUBLIC")
                .build();
    }

    // ── updateResource ──

    @Test
    void updateResource_nonOwnerIsRejected() {
        stubResource(resource(uploaderId, false));

        SecurityException ex = assertThrows(SecurityException.class, () -> service.updateResource(
                resourceId, requestWithTitle("Hijacked"), institutionId, otherUserId, "TEACHER"));

        assertTrue(ex.getMessage().contains("do not own"));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void updateResource_ownerMayUpdate() {
        Resource resource = resource(uploaderId, false);
        stubResource(resource);
        stubSave();

        service.updateResource(resourceId, requestWithTitle("Owned edit"), institutionId, uploaderId, "TEACHER");

        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceRepository).save(captor.capture());
        assertEquals("Owned edit", captor.getValue().getTitle());
    }

    @Test
    void updateResource_adminRolesMayUpdateAnotherUsersResource() {
        for (String role : List.of("ADMIN", "INSTITUTION_ADMIN", "NATIONAL_ADMIN")) {
            Resource resource = resource(uploaderId, false);
            stubResource(resource);
            stubSave();

            assertDoesNotThrow(() -> service.updateResource(
                            resourceId, requestWithTitle("Admin edit"), institutionId, otherUserId, role),
                    "role " + role + " must bypass resource ownership");

            reset(resourceRepository);
        }
    }

    @Test
    void updateResource_crossInstitutionIsRejected() {
        stubResource(resource(uploaderId, false));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateResource(
                resourceId, requestWithTitle("Cross org"), otherInstitutionId, uploaderId, "TEACHER"));

        assertEquals("Resource not found", ex.getMessage());
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void updateResource_deletedResourceIsRejected() {
        stubResource(resource(uploaderId, true));

        assertThrows(RuntimeException.class, () -> service.updateResource(
                resourceId, requestWithTitle("Zombie edit"), institutionId, uploaderId, "ADMIN"));
        verify(resourceRepository, never()).save(any());
    }

    // ── deleteResource ──

    @Test
    void deleteResource_nonOwnerIsRejected() {
        stubResource(resource(uploaderId, false));

        assertThrows(SecurityException.class, () -> service.deleteResource(
                resourceId, institutionId, otherUserId, "TEACHER"));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void deleteResource_ownerMaySoftDelete() {
        Resource resource = resource(uploaderId, false);
        stubResource(resource);
        stubSave();

        service.deleteResource(resourceId, institutionId, uploaderId, "TEACHER");

        assertTrue(resource.getIsDeleted());
        verify(resourceRepository).save(resource);
    }

    @Test
    void deleteResource_adminMayDeleteAnotherUsersResource() {
        Resource resource = resource(uploaderId, false);
        stubResource(resource);
        stubSave();

        service.deleteResource(resourceId, institutionId, otherUserId, "NATIONAL_ADMIN");

        assertTrue(resource.getIsDeleted());
    }

    @Test
    void deleteResource_crossInstitutionIsRejected() {
        stubResource(resource(uploaderId, false));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.deleteResource(
                resourceId, otherInstitutionId, uploaderId, "ADMIN"));

        assertEquals("Resource not found", ex.getMessage());
        verify(resourceRepository, never()).save(any());
    }

    // ── getResource access ──

    @Test
    void getResource_privateResourceDeniedToNonOwnerNonAdmin() {
        stubResource(resource(uploaderId, false));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getResource(
                resourceId, institutionId, otherUserId, "TEACHER"));

        assertEquals("Access denied to resource", ex.getMessage());
    }

    @Test
    void getResource_publicResourceIsVisibleToANonOwner() {
        // Regression: visibility compare previously used the enum against a
        // List<String> (always false) which failed closed on PUBLIC resources.
        Resource resource = resource(uploaderId, false);
        resource.setVisibility(Resource.ResourceVisibility.PUBLIC);
        stubResource(resource);

        ResourceResponse response = service.getResource(
                resourceId, institutionId, otherUserId, "STUDENT");

        assertNotNull(response);
        assertEquals("Existing resource", response.getTitle());
    }

    @Test
    void getResource_privateResourceIsNotVisibleToANonOwner() {
        Resource resource = resource(uploaderId, false);
        resource.setVisibility(Resource.ResourceVisibility.PRIVATE);
        stubResource(resource);

        SecurityException ex = assertThrows(SecurityException.class, () -> service.getResource(
                resourceId, institutionId, otherUserId, "STUDENT"));

        assertEquals("Access denied to resource", ex.getMessage());
    }

    @Test
    void getResource_uploaderMayReadOwnPrivateResource() {
        Resource resource = resource(uploaderId, false);
        stubResource(resource);

        ResourceResponse response = service.getResource(resourceId, institutionId, uploaderId, "TEACHER");

        assertNotNull(response);
        assertEquals("Existing resource", response.getTitle());
    }

    @Test
    void getResource_adminAndInstitutionAdminMayReadPrivateResourceOfAnotherUser() {
        for (String role : List.of("ADMIN", "INSTITUTION_ADMIN")) {
            Resource resource = resource(uploaderId, false);
            stubResource(resource);

            assertNotNull(service.getResource(resourceId, institutionId, otherUserId, role),
                    "role " + role + " must be able to read any resource");

            reset(resourceRepository);
        }
    }

    @Test
    void getResource_crossInstitutionIsRejected() {
        stubResource(resource(uploaderId, false));

        assertThrows(RuntimeException.class, () -> service.getResource(
                resourceId, otherInstitutionId, uploaderId, "ADMIN"));
    }

    // ── validateLessonAccess via createResource ──

    private ResourceRequest requestLinkedToLesson(UUID lessonId) {
        return ResourceRequest.builder()
                .lessonId(lessonId)
                .title("Linked resource")
                .resourceType("PDF")
                .visibility("PUBLIC")
                .build();
    }

    private Lesson lesson(UUID lessonInstitutionId, boolean deleted) {
        return Lesson.builder()
                .id(UUID.randomUUID())
                .institutionId(lessonInstitutionId)
                .subjectId(UUID.randomUUID())
                .classGroupId(UUID.randomUUID())
                .title("Lesson")
                .sortOrder(0)
                .isPublished(true)
                .status("PUBLISHED")
                .isDeleted(deleted)
                .build();
    }

    @Test
    void createResource_linkedToAnotherInstitutionsLessonIsRejected() {
        UUID lessonId = UUID.randomUUID();
        when(lessonRepository.findById(lessonId))
                .thenReturn(Optional.of(lesson(otherInstitutionId, false)));

        assertThrows(ResourceNotFoundException.class, () -> service.createResource(
                requestLinkedToLesson(lessonId), institutionId, uploaderId));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void createResource_linkedToMissingLessonIsRejected() {
        UUID lessonId = UUID.randomUUID();
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createResource(
                requestLinkedToLesson(lessonId), institutionId, uploaderId));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void createResource_linkedToDeletedLessonIsRejected() {
        UUID lessonId = UUID.randomUUID();
        when(lessonRepository.findById(lessonId))
                .thenReturn(Optional.of(lesson(institutionId, true)));

        assertThrows(ResourceNotFoundException.class, () -> service.createResource(
                requestLinkedToLesson(lessonId), institutionId, uploaderId));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void createResource_linkedToOwnLessonIsAllowed() {
        UUID lessonId = UUID.randomUUID();
        when(lessonRepository.findById(lessonId))
                .thenReturn(Optional.of(lesson(institutionId, false)));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceResponse response = service.createResource(
                requestLinkedToLesson(lessonId), institutionId, uploaderId);

        assertNotNull(response);
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void createResource_withoutLessonSkipsLessonValidation() {
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createResource(requestWithTitle("Standalone"), institutionId, uploaderId);

        verifyNoInteractions(lessonRepository);
        verify(resourceRepository).save(any(Resource.class));
    }

    // ── listResources uses the caller's visibility set ──

    @Test
    void listResources_passesStudentVisibilitySetToRepository() {
        when(resourceRepository.findByInstitutionIdAndVisibilities(institutionId, PUBLIC_SCOPED))
                .thenReturn(List.of());

        service.listResources(institutionId, otherUserId, "STUDENT", null, null, null, null, null, 0, 20);

        verify(resourceRepository).findByInstitutionIdAndVisibilities(institutionId, PUBLIC_SCOPED);
    }

    @Test
    void listResources_teacherVisibilitySetIncludesPrivateAndDraft() {
        List<String> teacherVisibilities =
                List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
        when(resourceRepository.findByInstitutionIdAndVisibilities(institutionId, teacherVisibilities))
                .thenReturn(List.of());

        service.listResources(institutionId, otherUserId, "TEACHER", null, null, null, null, null, 0, 20);

        verify(resourceRepository).findByInstitutionIdAndVisibilities(institutionId, teacherVisibilities);
        assertTrue(teacherVisibilities.contains("PRIVATE"));
        assertTrue(teacherVisibilities.contains("DRAFT"));
    }

    @Test
    void listResources_adminVisibilitySetIncludesPrivateAndDraft() {
        List<String> adminVisibilities =
                List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
        when(resourceRepository.findByInstitutionIdAndVisibilities(institutionId, adminVisibilities))
                .thenReturn(List.of());

        service.listResources(institutionId, otherUserId, "NATIONAL_ADMIN", null, null, null, null, null, 0, 20);

        verify(resourceRepository).findByInstitutionIdAndVisibilities(institutionId, adminVisibilities);
    }
}
