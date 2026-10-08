package tz.elmkusoma.workers.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private String notificationType;
    private String targetType;
    private UUID targetId;
    private UUID institutionId;
    private LocalDateTime timestamp;
}