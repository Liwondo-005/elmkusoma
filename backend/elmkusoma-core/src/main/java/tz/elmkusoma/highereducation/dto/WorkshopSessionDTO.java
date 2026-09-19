package tz.elmkusoma.highereducation.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkshopSessionDTO {
    private UUID id;
    private UUID institutionId;
    private UUID studentId;
    private String title;
    private String description;
    private String workshopType;
    private UUID courseId;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String location;
    private String status;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private String materialsUrl;
    private UUID instructorId;
}
