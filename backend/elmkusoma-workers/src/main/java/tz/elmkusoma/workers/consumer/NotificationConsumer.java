package tz.elmkusoma.workers.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tz.elmkusoma.workers.config.RabbitMQConfig;
import tz.elmkusoma.workers.domain.WorkerNotification;
import tz.elmkusoma.workers.event.NotificationEvent;
import tz.elmkusoma.workers.repository.WorkerNotificationRepository;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final WorkerNotificationRepository notificationRepository;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Received notification event: id={}, userId={}, type={}",
                event.getId(), event.getUserId(), event.getNotificationType());

        try {
            WorkerNotification notification = WorkerNotification.builder()
                    .userId(event.getUserId())
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .notificationType(event.getNotificationType())
                    .targetType(event.getTargetType())
                    .targetId(event.getTargetId())
                    .institutionId(event.getInstitutionId())
                    .isRead(false)
                    .build();

            WorkerNotification saved = notificationRepository.save(notification);
            log.info("Notification saved to database with id: {}", saved.getId());

            publishRealtimeEvent(saved);

            log.info("Notification processed successfully: id={}", event.getId());

        } catch (Exception e) {
            log.error("Failed to process notification event: id={}. Error: {}",
                    event.getId(), e.getMessage(), e);
            throw e;
        }
    }

    private void publishRealtimeEvent(WorkerNotification notification) {
        try {
            Map<String, String> eventData = new HashMap<>();
            eventData.put("type", "NOTIFICATION");
            eventData.put("notificationId", String.valueOf(notification.getId()));
            eventData.put("userId", String.valueOf(notification.getUserId()));
            eventData.put("title", notification.getTitle());
            eventData.put("message", notification.getMessage());
            eventData.put("notificationType", notification.getNotificationType());
            eventData.put("institutionId", String.valueOf(notification.getInstitutionId()));

            String channel = "realtime:user:" + notification.getUserId();
            redisTemplate.convertAndSend(channel, eventData.toString());

            log.info("Published realtime notification event to Redis channel: {}", channel);
        } catch (Exception e) {
            log.warn("Failed to publish realtime event to Redis: {}", e.getMessage());
        }
    }
}
