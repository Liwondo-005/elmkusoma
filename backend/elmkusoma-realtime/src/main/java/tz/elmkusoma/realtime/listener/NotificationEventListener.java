package tz.elmkusoma.realtime.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tz.elmkusoma.realtime.config.RabbitMQConfig;
import tz.elmkusoma.realtime.handler.NotificationBroadcaster;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationBroadcaster notificationBroadcaster;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationEvent(Map<String, Object> event) {
        log.info("Received notification event: {}", event);

        Object userId = event.get("userId");
        Object institutionId = event.get("institutionId");

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "NOTIFICATION");
        notification.put("userId", userId);
        notification.put("institutionId", institutionId);
        notification.put("title", event.get("title"));
        notification.put("message", event.get("message"));
        notification.put("notificationType", event.get("notificationType"));
        notification.put("targetType", event.get("targetType"));
        notification.put("targetId", event.get("targetId"));
        notification.put("timestamp", event.get("timestamp"));

        if (userId != null) {
            notificationBroadcaster.sendNotificationToUser(String.valueOf(userId), notification);
        }

        if (institutionId != null) {
            notificationBroadcaster.sendNotificationToInstitution(String.valueOf(institutionId), notification);
        }
    }
}
