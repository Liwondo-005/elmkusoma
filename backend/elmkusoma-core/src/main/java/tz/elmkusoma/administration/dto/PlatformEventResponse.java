package tz.elmkusoma.administration.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class PlatformEventResponse {
    private UUID id;
    private UUID institutionId;
    private String title;
    private String eventType;
    private String status;
    private LocalDateTime startsAt;
    private LocalDateTime createdAt;
}
