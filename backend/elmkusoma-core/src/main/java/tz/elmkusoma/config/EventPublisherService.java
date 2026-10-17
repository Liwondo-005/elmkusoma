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

    public static final String EXCHANGE_NAME = "elmkusoma.exchange";
    private static final String NOTIFICATION_ROUTING_KEY = "elmkusoma.notification";
    private static final String CERTIFICATE_ROUTING_KEY = "elmkusoma.certificate";
    public static final String EMAIL_ROUTING_KEY = "elmkusoma.email";
    private static final String PRESENCE_ROUTING_KEY = "elmkusoma.presence.realtime";

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

    /**
     * Publishes an email event and reports whether the broker accepted it.
     *
     * <p>Unlike {@link #publishSafely}, this never converts a failure into a silent
     * no-op: a caller is told the truth so it can record an accurate state instead of
     * claiming mail was queued when nothing reached the broker.</p>
     *
     * <p><b>This is not proof of delivery.</b> {@code true} means only that
     * {@code convertAndSend} completed without throwing, i.e. the broker took the
     * message. Rendering and SMTP delivery happen later, in the workers module, and
     * can still fail there.</p>
     *
     * @return {@code true} when the broker accepted the event without a synchronous error
     */
    public boolean publishEmailEvent(String toEmail, String subject, String templateName,
                                   Map<String, Object> templateVariables, UUID institutionId) {
        Map<String, Object> event = new HashMap<>();
        event.put("toEmail", toEmail);
        event.put("subject", subject);
        event.put("templateName", templateName);
        event.put("templateVariables", templateVariables);
        // Workers treat the tenant marker as opaque; null stays null.
        event.put("institutionId", institutionId != null ? institutionId.toString() : null);
        event.put("timestamp", System.currentTimeMillis());

        try {
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, EMAIL_ROUTING_KEY, event);
            log.debug("Published email event to {}", toEmail);
            return true;
        } catch (Exception e) {
            // No recipient address or message body is logged, and the reason is kept
            // so an operator can distinguish a broker outage from a bad payload.
            log.warn("Email event publish failed for template {}: {}",
                    templateName, e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
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
