package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummary {
    private UUID id;
    private String title;
    private String message;
    private String notificationType;
    private String targetType;
    private UUID targetId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
