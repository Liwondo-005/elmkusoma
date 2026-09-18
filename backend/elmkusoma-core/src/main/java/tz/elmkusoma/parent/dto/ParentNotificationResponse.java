package tz.elmkusoma.parent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentNotificationResponse {

    private List<NotificationItem> notifications;
    private long unreadCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationItem {
        private String id;
        private String title;
        private String message;
        private String category;
        private String priority;
        private String targetType;
        private String targetId;
        private boolean isRead;
        private LocalDateTime createdAt;
    }
}
