package tz.elmkusoma.realtime.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tz.elmkusoma.realtime.config.RabbitMQConfig;
import tz.elmkusoma.realtime.handler.NotificationBroadcaster;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationBroadcaster notificationBroadcaster;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationEvent(Map<String, Object> event) {
        log.info("Received notification event: {}", event);

        String userId = (String) event.get("userId");
        String institutionId = (String) event.get("institutionId");
        String type = (String) event.getOrDefault("type", "general");
        String message = (String) event.getOrDefault("message", "");

        Map<String, Object> notification = Map.of(
                "type", type,
                "message", message,
                "data", event.getOrDefault("data", Map.of())
        );

        if (userId != null) {
            notificationBroadcaster.sendNotificationToUser(userId, notification);
        }

        if (institutionId != null) {
            notificationBroadcaster.sendNotificationToInstitution(institutionId, notification);
        }
    }
}
