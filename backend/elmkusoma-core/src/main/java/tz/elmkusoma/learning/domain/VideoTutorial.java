package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_tutorials", indexes = {
    @Index(name = "idx_video_tutorials_institution", columnList = "institution_id"),
    @Index(name = "idx_video_tutorials_lesson", columnList = "lesson_id"),
    @Index(name = "idx_video_tutorials_module", columnList = "module_id"),
    @Index(name = "idx_video_tutorials_course", columnList = "course_id"),
    @Index(name = "idx_video_tutorials_status", columnList = "status"),
    @Index(name = "idx_video_tutorials_visibility", columnList = "visibility"),
    @Index(name = "idx_video_tutorials_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VideoTutorial extends BaseEntity {

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "module_id")
    private UUID moduleId;

    @Column(name = "course_id")
    private UUID courseId;


    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "recording_url", length = 1000)
    private String recordingUrl;

    @Column(name = "recording_object_key", length = 500)
    private String recordingObjectKey;

    @Column(name = "recording_bucket", length = 100)
    private String recordingBucket;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "thumbnail_object_key", length = 500)
    private String thumbnailObjectKey;

    @Column(name = "caption_url", length = 1000)
    private String captionUrl;

    @Column(name = "caption_object_key", length = 500)
    private String captionObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VideoTutorialStatus status = VideoTutorialStatus.DRAFT;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private ResourceVisibility visibility = ResourceVisibility.DRAFT;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_downloadable")
    @Builder.Default
    private Boolean isDownloadable = true;

    @Column(name = "is_previewable")
    @Builder.Default
    private Boolean isPreviewable = true;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "metadata", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;



    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // Explicit getId for BaseEntity inheritance
    public UUID getId() { return getIdDirect(); }
    public void setId(UUID id) { setIdDirect(id); }

    public enum VideoTutorialStatus {
        DRAFT, PROCESSING, READY, FAILED, ARCHIVED
    }

    // Reuse ResourceVisibility from Resource entity
    public enum ResourceVisibility {
        DRAFT, PRIVATE, CLASS_ONLY, COURSE_ONLY, SCHOOL, INSTITUTION, PUBLIC
    }

    // Helper methods
    public boolean isReady() {
        return status == VideoTutorialStatus.READY;
    }

    public boolean isProcessing() {
        return status == VideoTutorialStatus.PROCESSING;
    }

    public boolean isFailed() {
        return status == VideoTutorialStatus.FAILED;
    }

    public boolean isDraft() {
        return status == VideoTutorialStatus.DRAFT;
    }

    public boolean isPlayable() {
        return status == VideoTutorialStatus.READY && recordingUrl != null;
    }
}