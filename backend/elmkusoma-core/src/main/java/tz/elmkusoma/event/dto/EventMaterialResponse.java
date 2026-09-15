package tz.elmkusoma.event.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class EventMaterialResponse {
    private UUID id;
    private UUID eventId;
    private String eventTitle;
    private String title;
    private String description;
    private String materialType;
    private String fileUrl;
    private Long fileSize;
    private Integer durationMinutes;
    private Integer sortOrder;
    private Boolean isPublic;
}
