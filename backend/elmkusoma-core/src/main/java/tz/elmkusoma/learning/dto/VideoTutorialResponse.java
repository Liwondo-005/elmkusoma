package tz.elmkusoma.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTutorialResponse {
    private UUID id;
    private UUID institutionId;
    private UUID lessonId;
    private UUID moduleId;
    private UUID courseId;
    private String createdBy;
    private String createdByName;
    private String title;
    private String description;
    private Integer durationSeconds;
    private String recordingUrl;
    private String recordingObjectKey;
    private String recordingBucket;
    private String thumbnailUrl;
    private String captionUrl;
    private String status;
    private String processingError;
    private LocalDateTime processingStartedAt;
    private LocalDateTime processingCompletedAt;
    private String visibility;
    private Integer sortOrder;
    private Boolean isDownloadable;
    private Boolean isPreviewable;
    private String tags;
    private String metadata;
    private List<String> tagNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeletable;
    private Boolean isEditable;
    private Boolean isViewable;
    private Boolean isPlayable;
}