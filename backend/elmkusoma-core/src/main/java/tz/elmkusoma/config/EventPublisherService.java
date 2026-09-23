package tz.elmkusoma.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE_NAME = "elmkusoma.exchange";
    private static final String NOTIFICATION_ROUTING_KEY = "elmkusoma.notification";
    private static final String CERTIFICATE_ROUTING_KEY = "elmkusoma.certificate";
    private static final String EMAIL_ROUTING_KEY = "elmkusoma.email";
    private static final String PRESENCE_ROUTING_KEY = "elmkusoma.notification.realtime";

    // RabbitMQ is optional (documented in .env): publishing must never break the caller
    private void publishSafely(String routingKey, Map<String, Object> event, String description) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, event);
            log.debug("Published {}", description);
        } catch (Exception e) {
            log.warn("Event publish skipped ({}): {}", description, e.getMessage());
        }
    }

    public void publishNotificationEvent(UUID userId, String title, String message,
                                          String notificationType, String targetType,
                                          UUID targetId, UUID institutionId) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId.toString());
        event.put("title", title);
        event.put("message", message);
        event.put("notificationType", notificationType);
        event.put("targetType", targetType);
        event.put("targetId", targetId != null ? targetId.toString() : null);
        event.put("institutionId", institutionId.toString());
        event.put("timestamp", System.currentTimeMillis());

        publishSafely(NOTIFICATION_ROUTING_KEY, event, "notification event for user " + userId);
    }

    public void publishCertificateEvent(UUID certificateId, UUID studentId, String studentName,
                                         String courseName, UUID templateId, UUID institutionId) {
        Map<String, Object> event = new HashMap<>();
        event.put("certificateId", certificateId.toString());
        event.put("studentId", studentId.toString());
        event.put("studentName", studentName);
        event.put("courseName", courseName);
        event.put("templateId", templateId != null ? templateId.toString() : null);
        event.put("institutionId", institutionId.toString());
        event.put("timestamp", System.currentTimeMillis());

        publishSafely(CERTIFICATE_ROUTING_KEY, event, "certificate event for " + certificateId);
    }

    public void publishEmailEvent(String toEmail, String subject, String templateName,
                                   Map<String, Object> templateVariables, UUID institutionId) {
        Map<String, Object> event = new HashMap<>();
        event.put("toEmail", toEmail);
        event.put("subject", subject);
        event.put("templateName", templateName);
        event.put("templateVariables", templateVariables);
        event.put("institutionId", institutionId.toString());
        event.put("timestamp", System.currentTimeMillis());

        publishSafely(EMAIL_ROUTING_KEY, event, "email event to " + toEmail);
    }

    public void publishPresenceEvent(UUID userId, UUID institutionId, String status) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId.toString());
        event.put("institutionId", institutionId.toString());
        event.put("status", status);
        event.put("timestamp", System.currentTimeMillis());

        publishSafely(PRESENCE_ROUTING_KEY, event, "presence event for user " + userId + " status " + status);
    }
}
