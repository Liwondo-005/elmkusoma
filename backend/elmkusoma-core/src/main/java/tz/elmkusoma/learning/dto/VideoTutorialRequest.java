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
public class VideoTutorialRequest {
    private UUID lessonId;
    private UUID moduleId;
    private UUID courseId;

    @NotBlank
    @Size(max = 300)
    private String title;

    @Size(max = 2000)
    private String description;

    private Integer durationSeconds;
    private String recordingUrl;
    private String thumbnailUrl;
    private String captionUrl;

    @Builder.Default
    private String visibility = "DRAFT";

    private Integer sortOrder;

    @Builder.Default
    private Boolean isDownloadable = true;

    @Builder.Default
    private Boolean isPreviewable = true;

    private String tags;
    private String metadata;
    private List<String> tagNames;
}