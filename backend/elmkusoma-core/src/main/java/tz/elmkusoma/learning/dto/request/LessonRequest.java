package tz.elmkusoma.learning.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class LessonRequest {

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    @NotNull(message = "Class group ID is required")
    private UUID classGroupId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String contentText;

    private String videoUrl;

    private String fileAttachments;

    @NotNull(message = "Sort order is required")
    @Min(0)
    private Integer sortOrder;

    private Boolean isPublished = false;
}
