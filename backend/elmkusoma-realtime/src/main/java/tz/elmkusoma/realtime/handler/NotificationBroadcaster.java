package tz.elmkusoma.realtime.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotificationToUser(String userId, Map<String, Object> notification) {
        String destination = "/queue/notifications/" + userId;
        messagingTemplate.convertAndSend(destination, notification);
        log.info("Sent notification to user {} at {}", userId, destination);
    }

    public void sendNotificationToInstitution(String institutionId, Map<String, Object> notification) {
        String destination = "/topic/institution/" + institutionId + "/notifications";
        messagingTemplate.convertAndSend(destination, notification);
        log.info("Sent notification to institution {} at {}", institutionId, destination);
    }

    public void broadcastPresenceUpdate(String institutionId, String userId, String status) {
        String destination = "/topic/institution/" + institutionId + "/presence";
        Map<String, Object> presenceUpdate = Map.of(
                "userId", userId,
                "status", status,
                "institutionId", institutionId
        );
        messagingTemplate.convertAndSend(destination, presenceUpdate);
        log.info("Broadcast presence update for user {} with status {} to institution {}", userId, status, institutionId);
    }
}
