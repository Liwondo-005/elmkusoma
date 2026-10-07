package tz.elmkusoma.realtime.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
        Map<String, Object> presenceUpdate = new HashMap<>();
        presenceUpdate.put("type", "PRESENCE_UPDATE");
        presenceUpdate.put("userId", userId);
        presenceUpdate.put("status", status);
        presenceUpdate.put("institutionId", institutionId);
        presenceUpdate.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend(destination, presenceUpdate);
        log.info("Broadcast presence update for user {} with status {} to institution {}", userId, status, institutionId);
    }
}
