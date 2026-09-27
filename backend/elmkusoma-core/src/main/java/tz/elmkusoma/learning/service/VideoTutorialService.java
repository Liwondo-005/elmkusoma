package tz.elmkusoma.learning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.learning.domain.VideoTutorial;
import tz.elmkusoma.learning.domain.VideoTutorialProgress;
import tz.elmkusoma.learning.dto.VideoTutorialRequest;
import tz.elmkusoma.learning.dto.VideoTutorialResponse;
import tz.elmkusoma.learning.dto.VideoTutorialProgressResponse;
import tz.elmkusoma.learning.repository.VideoTutorialRepository;
import tz.elmkusoma.learning.repository.VideoTutorialProgressRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTutorialService {

    private final VideoTutorialRepository videoTutorialRepository;
    private final VideoTutorialProgressRepository progressRepository;
    private final UserRepository userRepository;

    private static final List<String> STUDENT_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION");
    private static final List<String> TEACHER_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
    private static final List<String> ADMIN_VISIBILITIES = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");

    // Create Video Tutorial
    @Transactional
    public VideoTutorialResponse createVideoTutorial(VideoTutorialRequest request, UUID institutionId, String userId) {
        log.info("Creating video tutorial: {} for institution: {}", request.getTitle(), institutionId);

        VideoTutorial video = VideoTutorial.builder()
                .institutionId(institutionId)
                .lessonId(request.getLessonId())
                .moduleId(request.getModuleId())
                .courseId(request.getCourseId())
                .createdBy(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .durationSeconds(request.getDurationSeconds())
                .recordingUrl(request.getRecordingUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .captionUrl(request.getCaptionUrl())
                .visibility(VideoTutorial.ResourceVisibility.valueOf(request.getVisibility()))
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isDownloadable(request.getIsDownloadable() != null ? request.getIsDownloadable() : true)
                .isPreviewable(request.getIsPreviewable() != null ? request.getIsPreviewable() : true)
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .status(VideoTutorial.VideoTutorialStatus.DRAFT)
                .build();

        video = videoTutorialRepository.save(video);
        log.info("Created video tutorial: {} for institution: {}", video.getId(), institutionId);
        return mapToResponse(video);
    }

    // Get Video Tutorial
    @Transactional(readOnly = true)
    public VideoTutorialResponse getVideoTutorial(UUID videoTutorialId, UUID institutionId, String userId, String userRole) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Video tutorial not found"));

        if (!canAccessVideo(video, userId)) {
            throw new RuntimeException("Access denied to video tutorial");
        }

        return mapToResponse(video);
    }

    // List Video Tutorials
    @Transactional(readOnly = true)
    public List<VideoTutorialResponse> listVideoTutorials(UUID institutionId, String userId, String userRole,
                                                          UUID lessonId, UUID moduleId, UUID courseId,
                                                          String status, String visibility,
                                                          int page, int size) {
        List<String> allowedVisibilities = getAllowedVisibilities("TEACHER");

        if (lessonId != null) {
            return videoTutorialRepository.findVisibleByLessonId(lessonId, getAllowedVisibilities("TEACHER"))
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        if (moduleId != null) {
            return videoTutorialRepository.findVisibleByModuleId(moduleId, getAllowedVisibilities("TEACHER"))
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        if (courseId != null) {
            return videoTutorialRepository.findVisibleByCourseId(courseId, getAllowedVisibilities("TEACHER"))
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        List<VideoTutorial> videos = videoTutorialRepository.findByInstitutionIdAndVisibilities(institutionId, allowedVisibilities);
        return videos.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Update Video Tutorial
    @Transactional
public VideoTutorialResponse updateVideoTutorial(UUID videoTutorialId, VideoTutorialRequest request,
                                                      UUID institutionId, String userId) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Video tutorial not found"));

        if (request.getTitle() != null) video.setTitle(request.getTitle());
        if (request.getDescription() != null) video.setDescription(request.getDescription());
        if (request.getDurationSeconds() != null) video.setDurationSeconds(request.getDurationSeconds());
        if (request.getRecordingUrl() != null) video.setRecordingUrl(request.getRecordingUrl());
        if (request.getThumbnailUrl() != null) video.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getCaptionUrl() != null) video.setCaptionUrl(request.getCaptionUrl());
        if (request.getVisibility() != null) video.setVisibility(VideoTutorial.ResourceVisibility.valueOf(request.getVisibility()));
        if (request.getSortOrder() != null) video.setSortOrder(request.getSortOrder());
        if (request.getIsDownloadable() != null) video.setIsDownloadable(request.getIsDownloadable());
        if (request.getIsPreviewable() != null) video.setIsPreviewable(request.getIsPreviewable());
        if (request.getTags() != null) video.setTags(request.getTags());
        if (request.getMetadata() != null) video.setMetadata(request.getMetadata());

        video = videoTutorialRepository.save(video);
        return mapToResponse(video);
    }

    // Delete Video Tutorial
    @Transactional
    public void deleteVideoTutorial(UUID videoTutorialId, UUID institutionId, String userId) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Video tutorial not found"));

        video.setIsDeleted(true);
        videoTutorialRepository.save(video);
        log.info("Deleted video tutorial: {}", videoTutorialId);
    }

    // Publish Video Tutorial
    @Transactional
    public VideoTutorialResponse publishVideoTutorial(UUID videoTutorialId, UUID institutionId, String userId) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Video tutorial not found"));

        if (!video.isReady()) {
            throw new RuntimeException("Cannot publish video tutorial that is not READY");
        }

        video.setVisibility(VideoTutorial.ResourceVisibility.PUBLIC);
        video = videoTutorialRepository.save(video);
        return mapToResponse(video);
    }

    // Unpublish Video Tutorial
    // Unpublish Video Tutorial
    @Transactional
    public VideoTutorialResponse unpublishVideoTutorial(UUID videoTutorialId, UUID institutionId, String userId) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Video tutorial not found"));

        video.setVisibility(VideoTutorial.ResourceVisibility.DRAFT);
        video = videoTutorialRepository.save(video);
        return mapToResponse(video);
    }

    // Upload Video - simplified version without media service dependency
    @Transactional
    public VideoTutorialResponse uploadVideo(UUID videoTutorialId, MultipartFile file, UUID institutionId, String userId) throws IOException {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Video tutorial not found"));

        // Note: In production, this would upload to MinIO/S3 via the media service
        // For now, we'll just update the status to PROCESSING
        video.setStatus(VideoTutorial.VideoTutorialStatus.PROCESSING);
        video.setProcessingStartedAt(LocalDateTime.now());

        video = videoTutorialRepository.save(video);

        // Process video asynchronously (would typically be async)
        processVideoAsync(video.getId(), file);

        return mapToResponse(video);
    }

    // Get Presigned Upload URL
    public String getPresignedUploadUrl(UUID institutionId, String fileName, String contentType, String userId) {
        // Note: In production, this would generate a presigned URL via the media service
        // For now, return placeholder
        return "/api/v1/media/presigned-upload";
    }

    // Student Progress
    @Transactional(readOnly = true)
    public VideoTutorialProgressResponse getProgress(UUID videoTutorialId, UUID studentId) {
        return progressRepository.findByVideoTutorialIdAndStudentId(videoTutorialId, studentId)
                .map(this::mapToProgressResponse)
                .orElse(VideoTutorialProgressResponse.builder()
                        .videoTutorialId(videoTutorialId)
                        .studentId(studentId)
                        .positionSeconds(0)
                        .completed(false)
                        .completionPercentage(0.0)
                        .build());
    }

    @Transactional
    public VideoTutorialProgressResponse updateProgress(UUID videoTutorialId, UUID studentId,
                                                         int positionSeconds, Integer durationSeconds,
                                                         Boolean completed) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .orElseThrow(() -> new RuntimeException("Video tutorial not found"));

        VideoTutorialProgress progress = progressRepository.findByVideoTutorialIdAndStudentId(videoTutorialId, studentId)
                .orElseGet(() -> VideoTutorialProgress.builder()
                        .videoTutorialId(videoTutorialId)
                        .studentId(studentId)
                        .build());

        progress.updateProgress(positionSeconds, durationSeconds != null ? durationSeconds : video.getDurationSeconds());
        if (completed != null) {
            progress.setCompleted(completed);
            if (completed && progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }
        }

        progress = progressRepository.save(progress);
        return mapToProgressResponse(progress);
    }

    @Transactional(readOnly = true)
    public List<VideoTutorialProgressResponse> getStudentProgress(UUID studentId) {
        return progressRepository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                .map(this::mapToProgressResponse)
                .collect(Collectors.toList());
    }

    // Helper Methods
    private boolean canAccessVideo(VideoTutorial video, String userId) {
        // TODO: Implement proper access check based on role, visibility, enrollment
        return true; // Simplified for now
    }

    private List<String> getAllowedVisibilities(String userRole) {
        if ("ADMIN".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole)) {
            return List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
        } else if ("TEACHER".equals(userRole)) {
            return List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
        } else {
            return List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION");
        }
    }



    private void processVideoAsync(UUID videoTutorialId, MultipartFile file) {
        // TODO: Implement async video processing (transcoding, thumbnail generation, caption extraction)
        // For now, mark as READY
        try {
            VideoTutorial video = videoTutorialRepository.findById(videoTutorialId).orElseThrow();
            video.setStatus(VideoTutorial.VideoTutorialStatus.READY);
            video.setProcessingCompletedAt(LocalDateTime.now());
            videoTutorialRepository.save(video);
        } catch (Exception e) {
            log.error("Video processing failed for {}: {}", videoTutorialId, e.getMessage());
        }
    }



    private VideoTutorialResponse mapToResponse(VideoTutorial video) {
        
        
            
        
            
        
        User creator = video.getCreatedBy() != null ? userRepository.findByEmailAndIsDeletedFalse(video.getCreatedBy()).orElse(null) : null;
        return VideoTutorialResponse.builder()
                .id(video.getId())
                .institutionId(video.getInstitutionId())
                .lessonId(video.getLessonId())
                .moduleId(video.getModuleId())
                .courseId(video.getCourseId())
                .createdBy(video.getCreatedBy())
                .createdByName(creator != null ? creator.getFirstName() + " " + creator.getLastName() : "Unknown")
                .title(video.getTitle())
                .description(video.getDescription())
                .durationSeconds(video.getDurationSeconds())
                .recordingUrl(video.getRecordingUrl())
                .recordingObjectKey(video.getRecordingObjectKey())
                .recordingBucket(video.getRecordingBucket())
                .thumbnailUrl(video.getThumbnailUrl())
                .captionUrl(video.getCaptionUrl())
                .durationSeconds(video.getDurationSeconds())
                .status(video.getStatus().name())
                .processingError(video.getProcessingError())
                .processingStartedAt(video.getProcessingStartedAt())
                .processingCompletedAt(video.getProcessingCompletedAt())
                .visibility(video.getVisibility().name())
                .sortOrder(video.getSortOrder())
                .isDownloadable(video.getIsDownloadable())
                .isPreviewable(video.getIsPreviewable())
                .tags(video.getTags())
                .metadata(video.getMetadata())
                .createdByName(video.getCreatedBy() != null ?
                        (userRepository.findByEmailAndIsDeletedFalse(video.getCreatedBy()).map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown")) : "Unknown")
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .isDeletable(true)
                .isEditable(true)
                .isViewable(true)
                .isPlayable(video.isPlayable())
                .build();
    }

    private VideoTutorialProgressResponse mapToProgressResponse(VideoTutorialProgress progress) {
        return VideoTutorialProgressResponse.builder()
                .id(progress.getId())
                .videoTutorialId(progress.getVideoTutorialId())
                .studentId(progress.getStudentId())
                .positionSeconds(progress.getPositionSeconds())
                .completed(progress.getCompleted())
                .completionPercentage(progress.getCompletionPercentage())
                .lastWatchedAt(progress.getLastWatchedAt())
                .completedAt(progress.getCompletedAt())
                .watchCount(progress.getWatchCount())
                .totalWatchTimeSeconds(progress.getTotalWatchTimeSeconds())
                .lastPositionSeconds(progress.getLastPositionSeconds())
                .completedAt(progress.getCompletedAt())
                .createdAt(progress.getCreatedAt())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}