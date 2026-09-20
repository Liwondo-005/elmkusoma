package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationSummaryResponse {
    private UUID id;
    private String title;
    private String message;
    private String notificationType;
    private String priority;
    private String targetAudience;
    private String targetRole;
    private String sentBy;
    private LocalDateTime sentAt;
    private Integer readCount;
}
