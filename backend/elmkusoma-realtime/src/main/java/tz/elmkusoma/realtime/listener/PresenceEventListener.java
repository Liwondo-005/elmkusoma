package tz.elmkusoma.realtime.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tz.elmkusoma.realtime.config.RabbitMQConfig;
import tz.elmkusoma.realtime.handler.NotificationBroadcaster;
import tz.elmkusoma.realtime.handler.PresenceHandler;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceEventListener {

    private final PresenceHandler presenceHandler;
    private final NotificationBroadcaster notificationBroadcaster;

    @RabbitListener(queues = RabbitMQConfig.PRESENCE_QUEUE)
    public void handlePresenceEvent(Map<String, Object> event) {
        log.info("Received presence event: {}", event);

        String userId = (String) event.get("userId");
        String institutionId = (String) event.get("institutionId");
        String status = (String) event.get("status");

        if (userId == null || institutionId == null) {
            log.warn("Invalid presence event: missing userId or institutionId");
            return;
        }

        switch (status == null ? "" : status) {
            case "ONLINE" -> {
                updatePresenceState(() -> presenceHandler.userConnected(userId, institutionId));
                notificationBroadcaster.broadcastPresenceUpdate(institutionId, userId, "ONLINE");
            }
            case "OFFLINE" -> {
                updatePresenceState(() -> presenceHandler.userDisconnected(userId));
                notificationBroadcaster.broadcastPresenceUpdate(institutionId, userId, "OFFLINE");
            }
            default -> log.warn("Unknown presence status: {}", status);
        }
    }

    private void updatePresenceState(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("Presence state store unavailable: {}", e.getMessage());
        }
    }
}
