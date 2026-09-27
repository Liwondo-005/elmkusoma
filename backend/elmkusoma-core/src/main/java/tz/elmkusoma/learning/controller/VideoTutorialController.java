package tz.elmkusoma.learning.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.learning.dto.VideoTutorialRequest;
import tz.elmkusoma.learning.dto.VideoTutorialResponse;
import tz.elmkusoma.learning.dto.VideoTutorialProgressResponse;
import tz.elmkusoma.learning.service.VideoTutorialService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/video-tutorials")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
@Tag(name = "Video Tutorial Management", description = "Video tutorial management for learning content")
public class VideoTutorialController {

    private final VideoTutorialService videoTutorialService;
    private final tz.elmkusoma.learning.service.ResourceAnalyticsService resourceAnalyticsService;

    @PostMapping
    @Operation(summary = "Create a new video tutorial")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<VideoTutorialResponse>> createVideoTutorial(
            @Valid @RequestBody VideoTutorialRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") String userId) {
        VideoTutorialResponse response = videoTutorialService.createVideoTutorial(request, institutionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Video tutorial created successfully", response));
    }

    @GetMapping("/{videoTutorialId}")
    @Operation(summary = "Get a specific video tutorial")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<VideoTutorialResponse>> getVideoTutorial(
            @PathVariable UUID videoTutorialId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") String userId,
            @RequestAttribute("userRole") String userRole) {
        VideoTutorialResponse response = videoTutorialService.getVideoTutorial(videoTutorialId, institutionId, userId, userRole);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List video tutorials")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<VideoTutorialResponse>>> listVideoTutorials(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") String userId,
            @RequestAttribute("userRole") String userRole,
            @RequestParam(required = false) UUID lessonId,
            @RequestParam(required = false) UUID moduleId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<VideoTutorialResponse> tutorials = videoTutorialService.listVideoTutorials(
                institutionId, userId, userRole, lessonId, moduleId, courseId, status, visibility, page, size);
        return ResponseEntity.ok(ApiResponse.success(tutorials));
    }

    @PutMapping("/{videoTutorialId}")
    @Operation(summary = "Update a video tutorial")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<VideoTutorialResponse>> updateVideoTutorial(
            @PathVariable UUID videoTutorialId,
            @Valid @RequestBody VideoTutorialRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") String userId) {
        VideoTutorialResponse response = videoTutorialService.updateVideoTutorial(videoTutorialId, request, institutionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Video tutorial updated successfully", response));
    }

    @DeleteMapping("/{videoTutorialId}")
    @Operation(summary = "Delete a video tutorial")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteVideoTutorial(
            @PathVariable UUID videoTutorialId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") String userId) {
        videoTutorialService.deleteVideoTutorial(videoTutorialId, institutionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Video tutorial deleted successfully", null));
    }

    @PostMapping("/{videoTutorialId}/publish")
    @Operation(summary = "Publish a video tutorial")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<VideoTutorialResponse>> publishVideoTutorial(
            @PathVariable UUID videoTutorialId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") String userId) {
        VideoTutorialResponse response = videoTutorialService.publishVideoTutorial(videoTutorialId, institutionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Video tutorial published successfully", response));
    }

    @PostMapping("/{videoTutorialId}/unpublish")
    @Operation(summary = "Unpublish a video tutorial")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<VideoTutorialResponse>> unpublishVideoTutorial(
            @PathVariable UUID videoTutorialId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") String userId) {
        VideoTutorialResponse response = videoTutorialService.unpublishVideoTutorial(videoTutorialId, institutionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Video tutorial unpublished successfully", response));
    }

    @PostMapping("/{videoTutorialId}/upload")
    @Operation(summary = "Upload video file for a tutorial")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<VideoTutorialResponse>> uploadVideo(
            @PathVariable UUID videoTutorialId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") String userId) throws Exception {
        VideoTutorialResponse response = videoTutorialService.uploadVideo(videoTutorialId, file, institutionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Video uploaded successfully, processing started", response));
    }

    @GetMapping("/{videoTutorialId}/presigned-upload")
    @Operation(summary = "Get presigned upload URL for video")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<String>> getPresignedUploadUrl(
            @PathVariable UUID videoTutorialId,
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") String userId) {
        String uploadUrl = videoTutorialService.getPresignedUploadUrl(institutionId, fileName, contentType, userId);
        return ResponseEntity.ok(ApiResponse.success(uploadUrl));
    }

    @GetMapping("/{videoTutorialId}/progress")
    @Operation(summary = "Get student's progress for a video tutorial")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<VideoTutorialProgressResponse>> getProgress(
            @PathVariable UUID videoTutorialId,
            @RequestAttribute("userId") UUID userId) {
        VideoTutorialProgressResponse response = videoTutorialService.getProgress(videoTutorialId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{videoTutorialId}/progress")
    @Operation(summary = "Update student's progress for a video tutorial")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<VideoTutorialProgressResponse>> updateProgress(
            @PathVariable UUID videoTutorialId,
            @RequestParam int positionSeconds,
            @RequestParam(required = false) Integer durationSeconds,
            @RequestParam(required = false) Boolean completed,
            @RequestAttribute("userId") UUID userId) {
        VideoTutorialProgressResponse response = videoTutorialService.updateProgress(
                videoTutorialId, userId, positionSeconds, durationSeconds, completed);
        return ResponseEntity.ok(ApiResponse.success("Progress updated successfully", response));
    }

    @GetMapping("/progress")
    @Operation(summary = "Get student's progress for all video tutorials")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<VideoTutorialProgressResponse>>> getStudentProgress(
            @RequestAttribute("userId") UUID userId) {
        List<VideoTutorialProgressResponse> progress = videoTutorialService.getStudentProgress(userId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    @GetMapping("/{videoTutorialId}/analytics")
    @Operation(summary = "Get analytics for a video tutorial")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<tz.elmkusoma.learning.dto.VideoTutorialAnalyticsResponse>> getAnalytics(
            @PathVariable UUID videoTutorialId,
            @RequestAttribute("institutionId") UUID institutionId) {
        tz.elmkusoma.learning.dto.VideoTutorialAnalyticsResponse analytics =
                resourceAnalyticsService.getVideoAnalytics(videoTutorialId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}