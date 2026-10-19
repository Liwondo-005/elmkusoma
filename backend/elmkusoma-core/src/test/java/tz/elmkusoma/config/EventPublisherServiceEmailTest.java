package tz.elmkusoma.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The publisher must tell the truth about email events.
 *
 * <p>Email is the one event type whose failure a caller genuinely needs to know
 * about, because a caller records durable state from the outcome. These tests use a
 * mocked {@link RabbitTemplate}, so no broker is involved.</p>
 */
class EventPublisherServiceEmailTest {

    private static final String EMAIL_ROUTING_KEY = "elmkusoma.email";

    private RabbitTemplate rabbitTemplate;
    private EventPublisherService service;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        service = new EventPublisherService(rabbitTemplate);
    }

    @Test
    @DisplayName("A successful publish reports true and reaches the email routing key")
    void successfulPublishReturnsTrue() {
        boolean ok = service.publishEmailEvent(
                "learner@example.test", "Welcome", "welcome",
                new HashMap<>(), UUID.randomUUID());

        assertTrue(ok);
        verify(rabbitTemplate).convertAndSend(anyString(), eq(EMAIL_ROUTING_KEY), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    @DisplayName("A broker failure reports false instead of being swallowed")
    void brokerFailureReturnsFalse() {
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), org.mockito.ArgumentMatchers.<Object>any());

        boolean ok = service.publishEmailEvent(
                "learner@example.test", "Welcome", "welcome",
                new HashMap<>(), UUID.randomUUID());

        assertFalse(ok,
                "reporting true here would let a caller record QUEUED for mail the broker never saw");
    }

    @Test
    @DisplayName("Routing key, exchange and payload fields are unchanged")
    void payloadAndRoutingArePreserved() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("reference", "ENQ-1");
        UUID institutionId = UUID.randomUUID();

        service.publishEmailEvent("learner@example.test", "Subject", "contact-enquiry", vars, institutionId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(anyString(), eq(EMAIL_ROUTING_KEY), payload.capture());

        Map<String, Object> sent = payload.getValue();
        assertEquals("learner@example.test", sent.get("toEmail"));
        assertEquals("Subject", sent.get("subject"));
        assertEquals("contact-enquiry", sent.get("templateName"));
        assertEquals(vars, sent.get("templateVariables"));
        assertEquals(institutionId.toString(), sent.get("institutionId"));
        assertTrue(sent.containsKey("timestamp"));
    }

    @Test
    @DisplayName("A null institution stays null rather than throwing")
    void nullInstitutionIsTolerated() {
        assertTrue(service.publishEmailEvent(
                "learner@example.test", "S", "welcome", new HashMap<>(), null));
    }

    @Test
    @DisplayName("Best-effort non-email events keep their existing swallow-and-warn behaviour")
    void bestEffortEventsRemainUnchanged() {
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), org.mockito.ArgumentMatchers.<Object>any());

        // Presence is intentionally best-effort; it must still not throw, because
        // callers treat it as fire-and-forget.

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.publishPresenceEvent(
                UUID.randomUUID(), UUID.randomUUID(), "ONLINE"));

        // Presence is intentionally fire-and-forget: it must never propagate.
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(),
                org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    @DisplayName("A failed email publish is not retried by the publisher itself")
    void publisherDoesNotRetry() {
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), org.mockito.ArgumentMatchers.<Object>any());

        service.publishEmailEvent("learner@example.test", "S", "welcome", new HashMap<>(), UUID.randomUUID());

        // Exactly one attempt: retry policy belongs to the caller/outbox, not here,
        // so a broker outage cannot turn one enquiry into a publish storm.
        verify(rabbitTemplate, org.mockito.Mockito.times(1))
                .convertAndSend(anyString(), anyString(), org.mockito.ArgumentMatchers.<Object>any());
    }
}
