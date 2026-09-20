package tz.elmkusoma.administration.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationCreateRequest {
    private String title;
    private String message;
    private String notificationType;
    private String priority;
    private String targetAudience;
    private String targetRole;
}
