package tz.elmkusoma.learning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.common.ApiResponse;
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

import java.util.Objects;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    private static final List<String> STUDENT_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION");
    private static final List<String> TEACHER_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
    private static final List<String> ADMIN_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");

    // ── Create Resource ──
    @Transactional
    public ResourceResponse createResource(ResourceRequest request, UUID institutionId, UUID userId) {
        log.info("Creating resource: {} for institution: {}", request.getTitle(), institutionId);

        // Validate lesson/module/course access if provided
        if (request.getLessonId() != null) {
            validateLessonAccess(request.getLessonId(), institutionId);
        }
        if (request.getModuleId() != null) {
            // TODO: validate module access
        }
        if (request.getCourseId() != null) {
            // TODO: validate course access
        }

        Resource resource = Resource.builder()
                .institutionId(institutionId)
                .lessonId(request.getLessonId())
                .moduleId(request.getModuleId())
                .courseId(request.getCourseId())
                .uploadedBy(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .resourceType(Resource.ResourceType.valueOf(request.getResourceType()))
                .visibility(Resource.ResourceVisibility.valueOf(request.getVisibility()))
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isDownloadable(request.getIsDownloadable() != null ? request.getIsDownloadable() : true)
                .isPreviewable(request.getIsPreviewable() != null ? request.getIsPreviewable() : false)
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .processingStatus("READY")
                .storageUrl(request.getStorageUrl())
                .storageObjectKey(request.getStorageObjectKey())
                .storageBucket(request.getStorageBucket())
                .externalUrl(request.getExternalUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .mimeType(request.getMimeType())
                .fileSize(request.getFileSize())
                .durationSeconds(request.getDurationSeconds())
                .pageCount(request.getPageCount())
                .width(request.getWidth())
                .height(request.getHeight())
                .build();

        // Handle tags
        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            List<ResourceTag> tags = request.getTagNames().stream()
                    .map(name -> tagRepository.findByName(name)
                            .orElseGet(() -> tagRepository.save(ResourceTag.builder()
                                    .name(name)
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

        Resource savedResource = resourceRepository.save(resource);
        log.info("Created resource: {} for institution: {}", resource.getId(), institutionId);

        return mapToResponse(resource);
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
        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> r.getInstitutionId().equals(institutionId))
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        // Ownership: only the uploader (or an institution/platform admin) may modify
        if ((resource.getUploadedBy() == null || !resource.getUploadedBy().equals(userId)) && !isAdminRole(userRole)) {
            throw new SecurityException("You do not own this resource");
        }

        if (request.getTitle() != null) resource.setTitle(request.getTitle());
        if (request.getDescription() != null) resource.setDescription(request.getDescription());
        if (request.getResourceType() != null) resource.setResourceType(Resource.ResourceType.valueOf(request.getResourceType()));
        if (request.getVisibility() != null) resource.setVisibility(Resource.ResourceVisibility.valueOf(request.getVisibility()));
        if (request.getSortOrder() != null) resource.setSortOrder(request.getSortOrder());
        if (request.getIsDownloadable() != null) resource.setIsDownloadable(request.getIsDownloadable());
        if (request.getIsPreviewable() != null) resource.setIsPreviewable(request.getIsPreviewable());
        if (request.getTags() != null) resource.setTags(request.getTags());
        if (request.getMetadata() != null) resource.setMetadata(request.getMetadata());

        // Update tags
        if (request.getLessonId() != null) resource.setLessonId(request.getLessonId());

        if (request.getTagNames() != null) {
            List<ResourceTag> tags = request.getTagNames().stream()
                    .map(name -> tagRepository.findByName(name)
                            .orElseGet(() -> tagRepository.save(ResourceTag.builder()
                                    .name(name)
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

        Resource savedResource = resourceRepository.save(resource);
        return mapToResponse(savedResource);
    }

    // ── Delete Resource ──
    @Transactional
    public void deleteResource(UUID resourceId, UUID institutionId, UUID userId, String userRole) {
        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> r.getInstitutionId().equals(institutionId))
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        // Ownership: only the uploader (or an institution/platform admin) may delete
        if ((resource.getUploadedBy() == null || !resource.getUploadedBy().equals(userId)) && !isAdminRole(userRole)) {
            throw new SecurityException("You do not own this resource");
        }

        resource.setIsDeleted(true);
        resourceRepository.save(resource);
        log.info("Deleted resource: {}", resourceId);
    }

    // ── Resource Tags ──
    @Transactional
    public ResourceResponse addTag(UUID resourceId, String tagName, UUID institutionId) {
        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> r.getInstitutionId().equals(institutionId))
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

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

        return getResource(resourceId, institutionId, null, "ADMIN");
    }

    @Transactional
    public void removeTag(UUID resourceId, UUID tagId, UUID institutionId) {
        taggingRepository.deleteByResourceIdAndTagId(resourceId, tagId);
    }

    // ── Student Saved Resources ──
    @Transactional
    public void saveResource(UUID studentId, UUID resourceId) {
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

    @Transactional(readOnly = true)
    public List<ResourceResponse> getSavedResources(UUID studentId) {
        return savedResourceRepository.findByStudentIdOrderBySavedAtDesc(studentId).stream()
                .map(ssr -> resourceRepository.findById(ssr.getResourceId())
                        .map(this::mapToResponse)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isSaved(UUID studentId, UUID resourceId) {
        return savedResourceRepository.existsByStudentIdAndResourceId(studentId, resourceId);
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