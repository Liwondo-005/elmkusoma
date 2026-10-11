package tz.elmkusoma.liveclass.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.dto.MediaAssetResponse;
import tz.elmkusoma.liveclass.dto.MediaAssetRequest;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.liveclass.service.MediaProxyService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Library", description = "Media assets for live classes and recordings")
public class MediaLibraryController {

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaProxyService mediaProxyService;
    private final tz.elmkusoma.shared.repository.InstitutionMembershipRepository membershipRepository;
    private final tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository participantRepository;
    private final tz.elmkusoma.course.repository.LiveClassRepository liveClassRepository;

    /**
     * Records whose class-level target must be enforced rather than merely filtered in the UI.
     *
     * <p>Recording assets are learner-facing recordings of a specific class. Institution
     * membership alone is not sufficient: before this check any student in the institution
     * could list and download every other class's recordings. The rule mirrors the one the
     * Replay Library already enforces in {@code LearnerReplayController.hasReplayEntitlement}
     * - participation in the recorded session - which is the repository's authoritative
     * answer to "who may see a recording".</p>
     *
     * <p>Staff keep their existing access: institution admins (and platform admins) see the
     * whole institutional library, and a teacher sees recordings of their own live classes.</p>
     */
    private boolean canViewAsset(MediaAsset asset, UUID callerUserId, UUID institutionId, String userRole) {
        if (isInstitutionScopedAdmin(userRole)) {
            return true;
        }
        // Institution boundary first: a recording never crosses tenants.
        if (institutionId == null || !institutionId.equals(asset.getInstitutionId())) {
            return false;
        }
        if (!"RECORDING".equalsIgnoreCase(asset.getMediaType())) {
            return true;
        }
        if (callerUserId == null) {
            return false;
        }
        // The teacher's own recording.
        if (callerUserId.equals(asset.getTeacherId())) {
            return true;
        }
        // Recordings originate from a live class; entitlement follows live class participation,
        // exactly as for the Replay Library.
        if ("LIVE_CLASS".equalsIgnoreCase(asset.getSourceType()) && asset.getSourceId() != null) {
            return participantRepository
                    .existsByLiveClassIdAndUserIdAndIsDeletedFalse(asset.getSourceId(), callerUserId);
        }
        // A recording with no live-class origin has no participation evidence: deny rather
        // than expose institution-wide, matching Replay's default-deny rule.
        return false;
    }

    private static boolean isInstitutionScopedAdmin(String userRole) {
        return "ADMIN".equals(userRole)
                || "NATIONAL_ADMIN".equals(userRole)
                || "INSTITUTION_ADMIN".equals(userRole);
    }

    /**
     * Origin keys that only server-side code may write.
     *
     * <p>Case-insensitive to match {@link #canViewAsset}, which resolves replay entitlement with
     * {@code "LIVE_CLASS".equalsIgnoreCase(asset.getSourceType())} - a caller must not be able to
     * slip past the guard with {@code live_class}.
     */
    private static boolean isReservedOrigin(String sourceType) {
        return sourceType != null && "LIVE_CLASS".equalsIgnoreCase(sourceType.trim());
    }

    /** Guards the class/recording listing used by the teacher media pages. */
    private boolean isMemberOfInstitution(UUID callerUserId, UUID institutionId) {
        return callerUserId != null && institutionId != null
                && membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(callerUserId, institutionId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "List media assets for institution")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> listMedia(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestParam(required = false) String type) {

        // The tenant must come from the server-derived attribute, and the caller must
        // actually belong to it. Without this check any authenticated user could set
        // X-Institution-Id to another tenant and read that tenant's media library.
        if (!isMemberOfInstitution(userId, institutionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this institution"));
        }

        List<MediaAsset> assets;
        if (type != null && !type.isEmpty()) {
            assets = mediaAssetRepository.findByInstitutionIdAndMediaTypeAndIsDeletedFalseOrderByCreatedAtDesc(institutionId, type);
        } else {
            assets = mediaAssetRepository.findByInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(institutionId);
        }

        // Recordings are filtered by entitlement server-side. Filtering in the UI is not
        // authorization, and without this every student would see every class's recording.
        List<MediaAssetResponse> response = assets.stream()
                .filter(a -> canViewAsset(a, userId, institutionId, userRole))
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/recordings/{sourceType}/{sourceId}")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get recordings for a live class or event")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> getRecordings(
            @PathVariable String sourceType,
            @PathVariable UUID sourceId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {

        if (!isMemberOfInstitution(userId, institutionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this institution"));
        }

        // Entitlement is per asset, not just per tenant: a learner may open the recordings of
        // sessions they took part in and must not enumerate another class's recordings by id.
        List<MediaAssetResponse> response = mediaAssetRepository
                .findBySourceTypeAndSourceIdAndIsDeletedFalse(sourceType, sourceId)
                .stream()
                .filter(a -> canViewAsset(a, userId, institutionId, userRole))
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/class/{classGroupId}")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get recording assets targeted at a class group")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> getRecordingsByClass(
            @PathVariable UUID classGroupId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {

        if (!isMemberOfInstitution(userId, institutionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this institution"));
        }

        // The class id is only a selector; the asset's own institution and entitlement are
        // still enforced, so a guessed class id cannot widen access.
        List<MediaAssetResponse> response = mediaAssetRepository
                .findByInstitutionIdAndClassGroupIdAndMediaTypeAndIsDeletedFalse(
                        institutionId, classGroupId, "RECORDING")
                .stream()
                .filter(a -> canViewAsset(a, userId, institutionId, userRole))
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get media asset detail")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> getMedia(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        MediaAsset asset = mediaAssetRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElse(null);
        if (asset == null || !canViewAsset(asset, userId, institutionId, userRole)) {
            // Same response as "missing" so this endpoint cannot be used to probe which
            // recordings exist in another class or another institution.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Media asset not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(toResponse(asset)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get teacher's media assets")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> myMedia(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {

        if (institutionId == null) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        List<MediaAsset> assets = mediaAssetRepository
                .findByTeacherIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        userId, institutionId);

        List<MediaAssetResponse> response = assets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Search media assets")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> searchMedia(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestParam String q) {

        if (!isMemberOfInstitution(userId, institutionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this institution"));
        }

        // Search must not become a side channel around the entitlement check on listing.
        List<MediaAsset> assets = mediaAssetRepository
                .searchByTitle(institutionId, q).stream()
                .filter(a -> canViewAsset(a, userId, institutionId, userRole))
                .toList();

        List<MediaAssetResponse> response = assets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Create a media asset")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> createMedia(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestBody MediaAssetRequest request) {

        // LIVE_CLASS is a reserved origin: it is written only by the recording hand-off
        // (LiveRecordingMediaPublisher on the webhook path, publishRecordingToMediaLibrary on the
        // synchronous finalize path) and it is what canViewAsset uses to derive replay
        // entitlement. Letting a request claim it would let anyone attach an arbitrary file to
        // someone else's session and - with V150's unique (source_type, source_id) key - squat the
        // slot so the genuine recording can never be published.
        if (isReservedOrigin(request.getSourceType())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("sourceType LIVE_CLASS is set by the server, not the client"));
        }

        MediaAsset asset = MediaAsset.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .mediaType(request.getMediaType())
                .fileUrl(request.getFileUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .durationSeconds(request.getDurationSeconds())
                .fileSizeBytes(request.getFileSizeBytes())
                .mimeType(request.getMimeType())
                .status(request.getStatus() != null ? request.getStatus() : "READY")
                .visibility(request.getVisibility() != null ? request.getVisibility() : "INSTITUTION")
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .teacherId(userId)
                .courseId(request.getCourseId())
                .classGroupId(request.getClassGroupId())
                .subjectId(request.getSubjectId())
                .tags(request.getTags())
                .build();
        asset.setInstitutionId(institutionId);
        asset = mediaAssetRepository.save(asset);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Media asset created", toResponse(asset)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Update a media asset")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> updateMedia(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id,
            @RequestBody MediaAssetRequest request) {

        MediaAsset asset = mediaAssetRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .filter(a -> institutionId.equals(a.getInstitutionId()))
                .orElse(null);
        if (asset == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Media asset not found"));
        }

        if (request.getTitle() != null) asset.setTitle(request.getTitle());
        if (request.getDescription() != null) asset.setDescription(request.getDescription());
        if (request.getMediaType() != null) asset.setMediaType(request.getMediaType());
        if (request.getFileUrl() != null) asset.setFileUrl(request.getFileUrl());
        if (request.getThumbnailUrl() != null) asset.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getDurationSeconds() != null) asset.setDurationSeconds(request.getDurationSeconds());
        if (request.getFileSizeBytes() != null) asset.setFileSizeBytes(request.getFileSizeBytes());
        if (request.getMimeType() != null) asset.setMimeType(request.getMimeType());
        if (request.getStatus() != null) asset.setStatus(request.getStatus());
        if (request.getVisibility() != null) asset.setVisibility(request.getVisibility());
        if (request.getTags() != null) asset.setTags(request.getTags());

        asset = mediaAssetRepository.save(asset);
        return ResponseEntity.ok(ApiResponse.success("Media asset updated", toResponse(asset)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Delete a media asset (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {

        MediaAsset asset = mediaAssetRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .filter(a -> institutionId.equals(a.getInstitutionId()))
                .orElse(null);
        if (asset == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Media asset not found"));
        }

        asset.setIsDeleted(true);
        mediaAssetRepository.save(asset);
        return ResponseEntity.ok(ApiResponse.success("Media asset deleted", null));
    }

    private MediaAssetResponse toResponse(MediaAsset asset) {
        return MediaAssetResponse.builder()
                .id(asset.getId())
                .title(asset.getTitle())
                .description(asset.getDescription())
                .mediaType(asset.getMediaType())
                .fileUrl(asset.getFileUrl())
                .thumbnailUrl(asset.getThumbnailUrl())
                .durationSeconds(asset.getDurationSeconds())
                .fileSizeBytes(asset.getFileSizeBytes())
                .mimeType(asset.getMimeType())
                .status(asset.getStatus())
                .visibility(asset.getVisibility())
                .sourceType(asset.getSourceType())
                .sourceId(asset.getSourceId())
                .teacherId(asset.getTeacherId())
                .courseId(asset.getCourseId())
                .classGroupId(asset.getClassGroupId())
                .subjectId(asset.getSubjectId())
                .tags(asset.getTags())
                .createdAt(asset.getCreatedAt())
                .build();
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Upload a media file via the media service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(value = "bearerToken", required = false) String bearerToken) {
        Map<String, Object> result = mediaProxyService.uploadFile(file, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("File uploaded successfully", result));
    }

    @PostMapping("/presigned-upload")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get a presigned URL for direct upload to object storage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPresignedUploadUrl(
            @RequestBody Map<String, String> request,
            @RequestAttribute(value = "bearerToken", required = false) String bearerToken) {
        Map<String, Object> result = mediaProxyService.getPresignedUploadUrl(
            request.get("fileName"), request.get("contentType"), bearerToken);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/download-url")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get a presigned download URL for a media asset")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDownloadUrl(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        // The download path must never be weaker than the detail path, or the entitlement
        // check on GET /v1/media/{id} would be bypassed by asking for the file directly.
        MediaAsset asset = mediaAssetRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElse(null);
        if (asset == null || !canViewAsset(asset, userId, institutionId, userRole)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Media asset not found"));
        }
        Map<String, Object> download = new java.util.HashMap<>();
        download.put("downloadUrl", asset.getFileUrl() != null ? asset.getFileUrl() : "");
        download.put("assetId", asset.getId());
        return ResponseEntity.ok(ApiResponse.success(download));
    }
}
