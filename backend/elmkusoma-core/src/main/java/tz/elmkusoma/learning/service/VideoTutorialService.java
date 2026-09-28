package tz.elmkusoma.learning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.VideoTutorial;
import tz.elmkusoma.learning.domain.VideoTutorialProgress;
import tz.elmkusoma.learning.dto.VideoTutorialRequest;
import tz.elmkusoma.learning.dto.VideoTutorialResponse;
import tz.elmkusoma.learning.dto.VideoTutorialProgressResponse;
import tz.elmkusoma.learning.repository.VideoTutorialRepository;
import tz.elmkusoma.learning.repository.VideoTutorialProgressRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.liveclass.service.MediaProxyService;
import java.util.Map;

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
    private final MediaProxyService mediaProxyService;
    private final MediaAssetRepository mediaAssetRepository;

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

    // Attach a live recording (MediaAsset) to the video tutorial library
    @Transactional
    public VideoTutorialResponse attachRecording(tz.elmkusoma.liveclass.domain.MediaAsset asset,
                                                 UUID institutionId, String userId) {
        String recordingUrl = asset.getFileUrl();
        if (recordingUrl != null && !recordingUrl.isBlank()
                && !videoTutorialRepository
                        .findByRecordingUrlAndInstitutionIdAndIsDeletedFalse(recordingUrl, institutionId)
                        .isEmpty()) {
            throw new IllegalStateException("Recording already attached to the video library");
        }

        VideoTutorial video = VideoTutorial.builder()
                .institutionId(institutionId)
                .createdBy(userId)
                .title(asset.getTitle())
                .description(asset.getDescription())
                .durationSeconds(asset.getDurationSeconds() != null ? asset.getDurationSeconds().intValue() : null)
                .recordingUrl(recordingUrl)
                .thumbnailUrl(asset.getThumbnailUrl())
                .visibility(VideoTutorial.ResourceVisibility.DRAFT)
                .status(VideoTutorial.VideoTutorialStatus.READY)
                .sortOrder(0)
                .isDownloadable(true)
                .isPreviewable(true)
                .build();

        video = videoTutorialRepository.save(video);
        log.info("Attached recording {} to video library as {} for institution {}",
                asset.getId(), video.getId(), institutionId);
        return mapToResponse(video);
    }

    // Get Video Tutorial
    @Transactional(readOnly = true)
    public VideoTutorialResponse getVideoTutorial(UUID videoTutorialId, UUID institutionId, String userId, String userRole) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Video tutorial not found"));

        if (!canAccessVideo(video, userId, userRole)) {
            throw new SecurityException("You are not allowed to view this video tutorial");
        }

        return mapToResponse(video);
    }

    // List Video Tutorials
    @Transactional(readOnly = true)
    public List<VideoTutorialResponse> listVideoTutorials(UUID institutionId, String userId, String userRole,
                                                          UUID lessonId, UUID moduleId, UUID courseId,
                                                          String status, String visibility,
                                                          int page, int size) {
        List<String> allowedVisibilities = getAllowedVisibilities(userRole);

        if (lessonId != null) {
            return videoTutorialRepository.findVisibleByLessonId(lessonId, allowedVisibilities)
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        if (moduleId != null) {
            return videoTutorialRepository.findVisibleByModuleId(moduleId, allowedVisibilities)
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        if (courseId != null) {
            return videoTutorialRepository.findVisibleByCourseId(courseId, allowedVisibilities)
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
                                                      UUID institutionId, String userId, String userRole) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Video tutorial not found"));
        assertCanManage(video, institutionId, userId, userRole);

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
    public void deleteVideoTutorial(UUID videoTutorialId, UUID institutionId, String userId, String userRole) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Video tutorial not found"));
        assertCanManage(video, institutionId, userId, userRole);

        video.setIsDeleted(true);
        videoTutorialRepository.save(video);
        log.info("Deleted video tutorial: {}", videoTutorialId);
    }

    // Publish Video Tutorial
    @Transactional
    public VideoTutorialResponse publishVideoTutorial(UUID videoTutorialId, UUID institutionId, String userId, String userRole) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Video tutorial not found"));
        assertCanManage(video, institutionId, userId, userRole);

        if (!video.isReady()) {
            throw new IllegalStateException("Cannot publish video tutorial that is not READY");
        }

        video.setVisibility(VideoTutorial.ResourceVisibility.PUBLIC);
        video = videoTutorialRepository.save(video);
        return mapToResponse(video);
    }

    // Unpublish Video Tutorial
    // Unpublish Video Tutorial
    @Transactional
    public VideoTutorialResponse unpublishVideoTutorial(UUID videoTutorialId, UUID institutionId, String userId, String userRole) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Video tutorial not found"));
        assertCanManage(video, institutionId, userId, userRole);

        video.setVisibility(VideoTutorial.ResourceVisibility.DRAFT);
        video = videoTutorialRepository.save(video);
        return mapToResponse(video);
    }

    // Upload Video - bytes are stored by the media service (MinIO), never discarded
    @Transactional
    public VideoTutorialResponse uploadVideo(UUID videoTutorialId, MultipartFile file, UUID institutionId,
                                             String userId, String userRole, String bearerToken) throws IOException {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> v.getInstitutionId().equals(institutionId))
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Video tutorial not found"));
        assertCanManage(video, institutionId, userId, userRole);

        video.setStatus(VideoTutorial.VideoTutorialStatus.PROCESSING);
        video.setProcessingStartedAt(LocalDateTime.now());
        video.setProcessingError(null);
        video = videoTutorialRepository.save(video);

        try {
            Map<String, Object> stored = mediaProxyService.uploadFile(file, bearerToken);
            video.setRecordingUrl(stored.get("url") != null ? String.valueOf(stored.get("url")) : file.getOriginalFilename());
            if (stored.get("objectKey") != null) {
                video.setRecordingObjectKey(String.valueOf(stored.get("objectKey")));
            }
            video.setStatus(VideoTutorial.VideoTutorialStatus.READY);
            video.setProcessingCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Video upload failed for {}: {}", videoTutorialId, e.getMessage());
            video.setStatus(VideoTutorial.VideoTutorialStatus.FAILED);
            video.setProcessingError(e.getMessage());
            videoTutorialRepository.save(video);
            throw new IOException("Video upload failed: " + e.getMessage(), e);
        }

        video = videoTutorialRepository.save(video);
        return mapToResponse(video);
    }

    // Get Presigned Upload URL (media service, authenticated as the caller)
    public String getPresignedUploadUrl(UUID institutionId, String fileName, String contentType, String userId, String bearerToken) {
        Map<String, Object> result = mediaProxyService.getPresignedUploadUrl(fileName, contentType, bearerToken);
        Object uploadUrl = result != null ? result.get("uploadUrl") : null;
        return uploadUrl != null ? String.valueOf(uploadUrl) : "/api/v1/media/presigned-upload";
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
                .orElseThrow(() -> new ResourceNotFoundException("Video tutorial not found"));

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
    private boolean canAccessVideo(VideoTutorial video, String userId, String userRole) {
        if (isAdminRole(userRole)) {
            return true;
        }
        VideoTutorial.ResourceVisibility visibility = video.getVisibility();
        if (visibility == VideoTutorial.ResourceVisibility.PUBLIC
                || visibility == VideoTutorial.ResourceVisibility.INSTITUTION
                || visibility == VideoTutorial.ResourceVisibility.SCHOOL
                || visibility == VideoTutorial.ResourceVisibility.CLASS_ONLY
                || visibility == VideoTutorial.ResourceVisibility.COURSE_ONLY) {
            return true;
        }
        // PRIVATE / DRAFT: creator or admin only
        return isCreator(video, userId);
    }

    /**
     * Ownership compare must tolerate both possible owner values: the userId the
     * service assigned at build time and the email Spring Data auditing writes
     * into {@code created_by} on persist.
     */
    private boolean isCreator(VideoTutorial video, String userId) {
        String owner = video.getCreatedBy();
        if (owner == null || "system".equals(owner)) {
            // Unknown owner: fail closed, admins already returned above.
            return false;
        }
        if (userId != null && owner.equals(userId)) {
            return true;
        }
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return authentication != null && owner.equalsIgnoreCase(authentication.getName());
    }

    private static boolean isAdminRole(String userRole) {
        return "ADMIN".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole) || "NATIONAL_ADMIN".equals(userRole);
    }

    /** Institution + ownership enforcement for mutating operations. */
    private void assertCanManage(VideoTutorial video, UUID institutionId, String userId, String userRole) {
        if (institutionId == null || !institutionId.equals(video.getInstitutionId())) {
            throw new ResourceNotFoundException("Video tutorial not found");
        }
        if (isAdminRole(userRole)) {
            return;
        }
        if (!isCreator(video, userId)) {
            throw new SecurityException("You do not own this video tutorial");
        }
    }

    private List<String> getAllowedVisibilities(String userRole) {
        if ("ADMIN".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole)
                || "NATIONAL_ADMIN".equals(userRole) || "TEACHER".equals(userRole)
                || "INSTRUCTOR".equals(userRole) || "LECTURER".equals(userRole)) {
            return List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
        } else {
            return List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION");
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