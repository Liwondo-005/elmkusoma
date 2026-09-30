package tz.elmkusoma.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {
    private UUID id;
    private UUID institutionId;
    private UUID lessonId;
    private UUID moduleId;
    private UUID courseId;
    /** Backward-compatible derived context id (lesson → module → course). */
    private UUID subjectId;
    private UUID uploadedBy;
    private String uploadedByName;
    private String title;
    private String description;
    private String resourceType;
    private String mimeType;
    private Long fileSize;
    private String storageUrl;
    private String storageObjectKey;
    private String storageBucket;
    private String externalUrl;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private Integer pageCount;
    private Integer width;
    private Integer height;
    private String visibility;
    private Integer sortOrder;
    private Boolean isDownloadable;
    private Boolean isPreviewable;
    private String processingStatus;
    private String processingError;
private String metadata;
    private String tags;
    private List<String> tagNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeletable;
    private Boolean isEditable;
    private Boolean isViewable;
}