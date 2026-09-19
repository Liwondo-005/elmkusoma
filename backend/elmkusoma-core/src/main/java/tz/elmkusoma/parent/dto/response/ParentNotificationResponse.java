package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentNotificationResponse {

    private List<NotificationItem> notifications;
    private Long unreadCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationItem {
        private String id;
        private String type;
        private String title;
        private String detail;
        private String message;
        private String category;
        private String childName;
        private String targetType;
        private String targetId;
        private LocalDateTime createdAt;
        private Boolean isRead;
        private String priority;
    }
}
