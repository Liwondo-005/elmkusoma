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
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.course.repository.CourseModuleRepository;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.learner.domain.LearnerEnrollment;
import tz.elmkusoma.learner.repository.LearnerEnrollmentRepository;
import tz.elmkusoma.learner.service.NotificationService;
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
import java.util.Set;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherAssignment;
import tz.elmkusoma.teacher.domain.TeacherAssignmentStatus;

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
    // Existing domain models reused for governance, eligibility and notification
    // audience — no new parallel services were introduced.
    private final ClassAccessGuard classAccessGuard;
    private final LearnerEnrollmentRepository learnerEnrollmentRepository;
    private final InstitutionRepository institutionRepository;
    private final NotificationService notificationService;
    // The existing teacher-assignment model is reused as the authoritative class
    // target of teacher-created resources — no parallel targeting system.
    private final tz.elmkusoma.teacher.repository.TeacherAssignmentRepository teacherAssignmentRepository;
    private final tz.elmkusoma.teacher.repository.TeacherRepository teacherRepository;

    private static final List<String> STUDENT_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION");
    private static final List<String> TEACHER_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
    private static final List<String> ADMIN_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
    /** Roles whose resource reads must satisfy course/class eligibility. */
    private static final List<String> LEARNER_ROLES = List.of("STUDENT", "OTHER_LEARNER", "LEARNER");

    // ── Create Resource (JSON path: URL-based types and pre-stored files) ──
    @Transactional
    public ResourceResponse createResource(ResourceRequest request, UUID institutionId, UUID userId) {
        return createResource(request, institutionId, userId, "TEACHER");
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request, UUID institutionId, UUID userId, String userRole) {
        log.info("Creating resource: {} for institution: {}", request.getTitle(), institutionId);

        Resource.ResourceType type = resolveType(request.getResourceType());
        validateContext(request, institutionId, userId, userRole);

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
        notifyResourcePublished(resource, userId);
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
        validateContext(request, institutionId, userId, userRole);
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
        notifyResourcePublished(saved, userId);
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
                .teacherAssignmentId(request.getTeacherAssignmentId())
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

    /**
     * Lesson/module/course links must exist inside the caller's institution, and a
     * teaching-assignment target must exist inside the caller's institution, belong
     * to the caller's own teacher profile (when the caller is a teacher) and still
     * be ACTIVE. Client-supplied ids are never trusted. CLASS_ONLY writes must
     * carry an authoritative class target (assignment or lesson).
     */
    private void validateContext(ResourceRequest request, UUID institutionId, UUID userId, String userRole) {
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
        validateAssignmentTarget(request.getTeacherAssignmentId(), institutionId, userId, userRole);
        requireClassTarget(request.getVisibility(), request.getLessonId(), request.getTeacherAssignmentId());
    }

    /**
     * Verifies the teaching-assignment target of a create/retarget request and
     * returns its id (null when untargeted).
     *
     * <p>The assignment must exist (not soft-deleted) inside the caller's
     * institution; a TEACHER may only target their own assignment; learners never
     * may; and the assignment must be ACTIVE for any new or changed target —
     * ended assignments cannot host new resources. Admins may target any
     * assignment of their own institution (institution filter above already
     * scoped the row). Out-of-institution ids surface as 404, never as a probe.
     */
    private UUID validateAssignmentTarget(UUID assignmentId, UUID institutionId, UUID userId, String userRole) {
        if (assignmentId == null) {
            return null;
        }
        TeacherAssignment assignment = teacherAssignmentRepository.findById(assignmentId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .filter(a -> institutionId != null && institutionId.equals(a.getInstitutionId()))
                .orElseThrow(() -> new ResourceNotFoundException("Teaching assignment", "id", assignmentId));
        if (LEARNER_ROLES.contains(userRole)) {
            throw new SecurityException("Learners cannot target teaching assignments");
        }
        if ("TEACHER".equals(userRole)) {
            UUID teacherId = teacherRepository.findByUserIdAndInstitutionId(userId, institutionId)
                    .map(Teacher::getId)
                    .orElse(null);
            if (teacherId == null || !teacherId.equals(assignment.getTeacherId())) {
                throw new SecurityException("You can only target your own teaching assignments");
            }
        }
        if (assignment.getStatus() != null && assignment.getStatus() != TeacherAssignmentStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "The teaching assignment is " + assignment.getStatus()
                            + " and cannot be used for new resources");
        }
        return assignmentId;
    }

    /** New/changed CLASS_ONLY writes must land on an authoritative class target. */
    private void requireClassTarget(String visibility, UUID lessonId, UUID assignmentId) {
        if (!"CLASS_ONLY".equals(visibility)) {
            return;
        }
        if (lessonId == null && assignmentId == null) {
            throw new IllegalArgumentException(
                    "CLASS_ONLY resources must target a teaching assignment or a lesson");
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
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        requireReadAccess(resource, institutionId, userId, userRole);

        return mapToResponse(resource);
    }

    // ── List Resources ──
    @Transactional(readOnly = true)
    public List<ResourceResponse> listResources(UUID institutionId, UUID userId, String userRole,
                                                UUID lessonId, UUID moduleId, UUID courseId,
                                                String resourceType, String visibility,
                                                int page, int size) {
        return listResources(institutionId, userId, userRole, lessonId, moduleId, courseId,
                resourceType, visibility, page, size, null);
    }

    /**
     * Server-side listing with an optional title/description query. Jurisdiction
     * admins (REGIONAL/DISTRICT) are scoped to the institutions of their
     * jurisdiction; everyone else stays inside their own institution with the
     * existing visibility rules, plus learner eligibility.
     */
    @Transactional(readOnly = true)
    public List<ResourceResponse> listResources(UUID institutionId, UUID userId, String userRole,
                                                UUID lessonId, UUID moduleId, UUID courseId,
                                                String resourceType, String visibility,
                                                int page, int size, String q) {
        if (isJurisdictionRole(userRole)) {
            return jurisdictionResources(userId, userRole).stream()
                    .filter(r -> matchesContextFilters(r, lessonId, moduleId, courseId))
                    .filter(r -> matchesQuery(r, q))
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        if (institutionId == null) {
            throw new SecurityException("Access denied to resources");
        }

        List<String> allowedVisibilities = getAllowedVisibilities(userRole);
        List<Resource> base;

        if (lessonId != null) {
            base = resourceRepository.findVisibleByLessonId(lessonId, allowedVisibilities)
                    .stream()
                    // institution predicate: a foreign lesson id must never list
                    // another tenant's resources
                    .filter(r -> institutionId.equals(r.getInstitutionId()))
                    .toList();
        } else if (moduleId != null) {
            base = resourceRepository.findVisibleByModuleId(moduleId, allowedVisibilities)
                    .stream()
                    .filter(r -> institutionId.equals(r.getInstitutionId()))
                    .toList();
        } else if (courseId != null) {
            base = resourceRepository.findVisibleByCourseId(courseId, allowedVisibilities)
                    .stream()
                    .filter(r -> institutionId.equals(r.getInstitutionId()))
                    .toList();
        } else if (q != null && !q.isBlank()) {
            base = resourceRepository.searchByInstitutionIdAndIsDeletedFalse(institutionId, q)
                    .stream()
                    .filter(r -> r.getVisibility() != null
                            && allowedVisibilities.contains(r.getVisibility().name()))
                    .toList();
        } else {
            base = resourceRepository.findByInstitutionIdAndVisibilities(institutionId, allowedVisibilities);
        }

        return base.stream()
                .filter(r -> matchesQuery(r, q))
                .filter(r -> canRead(r, userId, userRole))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Update Resource ──
    @Transactional
    public ResourceResponse updateResource(UUID resourceId, ResourceRequest request, UUID institutionId, UUID userId, String userRole) {
        Resource resource = loadOwnedResource(resourceId, institutionId, userId, userRole);
        Resource.ResourceVisibility previousVisibility = resource.getVisibility();

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
        // Retargeting is opt-in: an explicit, *changed* id is validated
        // (ownership + institution + ACTIVE) and applied; re-sending the id the
        // resource already carries is a no-op, so editing an older resource is
        // never blocked by its assignment having ended since.
        if (request.getTeacherAssignmentId() != null
                && !request.getTeacherAssignmentId().equals(resource.getTeacherAssignmentId())) {
            resource.setTeacherAssignmentId(
                    validateAssignmentTarget(request.getTeacherAssignmentId(), institutionId, userId, userRole));
        }
        // A visibility change that lands on CLASS_ONLY must carry a real target.
        // Echoing the current visibility is a no-op (the edit form always sends
        // it), so legacy CLASS_ONLY rows without targets stay editable.
        if (request.getVisibility() != null
                && Resource.ResourceVisibility.valueOf(request.getVisibility()) != previousVisibility) {
            requireClassTarget(request.getVisibility(), resource.getLessonId(), resource.getTeacherAssignmentId());
        }

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
        notifyIfBecameLearnerVisible(previousVisibility, savedResource, userId);
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
        Resource.ResourceVisibility previousVisibility = resource.getVisibility();
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
        // Same targeting rules as the JSON update path: a *changed* target is
        // validated (ownership + institution + ACTIVE); same-id is a no-op.
        if (request.getTeacherAssignmentId() != null
                && !request.getTeacherAssignmentId().equals(resource.getTeacherAssignmentId())) {
            resource.setTeacherAssignmentId(
                    validateAssignmentTarget(request.getTeacherAssignmentId(), institutionId, userId, userRole));
        }
        // Enforced only on an actual visibility change (see JSON path).
        if (request.getVisibility() != null
                && Resource.ResourceVisibility.valueOf(request.getVisibility()) != previousVisibility) {
            requireClassTarget(request.getVisibility(), resource.getLessonId(), resource.getTeacherAssignmentId());
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
        notifyIfBecameLearnerVisible(previousVisibility, saved, userId);

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
                .filter(r -> institutionId == null || canRead(r, studentId, userRole))
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

    /**
     * Read gate for content endpoints: jurisdiction → institution → state →
     * visibility → learner eligibility. Out-of-scope reads fail exactly like
     * before — 404 for institution/jurisdiction misses (no existence oracle),
     * 403 for visibility/eligibility denials.
     */
    public Resource requireVisible(UUID resourceId, UUID institutionId, UUID userId, String userRole) {
        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        requireReadAccess(resource, institutionId, userId, userRole);
        return resource;
    }

    private void requireReadAccess(Resource resource, UUID institutionId, UUID userId, String userRole) {
        if (isJurisdictionRole(userRole)) {
            // Regional/district governance: read inside the authorized
            // jurisdiction only, regardless of which institution the row
            // belongs to. Never reachable by merely knowing a UUID.
            if (!withinJurisdiction(resource, userId, userRole)) {
                throw new ResourceNotFoundException("Resource not found");
            }
            return;
        }
        if (institutionId == null || !institutionId.equals(resource.getInstitutionId())) {
            throw new ResourceNotFoundException("Resource not found");
        }
        if (!canSeeResource(resource, userId, userRole)) {
            throw new SecurityException("Access denied to resource");
        }
        assertEligible(resource, userId, userRole);
        assertAssignmentAudience(resource, userId, userRole);
    }

    // ── Jurisdiction scope (reuses the OversightController region/district rule) ──

    private static boolean isJurisdictionRole(String userRole) {
        return "REGIONAL_ADMIN".equals(userRole) || "DISTRICT_ADMIN".equals(userRole);
    }

    /** Region (regional admin) or district (district admin) match against the resource's institution. */
    private boolean withinJurisdiction(Resource resource, UUID userId, String userRole) {
        if (resource == null || resource.getInstitutionId() == null || userId == null) {
            return false;
        }
        Institution institution = institutionRepository.findByIdAndIsDeletedFalse(resource.getInstitutionId())
                .orElse(null);
        if (institution == null) {
            return false;
        }
        if ("REGIONAL_ADMIN".equals(userRole)) {
            UUID userRegionId = userRepository.findById(userId).map(User::getRegionId).orElse(null);
            return userRegionId != null && userRegionId.equals(institution.getRegionId());
        }
        if ("DISTRICT_ADMIN".equals(userRole)) {
            UUID userDistrictId = userRepository.findById(userId).map(User::getDistrictId).orElse(null);
            return userDistrictId != null && userDistrictId.equals(institution.getDistrictId());
        }
        return false;
    }

    /** All non-deleted resources of the institutions inside the caller's jurisdiction. */
    private List<Resource> jurisdictionResources(UUID userId, String userRole) {
        if (userId == null) {
            return List.of();
        }
        if ("REGIONAL_ADMIN".equals(userRole)) {
            UUID regionId = userRepository.findById(userId).map(User::getRegionId).orElse(null);
            if (regionId == null) {
                return List.of();
            }
            List<UUID> institutionIds = institutionRepository.findByRegionIdAndIsDeletedFalse(regionId)
                    .stream().map(Institution::getId).toList();
            return institutionIds.isEmpty()
                    ? List.of()
                    : resourceRepository.findByInstitutionIdInAndIsDeletedFalse(institutionIds);
        }
        if ("DISTRICT_ADMIN".equals(userRole)) {
            UUID districtId = userRepository.findById(userId).map(User::getDistrictId).orElse(null);
            if (districtId == null) {
                return List.of();
            }
            List<UUID> institutionIds = institutionRepository.findByDistrictIdAndIsDeletedFalse(districtId)
                    .stream().map(Institution::getId).toList();
            return institutionIds.isEmpty()
                    ? List.of()
                    : resourceRepository.findByInstitutionIdInAndIsDeletedFalse(institutionIds);
        }
        return List.of();
    }

    private static boolean matchesContextFilters(Resource resource, UUID lessonId, UUID moduleId, UUID courseId) {
        if (lessonId != null) {
            return lessonId.equals(resource.getLessonId());
        }
        if (moduleId != null) {
            return moduleId.equals(resource.getModuleId());
        }
        if (courseId != null) {
            return courseId.equals(resource.getCourseId());
        }
        return true;
    }

    private static boolean matchesQuery(Resource resource, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.trim().toLowerCase(Locale.ROOT);
        if (resource.getTitle() != null && resource.getTitle().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        return resource.getDescription() != null
                && resource.getDescription().toLowerCase(Locale.ROOT).contains(needle);
    }

    // ── Learner eligibility (existing enrollment/class-membership model) ──

    /**
     * A learner may only read a resource when they belong to the audiences the
     * resource is attached to: course → {@code learner_enrollments}, lesson →
     * class membership via the existing {@link ClassAccessGuard}. Standalone
     * resources (no course/lesson link) stay governed by institution +
     * visibility alone. Teachers/admins are never subject to this gate.
     */
    public void assertEligible(Resource resource, UUID userId, String userRole) {
        if (resource == null || userId == null || !LEARNER_ROLES.contains(userRole)) {
            return;
        }
        if (resource.getCourseId() != null) {
            boolean enrolled = learnerEnrollmentRepository
                    .findByUserIdAndCourseIdAndIsDeletedFalse(userId, resource.getCourseId())
                    .isPresent();
            if (!enrolled) {
                throw new SecurityException("You are not enrolled in the course for this resource");
            }
        }
        if (resource.getLessonId() != null) {
            lessonRepository.findById(resource.getLessonId())
                    .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                    .ifPresent(lesson -> {
                        if (lesson.getClassGroupId() != null) {
                            String email = userRepository.findById(userId).map(User::getEmail).orElse(null);
                            classAccessGuard.assertLearnerCanAccessClass(email, lesson.getClassGroupId());
                        }
                    });
        }
    }

    /** Non-throwing form for list filters. */
    public boolean isEligible(Resource resource, UUID userId, String userRole) {
        try {
            assertEligible(resource, userId, userRole);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Audience gate for assignment-targeted (class) resources.
     *
     * <p>Readable by: the learners of the target class (existing
     * {@link ClassAccessGuard} membership model — student_class_assignments ∪
     * ENROLLED enrollments, with proven membership: the shared guard's
     * "no membership data → allow" fallback never opens a class-targeted
     * resource), the uploading teacher, teachers holding an assignment for
     * that class in that institution, and admins. Jurisdiction governance
     * governance roles were already checked by {@code requireReadAccess} and
     * bypass here. A query parameter such as {@code ?classGroupId=} never
     * reaches this gate — the target comes from the stored row only. Legacy
     * resources without a target are untouched.</p>
     *
     * <p>ENDED assignments still serve their class on reads (history preserved);
     * only a genuinely missing row fails the read.</p>
     */
    public void assertAssignmentAudience(Resource resource, UUID userId, String userRole) {
        if (resource == null || resource.getTeacherAssignmentId() == null) {
            return;
        }
        if (userId == null || isJurisdictionRole(userRole)) {
            return;
        }
        boolean learner = LEARNER_ROLES.contains(userRole);
        boolean teacher = "TEACHER".equals(userRole);
        if (!learner && !teacher) {
            return; // admins keep their existing bypass
        }
        TeacherAssignment assignment = teacherAssignmentRepository
                .findById(resource.getTeacherAssignmentId())
                .orElseThrow(() -> new SecurityException(
                        "The teaching assignment for this resource no longer exists"));
        if (learner) {
            // Strict membership: the shared guard's "no membership data →
            // allow" fallback must not open assignment-targeted resources to
            // learners who cannot prove they belong to the target class
            // (e.g. OTHER_LEARNER profiles without any class rows).
            String email = userRepository.findById(userId).map(User::getEmail).orElse(null);
            if (!classAccessGuard.isLearnerInClass(email, assignment.getClassGroupId())) {
                log.warn("Assignment audience denied: user {} is not a member of class {}",
                        userId, assignment.getClassGroupId());
                throw new SecurityException("You are not a member of this class");
            }
            return;
        }
        if (userId.equals(resource.getUploadedBy())) {
            return; // the uploading teacher always keeps access to their resource
        }
        UUID teacherId = teacherRepository
                .findByUserIdAndInstitutionId(userId, resource.getInstitutionId())
                .map(Teacher::getId)
                .orElse(null);
        boolean authorized = teacherId != null
                && teacherAssignmentRepository.existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(
                        teacherId, assignment.getClassGroupId());
        if (!authorized) {
            throw new SecurityException("Access denied to resource");
        }
    }

    /** Single read predicate for callers outside this service: visibility + eligibility + audience. */
    public boolean canRead(Resource resource, UUID userId, String userRole) {
        if (!canSeeResource(resource, userId, userRole) || !isEligible(resource, userId, userRole)) {
            return false;
        }
        try {
            assertAssignmentAudience(resource, userId, userRole);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Analytics scope: institution admins own their institution's numbers and
     * teachers own their own resources — another teacher must never read
     * engagement data outside their scope.
     */
    public Resource requireAnalyticsAccess(UUID resourceId, UUID institutionId, UUID userId, String userRole) {
        Resource resource = requireVisible(resourceId, institutionId, userId, userRole);
        if (!isAdminRole(userRole)
                && (resource.getUploadedBy() == null || !resource.getUploadedBy().equals(userId))) {
            throw new SecurityException("Resource analytics are limited to the resource owner");
        }
        return resource;
    }

    // ── Publish notifications (existing NotificationService, server-side audience) ──

    /** Hidden content is never announced; everything else notifies the eligible audience. */
    private void notifyResourcePublished(Resource resource, UUID publisherId) {
        try {
            if (resource == null || resource.getVisibility() == null) {
                return;
            }
            String visibility = resource.getVisibility().name();
            if ("PRIVATE".equals(visibility) || "DRAFT".equals(visibility)) {
                return;
            }
            String title = "New resource available";
            String message = "New resource available: " + resource.getTitle();

            List<UUID> audience = null;
            boolean institutionFallback = false;

            if (resource.getLessonId() != null) {
                audience = lessonRepository.findById(resource.getLessonId())
                        .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                        .map(l -> classAccessGuard.resolveClassStudentUserIds(l.getClassGroupId()))
                        .orElse(List.of());
                if (audience.isEmpty()) {
                    // no class audience modelled → same fallback the lesson
                    // publisher uses (and the guard's own fail-open read rule)
                    institutionFallback = true;
                }
            } else if (resource.getCourseId() != null) {
                audience = learnerEnrollmentRepository.findByCourseIdAndIsDeletedFalse(resource.getCourseId())
                        .stream()
                        .map(LearnerEnrollment::getUserId)
                        .distinct()
                        .toList();
                if (audience.isEmpty()) {
                    // course eligibility is strict — nobody is entitled yet,
                    // so nobody gets announced
                    return;
                }
            } else {
                institutionFallback = true;
            }

            if (institutionFallback) {
                notificationService.notifyInstitutionStudentsExcluding(
                        resource.getInstitutionId(), publisherId, title, message,
                        "RESOURCE_PUBLISHED", "resource", resource.getId());
                return;
            }
            for (UUID studentUserId : audience) {
                if (studentUserId == null || studentUserId.equals(publisherId)) {
                    continue;
                }
                notificationService.notifyUser(studentUserId, title, message,
                        "RESOURCE_PUBLISHED", "resource", resource.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to notify learners about resource {}: {}",
                    resource != null ? resource.getId() : null, e.getMessage());
        }
    }

    private void notifyIfBecameLearnerVisible(Resource.ResourceVisibility previousVisibility,
                                              Resource resource, UUID publisherId) {
        if (resource == null || resource.getVisibility() == null) {
            return;
        }
        boolean wasHidden = previousVisibility == null
                || "PRIVATE".equals(previousVisibility.name())
                || "DRAFT".equals(previousVisibility.name());
        boolean isHidden = "PRIVATE".equals(resource.getVisibility().name())
                || "DRAFT".equals(resource.getVisibility().name());
        if (wasHidden && !isHidden) {
            notifyResourcePublished(resource, publisherId);
        }
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
        // R8: legacy/system resources may carry uploaded_by = null; findById(null)
        // throws and 500s the whole listing, so guard before resolving the uploader.
        User uploader = resource.getUploadedBy() != null
                ? userRepository.findById(resource.getUploadedBy()).orElse(null)
                : null;
        return ResourceResponse.builder()
                .id(resource.getId())
                .institutionId(resource.getInstitutionId())
                .lessonId(resource.getLessonId())
                .moduleId(resource.getModuleId())
                .courseId(resource.getCourseId())
                .teacherAssignmentId(resource.getTeacherAssignmentId())
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