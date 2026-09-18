package tz.elmkusoma.workers.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String notificationType;
    private String targetType;
    private Long targetId;
    private Long institutionId;
    private LocalDateTime timestamp;
}
