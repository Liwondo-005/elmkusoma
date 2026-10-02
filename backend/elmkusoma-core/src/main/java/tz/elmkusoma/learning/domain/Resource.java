package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.CascadeType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "resources", indexes = {
    @Index(name = "idx_resources_institution", columnList = "institution_id"),
    @Index(name = "idx_resources_lesson", columnList = "lesson_id"),
    @Index(name = "idx_resources_module", columnList = "module_id"),
    @Index(name = "idx_resources_course", columnList = "course_id"),
    @Index(name = "idx_resources_type", columnList = "resource_type"),
    @Index(name = "idx_resources_visibility", columnList = "visibility"),
    @Index(name = "idx_resources_uploaded_by", columnList = "uploaded_by"),
    @Index(name = "idx_resources_type_visibility", columnList = "resource_type, visibility")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Resource extends BaseEntity {

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "module_id")
    private UUID moduleId;

    @Column(name = "course_id")
    private UUID courseId;

    /** Authoritative class target of a teacher-created CLASS_ONLY resource. */
    @Column(name = "teacher_assignment_id")
    private UUID teacherAssignmentId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private ResourceType resourceType;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_url", length = 1000)
    private String storageUrl;

    @Column(name = "storage_object_key", length = 500)
    private String storageObjectKey;

    @Column(name = "storage_bucket", length = 100)
    private String storageBucket;

    @Column(name = "external_url", length = 1000)
    private String externalUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /**
     * Row id of the stored byte object inside the existing media service
     * (media_files.id).  Null for URL-based resources and for legacy rows that
     * were created with a storage URL only.
     */
    @Column(name = "media_id")
    private Long mediaId;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

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
    private Boolean isPreviewable = false;

    @Column(name = "processing_status", length = 30)
    @Builder.Default
    private String processingStatus = "READY";

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "metadata", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "tags", length = 500)
    private String tags;


    @OneToMany(mappedBy = "resourceId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ResourceTagging> taggings = new ArrayList<>();

    public enum ResourceType {
        PDF, DOCUMENT, PRESENTATION, SPREADSHEET, IMAGE, VIDEO,
        AUDIO, EXTERNAL_LINK, LINK, LIVE_RECORDING, ARCHIVE, OTHER
    }

    public enum ResourceVisibility {
        DRAFT, PRIVATE, CLASS_ONLY, COURSE_ONLY, SCHOOL, INSTITUTION, PUBLIC
    }

    // Helper methods
    /** LINK and EXTERNAL_LINK are both URL resources (no stored bytes). */
    public boolean isExternalLink() {
        return resourceType == ResourceType.EXTERNAL_LINK || resourceType == ResourceType.LINK;
    }

    public boolean isFileBased() {
        return !isExternalLink();
    }

    public boolean isVideo() {
        return resourceType == ResourceType.VIDEO;
    }

    public boolean isLiveRecording() {
        return resourceType == ResourceType.LIVE_RECORDING;
    }

    public boolean isDocument() {
        return resourceType == ResourceType.PDF 
            || resourceType == ResourceType.DOCUMENT 
            || resourceType == ResourceType.PRESENTATION 
            || resourceType == ResourceType.SPREADSHEET;
    }

    public boolean isImage() {
        return resourceType == ResourceType.IMAGE;
    }

    // Backward compatibility methods for existing LearnerController
    public UUID getSubjectId() {
        return courseId != null ? courseId : (moduleId != null ? moduleId : lessonId);
    }

    public String getFileUrl() {
        return storageUrl;
    }
}