package tz.elmkusoma.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequest {
        private UUID lessonId;

    private UUID moduleId;
    private UUID courseId;

    /** Authoritative teaching-assignment target for class-targeted resources (validated server-side). */
    private UUID teacherAssignmentId;

    @NotBlank
    @Size(max = 300)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull
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

    @Builder.Default
    private String visibility = "DRAFT";

    private Integer sortOrder;

    @Builder.Default
    private Boolean isDownloadable = true;

    @Builder.Default
    private Boolean isPreviewable = false;

    private String tags;
    private String metadata;

    private List<String> tagNames;
}