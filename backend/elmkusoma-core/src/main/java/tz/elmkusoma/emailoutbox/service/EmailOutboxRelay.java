package tz.elmkusoma.emailoutbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.config.EventPublisherService;
import tz.elmkusoma.emailoutbox.domain.EmailOutboxEvent;
import tz.elmkusoma.emailoutbox.repository.EmailOutboxRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Moves durable outbox rows onto the broker.
 *
 * <p>This is the piece that turns "the notification exists" into "the broker accepted
 * it", without ever holding a database transaction open across a network call. The
 * shape is deliberately three steps for exactly that reason:</p>
 *
 * <ol>
 *   <li><b>Claim</b> - a short transaction locks a batch and stamps a lease on each
 *       row, returning the payloads to publish.</li>
 *   <li><b>Publish</b> - no transaction is open while the broker is contacted, so a
 *       slow or hung broker cannot pin a database connection or block other work.</li>
 *   <li><b>Record</b> - a short transaction per event writes the outcome, rescheduling
 *       with backoff or failing terminally.</li>
 * </ol>
 *
 * <p><b>What {@code PUBLISHED} means.</b> Only that the broker accepted the message.
 * Template rendering and SMTP delivery happen in elmkusoma-workers and are tracked
 * there. This relay never claims an email arrived.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOutboxRelay {

    private final EmailOutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${elmkusoma.email.outbox.batch-size:50}")
    private int batchSize = 50;

    @Value("${elmkusoma.email.outbox.lease-minutes:5}")
    private int leaseMinutes = 5;

    @Value("${elmkusoma.email.outbox.base-backoff-seconds:30}")
    private int baseBackoffSeconds = 30;

    @Value("${elmkusoma.email.outbox.confirm-timeout-ms:5000}")
    private long confirmTimeoutMs = 5000;

    /** Correlation ids the broker returned as unroutable, so a return is a failure. */
    private final Map<String, Boolean> returned = new ConcurrentHashMap<>();

    /**
     * Publishes as mandatory, so an unroutable message comes back to us instead of
     * vanishing.
     *
     * <p>Without this, the broker happily ACKs a message whose routing key matches no
     * queue, and the relay would record {@code PUBLISHED} for an email that can never
     * be dispatched. That is the failure this exists to prevent.</p>
     *
     * <p>The callback and {@code CorrelationData.getReturned()} are both consulted
     * because the correlation data is the race-free source: Spring AMQP attaches the
     * returned message to the {@code CorrelationData} <em>before</em> completing its
     * confirm, so by the time the confirm future resolves the return is already
     * visible. The map is a defensive second signal.</p>
     */
    @jakarta.annotation.PostConstruct
    void configureMandatoryPublishing() {
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            // This Spring AMQP version carries the correlation on the message
            // properties rather than on ReturnedMessage itself.
            String correlationId = returnedMessage.getMessage()
                    .getMessageProperties().getCorrelationId();
            if (correlationId != null) {
                returned.put(correlationId, Boolean.TRUE);
            }
            // The reason only - never the payload, which may contain message content.
            log.warn("Email event returned as unroutable: {}",
                    returnedMessage.getReplyText());
        });
    }

    @Scheduled(fixedRateString = "${elmkusoma.email.outbox.poll-interval-ms:15000}",
            initialDelayString = "${elmkusoma.email.outbox.initial-delay-ms:20000}")
    public void dispatchDueEvents() {
        List<Dispatch> batch = claimBatch();
        if (batch.isEmpty()) {
            return;
        }
        log.info("Email outbox relay dispatching {} event(s)", batch.size());
        for (Dispatch dispatch : batch) {
            publish(dispatch);
        }
    }

    /**
     * Claims a batch of due events, each stamped with a lease.
     *
     * <p>Runs in its own transaction so the row locks are released before any network
     * work begins.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Dispatch> claimBatch() {
        LocalDateTime now = LocalDateTime.now();
        String worker = "relay-" + UUID.randomUUID();
        List<EmailOutboxEvent> due = repository.findDueForUpdate(now, PageRequest.of(0, batchSize));
        List<Dispatch> claimed = new java.util.ArrayList<>(due.size());
        for (EmailOutboxEvent event : due) {
            event.claim(worker, now, leaseMinutes);
            claimed.add(Dispatch.from(event));
        }
        if (!due.isEmpty()) {
            repository.saveAll(due);
        }
        return claimed;
    }

    void publish(Dispatch dispatch) {
        Map<String, Object> payload = toPayload(dispatch);
        CorrelationData correlation = new CorrelationData(dispatch.id().toString());
        try {
            rabbitTemplate.convertAndSend(EventPublisherService.EXCHANGE_NAME,
                    EventPublisherService.EMAIL_ROUTING_KEY, payload, correlation);

            CorrelationData.Confirm confirm =
                    correlation.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);

            if (!confirm.isAck()) {
                recordFailure(dispatch, "broker negatively acknowledged: " + confirm.getReason());
                return;
            }
            // Race-free: Spring AMQP records the return on the CorrelationData before
            // completing the confirm, so by this point it is already visible.
            boolean wasReturned = correlation.getReturned() != null
                    || returned.remove(dispatch.id().toString()) != null;
            if (wasReturned) {
                // The broker accepted the message but could not route it: the binding
                // is missing or wrong. Retrying will not help until that is fixed, but
                // it must still not be recorded as published.
                recordFailure(dispatch, "message was returned as unroutable");
                return;
            }
            recordSuccess(dispatch);
        } catch (Exception e) {
            recordFailure(dispatch, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Reproduces the exact payload workers already consumes.
     *
     * <p>Field names and types are identical to the pre-outbox publisher, so a message
     * relayed from the outbox is indistinguishable from one published inline.</p>
     */
    private Map<String, Object> toPayload(Dispatch dispatch) {
        Map<String, Object> event = new HashMap<>();
        event.put("toEmail", dispatch.recipient());
        event.put("subject", dispatch.subject());
        event.put("templateName", dispatch.template());
        event.put("templateVariables", dispatch.variables());
        event.put("institutionId", dispatch.institutionId() != null
                ? dispatch.institutionId().toString() : null);
        event.put("timestamp", System.currentTimeMillis());
        return event;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Dispatch dispatch) {
        repository.findById(dispatch.id()).ifPresent(event -> {
            event.markPublished(LocalDateTime.now());
            repository.save(event);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Dispatch dispatch, String error) {
        repository.findById(dispatch.id()).ifPresent(event -> {
            boolean willRetry = event.recordFailure(error, LocalDateTime.now(), baseBackoffSeconds);
            repository.save(event);
            if (willRetry) {
                log.warn("Email outbox event {} attempt {} failed, will retry: {}",
                        event.getId(), event.getAttempts(), event.getLastError());
            } else {
                log.error("Email outbox event {} exhausted {} attempts and is now FAILED: {}",
                        event.getId(), event.getAttempts(), event.getLastError());
            }
        });
    }

    /** Immutable snapshot taken while the row was locked. */
    public record Dispatch(UUID id, UUID institutionId, String recipient, String subject,
                           String template, Map<String, Object> variables) {
        static Dispatch from(EmailOutboxEvent event) {
            return new Dispatch(event.getId(), event.getInstitutionId(), event.getRecipient(),
                    event.getSubject(), event.getTemplateName(), event.getTemplateVariables());
        }
    }
}
