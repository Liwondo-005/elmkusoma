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

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "List media assets for institution")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> listMedia(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(required = false) String type) {

        List<MediaAsset> assets;
        if (type != null && !type.isEmpty()) {
            assets = mediaAssetRepository.findByInstitutionIdAndMediaTypeAndIsDeletedFalseOrderByCreatedAtDesc(institutionId, type);
        } else {
            assets = mediaAssetRepository.findByInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(institutionId);
        }

        List<MediaAssetResponse> response = assets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/recordings/{sourceType}/{sourceId}")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get recordings for a live class or event")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> getRecordings(
            @PathVariable String sourceType,
            @PathVariable UUID sourceId) {

        List<MediaAsset> recordings = mediaAssetRepository
                .findBySourceTypeAndSourceIdAndIsDeletedFalse(sourceType, sourceId);

        List<MediaAssetResponse> response = recordings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get media asset detail")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> getMedia(@PathVariable UUID id) {
        return mediaAssetRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .map(a -> ResponseEntity.ok(ApiResponse.success(toResponse(a))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Media asset not found")));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get teacher's media assets")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> myMedia(
            @RequestAttribute("userId") UUID userId) {

        List<MediaAsset> assets = mediaAssetRepository
                .findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        List<MediaAssetResponse> response = assets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Search media assets")
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> searchMedia(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam String q) {

        List<MediaAsset> assets = mediaAssetRepository
                .searchByTitle(institutionId, q);

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
                .subjectId(asset.getSubjectId())
                .tags(asset.getTags())
                .createdAt(asset.getCreatedAt())
                .build();
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Upload a media file via the media service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadMedia(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = mediaProxyService.uploadFile(file, institutionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("File uploaded successfully", result));
    }

    @PostMapping("/presigned-upload")
    @PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get a presigned URL for direct upload to object storage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPresignedUploadUrl(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestBody Map<String, String> request) {
        Map<String, Object> result = mediaProxyService.getPresignedUploadUrl(
            request.get("fileName"), request.get("contentType"), institutionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/download-url")
    @PreAuthorize("hasAnyRole('TEACHER','OTHER_LEARNER','INSTITUTION_ADMIN')")
    @Operation(summary = "Get a presigned download URL for a media asset")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDownloadUrl(@PathVariable UUID id) {
        Map<String, Object> result = mediaProxyService.getDownloadUrl(id.toString());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
