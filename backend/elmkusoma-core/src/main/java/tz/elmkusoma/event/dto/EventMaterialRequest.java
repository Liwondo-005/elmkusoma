package tz.elmkusoma.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EventMaterialRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Material type is required")
    private String materialType;

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private Long fileSize;

    private Integer durationMinutes;

    private Integer sortOrder = 0;

    private Boolean isPublic = true;
}
