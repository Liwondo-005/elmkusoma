package tz.elmkusoma.learning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.repository.CourseModuleRepository;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.domain.ResourceTag;
import tz.elmkusoma.learning.domain.ResourceTagging;
import tz.elmkusoma.learning.domain.StudentSavedResource;
import tz.elmkusoma.learning.dto.ResourceRequest;
import tz.elmkusoma.learning.dto.ResourceResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.ResourceTagRepository;
import tz.elmkusoma.learning.repository.ResourceTaggingRepository;
import tz.elmkusoma.learning.repository.StudentSavedResourceRepository;
import tz.elmkusoma.liveclass.service.MediaProxyService;

import java.util.Map;
import java.util.Objects;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final LessonRepository lessonRepository;
    private final ResourceTagRepository tagRepository;
    private final ResourceTaggingRepository taggingRepository;
    private final StudentSavedResourceRepository savedResourceRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final MediaProxyService mediaProxyService;
    private final ResourceMetadataExtractor metadataExtractor;
    private final AuditService auditService;

    private static final List<String> STUDENT_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION");
    private static final List<String> TEACHER_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
    private static final List<String> ADMIN_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");

    // ── Create Resource (JSON path: URL-based types and pre-stored files) ──
    @Transactional
    public ResourceResponse createResource(ResourceRequest request, UUID institutionId, UUID userId) {
        return createResource(request, institutionId, userId, "TEACHER");
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request, UUID institutionId, UUID userId, String userRole) {
        log.info("Creating resource: {} for institution: {}", request.getTitle(), institutionId);

        Resource.ResourceType type = resolveType(request.getResourceType());
        validateContext(request, institutionId);

        Resource resource = buildResource(request, institutionId, userId, type);
        applyRequestBodyTargets(resource, request);

        if (type == Resource.ResourceType.EXTERNAL_LINK || type == Resource.ResourceType.LINK) {
            // URL resources: an external target is mandatory, stored bytes are not.
            resource.setExternalUrl(requireHttpUrl(request.getExternalUrl(), "externalUrl"));
            resource.setStorageUrl(null);
            resource.setProcessingStatus("READY");
        } else {
            // File-based resources created from JSON must already point at stored
            // bytes (the multipart path stores them itself).
            if (isBlank(resource.getStorageUrl()) && isBlank(request.getStorageUrl())) {
                throw new IllegalArgumentException("A file must be uploaded for resource type " + type);
            }
            resource.setStorageUrl(normalizeStoredUrl(request.getStorageUrl()));
            resource.setProcessingStatus("READY");
        }
        if (request.getSortOrder() == null) {
            resource.setSortOrder(nextSortOrder(resource));
        }

        resource = resourceRepository.save(resource);
        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            applyTagNames(resource, request);
            resource = resourceRepository.save(resource);
        }
        log.info("Created resource: {} for institution: {}", resource.getId(), institutionId);
        auditResource(institutionId, userId, userRole, resource, AuditLog.AuditAction.CREATE, null);
        return mapToResponse(resource);
    }

    // ── Create Resource with a real file (validate → store → process → metadata → sort → save) ──
    @Transactional
    public ResourceResponse createResourceWithFile(ResourceRequest request, MultipartFile file,
                                                   UUID institutionId, UUID userId, String userRole,
                                                   String bearerToken) {
        Resource.ResourceType type = resolveType(request.getResourceType());
        if (type == Resource.ResourceType.EXTERNAL_LINK || type == Resource.ResourceType.LINK) {
            throw new IllegalArgumentException("Link resources are created from a URL, not a file");
        }
        validateContext(request, institutionId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A file is required for resource type " + type);
        }

        // VALIDATE: the real bytes must match the selected type before anything
        // is stored or persisted.
        ResourceMetadataExtractor.DetectedFile detected;
        try {
            detected = metadataExtractor.detectAndValidate(file, type.name());
        } catch (IOException e) {
            throw new IllegalArgumentException("The uploaded file could not be read");
        }

        Resource resource = buildResource(request, institutionId, userId, type);
        applyRequestBodyTargets(resource, request);
        resource.setMimeType(null);
        resource.setFileSize(file.getSize());
        resource.setProcessingStatus("PROCESSING");
        if (request.getSortOrder() == null) {
            resource.setSortOrder(nextSortOrder(resource));
        }
        resource = resourceRepository.save(resource);

        // STORE + PROCESS + METADATA: bytes go to the existing media service;
        // metadata is extracted from the actual file.
        try {
            Map<String, Object> stored = mediaProxyService.uploadFile(file, bearerToken);
            Object mediaId = stored != null ? stored.get("id") : null;
            if (mediaId instanceof Number n) {
                resource.setMediaId(n.longValue());
            }
            String storedUrl = stored != null && stored.get("url") != null
                    ? String.valueOf(stored.get("url")) : null;
            if (!isBlank(storedUrl)) {
                resource.setStorageUrl(storedUrl);
            }

            ResourceMetadataExtractor.ExtractedMetadata meta = metadataExtractor.extractMetadata(file, detected);
            resource.setMimeType(meta.mimeType());
            resource.setFileSize(meta.fileSize());
            resource.setDurationSeconds(meta.durationSeconds());
            resource.setPageCount(meta.pageCount());
            resource.setWidth(meta.width());
            resource.setHeight(meta.height());
            resource.setProcessingStatus("READY");
            resource.setProcessingError(null);
        } catch (Exception e) {
            log.error("Resource file processing failed for {}: {}", resource.getId(), e.getMessage());
            resource.setProcessingStatus("FAILED");
            resource.setProcessingError(truncateError(e.getMessage()));
        }

        Resource saved = resourceRepository.save(resource);
        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            applyTagNames(saved, request);
            saved = resourceRepository.save(saved);
        }
        auditResource(institutionId, userId, userRole, saved, AuditLog.AuditAction.CREATE, null);
        return mapToResponse(saved);
    }

    // ── Shared builders / validation ──

    /** Attaches tag names once the resource has a generated id. */
    private void applyTagNames(Resource resource, ResourceRequest request) {
        if (request.getTagNames() == null) {
            return;
        }
        List<ResourceTag> tags = request.getTagNames().stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> tagRepository.findByName(name.trim())
                        .orElseGet(() -> tagRepository.save(ResourceTag.builder()
                                .name(name.trim())
                                .color("#2563EB")
                                .isSystem(false)
                                .build())))
                .collect(Collectors.toList());
        resource.setTaggings(tags.stream()
                .map(tag -> ResourceTagging.builder()
                        .resourceId(resource.getId())
                        .tagId(tag.getId())
                        .build())
                .collect(Collectors.toList()));
    }

    private Resource buildResource(ResourceRequest request, UUID institutionId, UUID userId,
                                   Resource.ResourceType type) {
        return Resource.builder()
                .institutionId(institutionId)
                .lessonId(request.getLessonId())
                .moduleId(request.getModuleId())
                .courseId(request.getCourseId())
                .uploadedBy(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .resourceType(type)
                .visibility(request.getVisibility() != null
                        ? Resource.ResourceVisibility.valueOf(request.getVisibility())
                        : Resource.ResourceVisibility.DRAFT)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isDownloadable(request.getIsDownloadable() != null ? request.getIsDownloadable() : true)
                .isPreviewable(request.getIsPreviewable() != null ? request.getIsPreviewable() : false)
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .thumbnailUrl(request.getThumbnailUrl())
                .build();
    }

    /** Applies the caller-provided URL/storage fields of a JSON request. */
    private void applyRequestBodyTargets(Resource resource, ResourceRequest request) {
        if (request.getExternalUrl() != null) resource.setExternalUrl(request.getExternalUrl().trim());
        if (request.getStorageObjectKey() != null) resource.setStorageObjectKey(request.getStorageObjectKey());
        if (request.getStorageBucket() != null) resource.setStorageBucket(request.getStorageBucket());
        if (request.getMimeType() != null) resource.setMimeType(request.getMimeType());
        if (request.getFileSize() != null) resource.setFileSize(request.getFileSize());
        if (request.getDurationSeconds() != null) resource.setDurationSeconds(request.getDurationSeconds());
        if (request.getPageCount() != null) resource.setPageCount(request.getPageCount());
        if (request.getWidth() != null) resource.setWidth(request.getWidth());
        if (request.getHeight() != null) resource.setHeight(request.getHeight());
    }

    /** Lesson/module/course links must exist inside the caller's institution. */
    private void validateContext(ResourceRequest request, UUID institutionId) {
        if (request.getLessonId() != null) {
            validateLessonAccess(request.getLessonId(), institutionId);
        }
        if (request.getCourseId() != null) {
            courseRepository.findById(request.getCourseId())
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .filter(c -> institutionId.equals(c.getInstitutionId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
        }
        if (request.getModuleId() != null) {
            courseModuleRepository.findById(request.getModuleId())
                    .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                    .filter(m -> institutionId.equals(m.getInstitutionId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Module", "id", request.getModuleId()));
        }
    }

    private static Resource.ResourceType resolveType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("resourceType is required");
        }
        try {
            return Resource.ResourceType.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported resource type: " + raw);
        }
    }

    /** Only http(s) targets may be stored — rejects javascript:, data:, file: etc. */
    static String requireHttpUrl(String url, String field) {
        if (isBlank(url)) {
            throw new IllegalArgumentException(field + " is required");
        }
        String trimmed = url.trim();
        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException(field + " is too long");
        }
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException(field + " must use the http or https scheme");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException(field + " is not a valid URL");
            }
            return trimmed;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(field + " is not a valid URL");
        }
    }

    /** Storage URLs come from our own storage layer: absolute http(s) or app-relative. */
    static String normalizeStoredUrl(String url) {
        if (isBlank(url)) return null;
        String trimmed = url.trim();
        if (trimmed.startsWith("/")) return trimmed;
        return requireHttpUrl(trimmed, "storageUrl");
    }

    /**
     * Next automatic sort order in the resource's real context (lesson → module
     * → course → institution).  Teachers never enter this by hand.
     */
    private int nextSortOrder(Resource resource) {
        Integer max;
        if (resource.getLessonId() != null) {
            max = resourceRepository.findTopByLessonIdAndIsDeletedFalseOrderBySortOrderDesc(resource.getLessonId())
                    .map(Resource::getSortOrder).orElse(null);
        } else if (resource.getModuleId() != null) {
            max = resourceRepository.findTopByModuleIdAndIsDeletedFalseOrderBySortOrderDesc(resource.getModuleId())
                    .map(Resource::getSortOrder).orElse(null);
        } else if (resource.getCourseId() != null) {
            max = resourceRepository.findTopByCourseIdAndIsDeletedFalseOrderBySortOrderDesc(resource.getCourseId())
                    .map(Resource::getSortOrder).orElse(null);
        } else {
            max = resourceRepository.findTopByInstitutionIdAndIsDeletedFalseOrderBySortOrderDesc(resource.getInstitutionId())
                    .map(Resource::getSortOrder).orElse(null);
        }
        return max == null ? 0 : max + 1;
    }

    /** Null-safe audit write on the existing audit infrastructure. */
    private void auditResource(UUID institutionId, UUID userId, String userRole, Resource resource,
                               AuditLog.AuditAction action, Map<String, Object> newValues) {
        try {
            String email = userId == null ? null
                    : userRepository.findById(userId).map(User::getEmail).orElse(null);
            Map<String, Object> values = newValues != null ? newValues : Map.of(
                    "resourceType", resource.getResourceType().name(),
                    "visibility", resource.getVisibility().name(),
                    "processingStatus", resource.getProcessingStatus());
            auditService.recordAuditLog(institutionId, userId, email,
                    userRole != null ? userRole : "TEACHER",
                    "resource", resource.getId(), resource.getTitle(), action, null, values);
        } catch (Exception e) {
            log.warn("Resource audit write failed for {}: {}", resource.getId(), e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String truncateError(String message) {
        if (message == null) return "Processing failed";
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }

    // ── Get Resource ──
    @Transactional(readOnly = true)
    public ResourceResponse getResource(UUID resourceId, UUID institutionId, UUID userId, String userRole) {
        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> r.getInstitutionId().equals(institutionId))
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        if (!canAccessResource(resource, userId, userRole)) {
            throw new SecurityException("Access denied to resource");
        }

        return mapToResponse(resource);
    }

    // ── List Resources ──
    @Transactional(readOnly = true)
    public List<ResourceResponse> listResources(UUID institutionId, UUID userId, String userRole,
                                                UUID lessonId, UUID moduleId, UUID courseId,
                                                String resourceType, String visibility,
                                                int page, int size) {
        List<String> allowedVisibilities = getAllowedVisibilities(userRole);

        if (lessonId != null) {
            return resourceRepository.findVisibleByLessonId(lessonId, allowedVisibilities)
                    .stream()
                    .filter(r -> canSeeResource(r, userId, userRole))
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        if (moduleId != null) {
            return resourceRepository.findVisibleByModuleId(moduleId, allowedVisibilities)
                    .stream()
                    .filter(r -> canSeeResource(r, userId, userRole))
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        if (courseId != null) {
            return resourceRepository.findVisibleByCourseId(courseId, allowedVisibilities)
                    .stream()
                    .filter(r -> canSeeResource(r, userId, userRole))
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        List<Resource> resources = resourceRepository.findByInstitutionIdAndVisibilities(institutionId, allowedVisibilities);
        return resources.stream()
                .filter(r -> canSeeResource(r, userId, userRole))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Update Resource ──
    @Transactional
    public ResourceResponse updateResource(UUID resourceId, ResourceRequest request, UUID institutionId, UUID userId, String userRole) {
        Resource resource = loadOwnedResource(resourceId, institutionId, userId, userRole);

        if (request.getTitle() != null) resource.setTitle(request.getTitle());
        if (request.getDescription() != null) resource.setDescription(request.getDescription());
        if (request.getVisibility() != null) resource.setVisibility(Resource.ResourceVisibility.valueOf(request.getVisibility()));
        if (request.getSortOrder() != null) resource.setSortOrder(request.getSortOrder());
        if (request.getIsDownloadable() != null) resource.setIsDownloadable(request.getIsDownloadable());
        if (request.getIsPreviewable() != null) resource.setIsPreviewable(request.getIsPreviewable());
        if (request.getTags() != null) resource.setTags(request.getTags());
        if (request.getMetadata() != null) resource.setMetadata(request.getMetadata());
        if (request.getLessonId() != null) {
            validateLessonAccess(request.getLessonId(), institutionId);
            resource.setLessonId(request.getLessonId());
        }
        if (request.getStorageUrl() != null) {
            resource.setStorageUrl(request.getStorageUrl());
        }
        applyRequestBodyTargets(resource, request);

        if (request.getResourceType() != null) {
            Resource.ResourceType newType = resolveType(request.getResourceType());
            if (newType != resource.getResourceType()) {
                applyTypeSwitch(resource, newType, request);
            }
        } else if (resource.isExternalLink() && isBlank(resource.getExternalUrl())) {
            throw new IllegalArgumentException("externalUrl is required for resource type " + resource.getResourceType());
        } else if (resource.isFileBased() && isBlank(resource.getStorageUrl())) {
            throw new IllegalArgumentException("A file must be uploaded for resource type " + resource.getResourceType());
        }
        // Re-validate the stored target whenever it is present (legacy rows included).
        if (!isBlank(resource.getExternalUrl())) {
            resource.setExternalUrl(requireHttpUrl(resource.getExternalUrl(), "externalUrl"));
        }
        if (!isBlank(resource.getStorageUrl())) {
            resource.setStorageUrl(normalizeStoredUrl(resource.getStorageUrl()));
        }

        Resource savedResource = resourceRepository.save(resource);
        auditResource(institutionId, userId, userRole, savedResource, AuditLog.AuditAction.UPDATE, null);
        return mapToResponse(savedResource);
    }

    /**
     * Type switches must never leave stale content or metadata behind:
     * file → link requires a valid URL target; link → file and file → file
     * require a newly uploaded file (multipart update).
     */
    private void applyTypeSwitch(Resource resource, Resource.ResourceType newType, ResourceRequest request) {
        if (newType == Resource.ResourceType.EXTERNAL_LINK || newType == Resource.ResourceType.LINK) {
            if (isBlank(resource.getExternalUrl())) {
                throw new IllegalArgumentException("externalUrl is required for resource type " + newType);
            }
        } else {
            if (isBlank(request.getStorageUrl())) {
                throw new IllegalArgumentException(
                        "Changing to resource type " + newType + " requires uploading a new file");
            }
            resource.setStorageUrl(request.getStorageUrl());
            // New stored target: previous file metadata no longer applies.
            resource.setMediaId(null);
            resource.setDurationSeconds(request.getDurationSeconds());
            resource.setPageCount(request.getPageCount());
            resource.setWidth(request.getWidth());
            resource.setHeight(request.getHeight());
            if (request.getMimeType() == null) resource.setMimeType(null);
            if (request.getFileSize() == null) resource.setFileSize(null);
            resource.setProcessingStatus("READY");
            resource.setProcessingError(null);
        }
        resource.setResourceType(newType);
    }

    /** Update that replaces the stored file: upload → process → metadata → save. */
    @Transactional
    public ResourceResponse updateResourceWithFile(UUID resourceId, ResourceRequest request, MultipartFile file,
                                                   UUID institutionId, UUID userId, String userRole,
                                                   String bearerToken) {
        Resource resource = loadOwnedResource(resourceId, institutionId, userId, userRole);
        Resource.ResourceType type = resolveType(request.getResourceType() != null
                ? request.getResourceType() : resource.getResourceType().name());
        if (type == Resource.ResourceType.EXTERNAL_LINK || type == Resource.ResourceType.LINK) {
            throw new IllegalArgumentException("Link resources are created from a URL, not a file");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A file is required for resource type " + type);
        }

        ResourceMetadataExtractor.DetectedFile detected;
        try {
            detected = metadataExtractor.detectAndValidate(file, type.name());
        } catch (IOException e) {
            throw new IllegalArgumentException("The uploaded file could not be read");
        }

        Long previousMediaId = resource.getMediaId();
        resource.setResourceType(type);
        if (request.getTitle() != null) resource.setTitle(request.getTitle());
        if (request.getDescription() != null) resource.setDescription(request.getDescription());
        if (request.getVisibility() != null) resource.setVisibility(Resource.ResourceVisibility.valueOf(request.getVisibility()));
        if (request.getIsDownloadable() != null) resource.setIsDownloadable(request.getIsDownloadable());
        if (request.getIsPreviewable() != null) resource.setIsPreviewable(request.getIsPreviewable());
        if (request.getTags() != null) resource.setTags(request.getTags());
        if (request.getLessonId() != null) {
            validateLessonAccess(request.getLessonId(), institutionId);
            resource.setLessonId(request.getLessonId());
        }
        resource.setMediaId(null);
        resource.setMimeType(null);
        resource.setFileSize(file.getSize());
        resource.setDurationSeconds(null);
        resource.setPageCount(null);
        resource.setWidth(null);
        resource.setHeight(null);
        resource.setProcessingStatus("PROCESSING");
        resource.setProcessingError(null);
        resource = resourceRepository.save(resource);

        try {
            Map<String, Object> stored = mediaProxyService.uploadFile(file, bearerToken);
            Object mediaId = stored != null ? stored.get("id") : null;
            if (mediaId instanceof Number n) {
                resource.setMediaId(n.longValue());
            }
            String storedUrl = stored != null && stored.get("url") != null
                    ? String.valueOf(stored.get("url")) : null;
            if (!isBlank(storedUrl)) {
                resource.setStorageUrl(storedUrl);
            }

            ResourceMetadataExtractor.ExtractedMetadata meta = metadataExtractor.extractMetadata(file, detected);
            resource.setMimeType(meta.mimeType());
            resource.setFileSize(meta.fileSize());
            resource.setDurationSeconds(meta.durationSeconds());
            resource.setPageCount(meta.pageCount());
            resource.setWidth(meta.width());
            resource.setHeight(meta.height());
            resource.setProcessingStatus("READY");
            resource.setProcessingError(null);
        } catch (Exception e) {
            log.error("Resource file replacement failed for {}: {}", resource.getId(), e.getMessage());
            resource.setProcessingStatus("FAILED");
            resource.setProcessingError(truncateError(e.getMessage()));
        }

        Resource saved = resourceRepository.save(resource);
        auditResource(institutionId, userId, userRole, saved, AuditLog.AuditAction.UPDATE, null);

        // Best-effort cleanup of the replaced object in the existing media store.
        if (previousMediaId != null && !previousMediaId.equals(saved.getMediaId())) {
            try {
                mediaProxyService.deleteMedia(previousMediaId, bearerToken);
            } catch (Exception e) {
                log.warn("Old media object {} could not be removed: {}", previousMediaId, e.getMessage());
            }
        }
        return mapToResponse(saved);
    }

    /** Institution + ownership + state gate shared by every mutating entry point. */
    private Resource loadOwnedResource(UUID resourceId, UUID institutionId, UUID userId, String userRole) {
        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> r.getInstitutionId().equals(institutionId))
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        if ((resource.getUploadedBy() == null || !resource.getUploadedBy().equals(userId)) && !isAdminRole(userRole)) {
            throw new SecurityException("You do not own this resource");
        }
        return resource;
    }

    // ── Delete Resource ──
    @Transactional
    public void deleteResource(UUID resourceId, UUID institutionId, UUID userId, String userRole) {
        Resource resource = loadOwnedResource(resourceId, institutionId, userId, userRole);

        resource.setIsDeleted(true);
        resourceRepository.save(resource);
        log.info("Deleted resource: {}", resourceId);
        auditResource(institutionId, userId, userRole, resource, AuditLog.AuditAction.DELETE, null);
    }

    // ── Resource Tags ──
    @Transactional
    public ResourceResponse addTag(UUID resourceId, String tagName, UUID institutionId) {
        return addTag(resourceId, tagName, institutionId, null, null);
    }

    @Transactional
    public ResourceResponse addTag(UUID resourceId, String tagName, UUID institutionId, UUID userId, String userRole) {
        Resource resource = loadManagedResource(resourceId, institutionId, userId, userRole);

        ResourceTag tag = tagRepository.findByName(tagName)
                .orElseGet(() -> tagRepository.save(ResourceTag.builder()
                        .name(tagName)
                        .color("#2563EB")
                        .isSystem(false)
                        .build()));

        if (!taggingRepository.existsByResourceIdAndTagId(resourceId, tag.getId())) {
            ResourceTagging tagging = ResourceTagging.builder()
                    .resourceId(resourceId)
                    .tagId(tag.getId())
                    .build();
            taggingRepository.save(tagging);
        }

        return mapToResponse(resource);
    }

    @Transactional
    public void removeTag(UUID resourceId, UUID tagId, UUID institutionId) {
        removeTag(resourceId, tagId, institutionId, null, null);
    }

    @Transactional
    public void removeTag(UUID resourceId, UUID tagId, UUID institutionId, UUID userId, String userRole) {
        loadManagedResource(resourceId, institutionId, userId, userRole);
        taggingRepository.deleteByResourceIdAndTagId(resourceId, tagId);
    }

    /** Tag management follows resource ownership: owner or admin only. */
    private Resource loadManagedResource(UUID resourceId, UUID institutionId, UUID userId, String userRole) {
        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> r.getInstitutionId().equals(institutionId))
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        if (userId != null && (resource.getUploadedBy() == null || !resource.getUploadedBy().equals(userId))
                && !isAdminRole(userRole)) {
            throw new SecurityException("You do not own this resource");
        }
        return resource;
    }

    // ── Student Saved Resources ──
    /**
     * Bookmarking never grants access: the target must exist inside the
     * caller's institution and the caller must already be able to see it.
     */
    @Transactional
    public void saveResource(UUID studentId, UUID resourceId, UUID institutionId, String userRole) {
        requireVisible(resourceId, institutionId, studentId, userRole);
        if (savedResourceRepository.existsByStudentIdAndResourceId(studentId, resourceId)) {
            return;
        }
        StudentSavedResource saved = StudentSavedResource.builder()
                .studentId(studentId)
                .resourceId(resourceId)
                .build();
        savedResourceRepository.save(saved);
    }

    @Transactional
    public void unsaveResource(UUID studentId, UUID resourceId) {
        savedResourceRepository.deleteByStudentIdAndResourceId(studentId, resourceId);
    }

    /** Saved list re-checks every row: institution + visibility still apply. */
    @Transactional(readOnly = true)
    public List<ResourceResponse> getSavedResources(UUID studentId, UUID institutionId, String userRole) {
        return savedResourceRepository.findByStudentIdOrderBySavedAtDesc(studentId).stream()
                .map(ssr -> resourceRepository.findById(ssr.getResourceId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .filter(r -> institutionId == null || canSeeResource(r, studentId, userRole))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isSaved(UUID studentId, UUID resourceId) {
        return savedResourceRepository.existsByStudentIdAndResourceId(studentId, resourceId);
    }

    // ── Authorized content access (view URL + download) ──

    /**
     * Resolves a short-lived, authorized URL for viewing this resource's real
     * content.  Authorization (institution + visibility + state) happens here —
     * the returned URL is only a transport detail, never a permission grant.
     */
    @Transactional(readOnly = true)
    public String resolveContentUrl(UUID resourceId, UUID institutionId, UUID userId, String userRole,
                                    String bearerToken) {
        Resource resource = requireVisible(resourceId, institutionId, userId, userRole);

        if (resource.isExternalLink()) {
            if (isBlank(resource.getExternalUrl())) {
                throw new IllegalStateException("This link resource has no target URL");
            }
            return resource.getExternalUrl();
        }
        requireReady(resource);

        if (resource.getMediaId() != null) {
            Map<String, Object> download = mediaProxyService.getDownloadUrl(String.valueOf(resource.getMediaId()), bearerToken);
            Object url = download != null ? download.get("downloadUrl") : null;
            if (url != null && !String.valueOf(url).isBlank()) {
                return String.valueOf(url);
            }
        }
        if (!isBlank(resource.getStorageUrl())) {
            return resource.getStorageUrl();
        }
        throw new ResourceNotFoundException("This resource has no stored content");
    }

    /** Bytes for an authorized download; view and download stay separate rights. */
    @Transactional(readOnly = true)
    public ResourceDownload resolveDownload(UUID resourceId, UUID institutionId, UUID userId, String userRole,
                                            String bearerToken) {
        Resource resource = requireVisible(resourceId, institutionId, userId, userRole);

        if (resource.isExternalLink()) {
            throw new IllegalStateException("Link resources are opened, not downloaded");
        }
        requireReady(resource);

        boolean allowed = Boolean.TRUE.equals(resource.getIsDownloadable())
                || isAdminRole(userRole)
                || (resource.getUploadedBy() != null && resource.getUploadedBy().equals(userId));
        if (!allowed) {
            throw new SecurityException("Download is not permitted for this resource");
        }

        String sourceUrl = null;
        if (resource.getMediaId() != null) {
            Map<String, Object> download = mediaProxyService.getDownloadUrl(String.valueOf(resource.getMediaId()), bearerToken);
            Object url = download != null ? download.get("downloadUrl") : null;
            if (url != null && !String.valueOf(url).isBlank()) {
                sourceUrl = String.valueOf(url);
            }
        }
        if (sourceUrl == null && !isBlank(resource.getStorageUrl())) {
            sourceUrl = resource.getStorageUrl();
        }
        if (sourceUrl == null) {
            throw new ResourceNotFoundException("This resource has no stored content");
        }

        byte[] bytes = mediaProxyService.fetchBytes(sourceUrl);
        return new ResourceDownload(resolveContentType(resource), buildDownloadFileName(resource), bytes);
    }

    /** Read gate for content endpoints: institution + state + visibility. */
    public Resource requireVisible(UUID resourceId, UUID institutionId, UUID userId, String userRole) {
        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> r.getInstitutionId().equals(institutionId))
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        if (!canSeeResource(resource, userId, userRole)) {
            throw new SecurityException("Access denied to resource");
        }
        return resource;
    }

    /** Students must never receive a resource that is still processing or failed. */
    private void requireReady(Resource resource) {
        String status = resource.getProcessingStatus();
        if (status == null || "READY".equals(status)) {
            return;
        }
        throw new IllegalStateException("This resource is not available yet (status: " + status + ")");
    }

    private static String resolveContentType(Resource resource) {
        if (!isBlank(resource.getMimeType())) {
            return resource.getMimeType();
        }
        if (resource.getResourceType() != null) {
            return switch (resource.getResourceType()) {
                case PDF -> "application/pdf";
                case IMAGE -> "image/*";
                case VIDEO, LIVE_RECORDING -> "video/*";
                case AUDIO -> "audio/*";
                case ARCHIVE -> "application/zip";
                default -> "application/octet-stream";
            };
        }
        return "application/octet-stream";
    }

    private static String buildDownloadFileName(Resource resource) {
        String base = resource.getTitle() != null ? resource.getTitle() : "resource";
        base = base.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim();
        if (base.isEmpty()) base = "resource";
        String ext = extensionFor(resource);
        if (ext != null && !base.toLowerCase(Locale.ROOT).endsWith(ext)) {
            base = base + ext;
        }
        return base;
    }

    private static String extensionFor(Resource resource) {
        String mime = resource.getMimeType();
        if (mime != null) {
            return switch (mime.toLowerCase(Locale.ROOT)) {
                case "application/pdf" -> ".pdf";
                case "image/png" -> ".png";
                case "image/jpeg" -> ".jpg";
                case "image/gif" -> ".gif";
                case "audio/wav", "audio/x-wav" -> ".wav";
                case "audio/mpeg" -> ".mp3";
                case "audio/mp4", "audio/m4a" -> ".m4a";
                case "video/mp4" -> ".mp4";
                case "video/webm" -> ".webm";
                case "application/zip" -> ".zip";
                default -> null;
            };
        }
        if (!isBlank(resource.getStorageUrl()) && resource.getStorageUrl().contains(".")) {
            String path = resource.getStorageUrl();
            String tail = path.substring(path.lastIndexOf('/') + 1);
            int dot = tail.lastIndexOf('.');
            if (dot > 0 && dot < tail.length() - 1 && tail.length() <= 8) {
                return "." + tail.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    /** Authorized download payload returned to the controller for streaming. */
    public record ResourceDownload(String contentType, String fileName, byte[] bytes) {
    }

    /** Public view of a stored entity for callers that loaded the row directly. */
    public ResourceResponse toResponse(Resource resource) {
        return mapToResponse(resource);
    }

    // ── Presigned Upload URL ──
    public String getPresignedUploadUrl(UUID institutionId, String fileName, String contentType, UUID userId) {
        // TODO: Implement presigned URL generation using StorageService
        // For now, return placeholder
        return "/api/v1/media/presigned-upload";
    }

    // ── Helper Methods ──

    /**
     * Static visibility check used by callers outside this service (e.g. the learner
     * lesson endpoint that aggregates lesson materials).
     *
     * @param resource the resource to check (never null)
     * @param userId   id of the requesting user (may be null)
     * @param userRole role of the requesting user
     * @return true when the caller is allowed to see the resource
     */
    private boolean canAccessResource(Resource resource, UUID userId, String userRole) {
        return canSeeResource(resource, userId, userRole);
    }

    /**
     * Single source of truth for read access: admins see everything; the uploader
     * always sees their own; PRIVATE is owner-only even for other teachers
     * (prompt: Teacher A must not reach Teacher B's private resources);
     * everything else follows the role's allowed visibility set.
     */
    public static boolean canSeeResource(Resource resource, UUID userId, String userRole) {
        if (resource == null || Boolean.TRUE.equals(resource.getIsDeleted())
                || resource.getVisibility() == null) {
            return false;
        }
        if (isAdminRole(userRole)) {
            return true;
        }
        if (resource.getUploadedBy() != null && resource.getUploadedBy().equals(userId)) {
            return true;
        }
        if (resource.getVisibility() == Resource.ResourceVisibility.PRIVATE) {
            return false;
        }
        return getAllowedVisibilities(userRole).contains(resource.getVisibility().name());
    }

    public static List<String> getAllowedVisibilities(String userRole) {
        if ("ADMIN".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole) || "NATIONAL_ADMIN".equals(userRole)) {
            return ADMIN_VISIBILITIES;
        } else if ("TEACHER".equals(userRole) || "INSTRUCTOR".equals(userRole) || "LECTURER".equals(userRole)) {
            return TEACHER_VISIBILITIES;
        } else {
            return STUDENT_VISIBILITIES;
        }
    }

    public static boolean isAdminRole(String userRole) {
        return "ADMIN".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole) || "NATIONAL_ADMIN".equals(userRole);
    }

    // ── Reorder Resources ──
    @Transactional
    public List<ResourceResponse> reorderResources(List<ReorderItem> items, UUID institutionId, UUID userId, String userRole) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<UUID> resourceIds = items.stream().map(ReorderItem::getId).collect(Collectors.toList());
        List<Resource> resources = resourceRepository.findAllById(resourceIds);

        if (resources.size() != resourceIds.size()) {
            throw new ResourceNotFoundException("One or more resources not found");
        }

        for (Resource resource : resources) {
            if (!resource.getInstitutionId().equals(institutionId)) {
                throw new SecurityException("Access denied to resource");
            }
            if ((resource.getUploadedBy() == null || !resource.getUploadedBy().equals(userId)) && !isAdminRole(userRole)) {
                throw new SecurityException("You do not own this resource");
            }
        }

        for (ReorderItem item : items) {
            Resource resource = resources.stream()
                    .filter(r -> r.getId().equals(item.id()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + item.id()));
            resource.setSortOrder(item.sortOrder());
        }

        List<Resource> savedResources = resourceRepository.saveAll(resources);
        return savedResources.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public record ReorderItem(UUID id, Integer sortOrder) {
        public UUID getId() { return id; }
        public Integer getSortOrder() { return sortOrder; }
    }

    private void validateLessonAccess(UUID lessonId, UUID institutionId) {
        lessonRepository.findById(lessonId)
                .filter(l -> !l.getIsDeleted())
                .filter(l -> institutionId.equals(l.getInstitutionId()))
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
    }

    private ResourceResponse mapToResponse(Resource resource) {
        User uploader = userRepository.findById(resource.getUploadedBy()).orElse(null);
        return ResourceResponse.builder()
                .id(resource.getId())
                .institutionId(resource.getInstitutionId())
                .lessonId(resource.getLessonId())
                .moduleId(resource.getModuleId())
                .courseId(resource.getCourseId())
                .subjectId(resource.getSubjectId())
                .uploadedBy(resource.getUploadedBy())
                .uploadedByName(resource.getUploadedBy() != null ? 
                    (uploader != null ? uploader.getFirstName() + " " + uploader.getLastName() : "Unknown") : null)
                .title(resource.getTitle())
                .description(resource.getDescription())
                .resourceType(resource.getResourceType().name())
                .mimeType(resource.getMimeType())
                .fileSize(resource.getFileSize())
                .storageUrl(resource.getStorageUrl())
                .storageObjectKey(resource.getStorageObjectKey())
                .storageBucket(resource.getStorageBucket())
                .externalUrl(resource.getExternalUrl())
                .thumbnailUrl(resource.getThumbnailUrl())
                .durationSeconds(resource.getDurationSeconds())
                .pageCount(resource.getPageCount())
                .width(resource.getWidth())
                .height(resource.getHeight())
                .visibility(resource.getVisibility().name())
                .sortOrder(resource.getSortOrder())
                .isDownloadable(resource.getIsDownloadable())
                .isPreviewable(resource.getIsPreviewable())
                .processingStatus(resource.getProcessingStatus())
                .processingError(resource.getProcessingError())
                .metadata(resource.getMetadata())
                .tags(resource.getTags())
                .uploadedByName(uploader != null ? uploader.getFirstName() + " " + uploader.getLastName() : "Unknown")
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .isDeletable(true)
                .isEditable(true)
                .isViewable(true)
                .isDownloadable(resource.getIsDownloadable())
                .isPreviewable(resource.getIsPreviewable())
                .build();
    }
}