package tz.elmkusoma.emailoutbox.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The outbox state machine, with no Spring and no broker.
 *
 * <p>These are the rules that decide whether a notification survives a broker outage
 * and whether a retry can ever turn into an unbounded duplicate storm.</p>
 */
class EmailOutboxEventTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 1, 12, 0);

    private EmailOutboxEvent event(int maxAttempts) {
        return EmailOutboxEvent.builder()
                .recipient("learner@example.test")
                .subject("Reset your Elmkusoma password")
                .templateName("password-reset")
                .templateVariables(Map.of("reference", "ENQ-1"))
                .status(EmailOutboxEvent.STATUS_PENDING)
                .attempts(0)
                .maxAttempts(maxAttempts)
                .nextAttemptAt(NOW)
                .build();
    }

    @Test
    @DisplayName("A new event is PENDING and immediately dispatchable")
    void newEventIsDispatchable() {
        EmailOutboxEvent e = event(5);
        assertEquals(EmailOutboxEvent.STATUS_PENDING, e.getStatus());
        assertTrue(e.isDispatchable(NOW));
        assertFalse(e.isTerminallyFailed());
    }

    @Test
    @DisplayName("Claiming increments the attempt count and takes a lease")
    void claimCountsAttemptAndLeases() {
        EmailOutboxEvent e = event(5);

        e.claim("relay-a", NOW, 5);

        assertEquals(1, e.getAttempts());
        assertEquals(EmailOutboxEvent.STATUS_PUBLISHING, e.getStatus());
        assertEquals("relay-a", e.getLockedBy());
        assertFalse(e.isDispatchable(NOW.plusMinutes(1)), "a live lease blocks another relay");
    }

    @Test
    @DisplayName("An expired lease is reclaimable, so a dead relay cannot strand mail")
    void expiredLeaseIsReclaimable() {
        EmailOutboxEvent e = event(5);
        e.claim("relay-a", NOW, 5);

        // Relay "relay-a" was killed. Five minutes later the row is fair game again.
        assertTrue(e.isDispatchable(NOW.plusMinutes(6)));
        assertTrue(e.leaseExpired(NOW.plusMinutes(6)));
    }

    @Test
    @DisplayName("Backoff grows between attempts and the event returns to PENDING")
    void backoffGrowsAndEventIsRescheduled() {
        EmailOutboxEvent e = event(5);
        e.claim("relay-a", NOW, 5);

        boolean willRetry = e.recordFailure("broker down", NOW, 30);

        assertTrue(willRetry);
        assertEquals(EmailOutboxEvent.STATUS_PENDING, e.getStatus());
        assertNull(e.getLockedBy(), "the lease is released so a retry is not blocked");
        assertEquals("broker down", e.getLastError());
        assertEquals(NOW.plusSeconds(30), e.getNextAttemptAt(), "first retry uses the base backoff");

        // Second failure backs off further rather than hammering the broker.
        e.claim("relay-b", NOW, 5);
        e.recordFailure("broker down", NOW, 30);
        assertEquals(NOW.plusSeconds(60), e.getNextAttemptAt());
    }

    @Test
    @DisplayName("Retries are bounded and end in an observable FAILED state")
    void retriesAreBoundedAndTerminal() {
        EmailOutboxEvent e = event(3);
        boolean willRetry = true;
        int rounds = 0;
        while (willRetry && rounds < 10) {
            e.claim("relay-a", NOW, 5);
            willRetry = e.recordFailure("broker down", NOW, 30);
            rounds++;
        }

        assertEquals(3, rounds, "exactly maxAttempts attempts, never an unbounded loop");
        assertFalse(willRetry);
        assertEquals(EmailOutboxEvent.STATUS_FAILED, e.getStatus());
        assertTrue(e.isTerminallyFailed());
        assertFalse(e.isDispatchable(NOW.plusDays(1)), "a failed event must stop being picked up");
    }

    @Test
    @DisplayName("A long error is truncated to the column width rather than failing the insert")
    void longErrorIsTruncated() {
        EmailOutboxEvent e = event(5);
        e.claim("relay-a", NOW, 5);

        e.recordFailure("x".repeat(2000), NOW, 30);

        assertEquals(500, e.getLastError().length());
    }

    @Test
    @DisplayName("PUBLISHED records broker acceptance and clears the error")
    void markPublishedClearsErrorAndLease() {
        EmailOutboxEvent e = event(5);
        e.claim("relay-a", NOW, 5);
        e.recordFailure("transient", NOW, 30);
        e.claim("relay-b", NOW, 5);

        e.markPublished(NOW);

        assertEquals(EmailOutboxEvent.STATUS_PUBLISHED, e.getStatus());
        assertEquals(NOW, e.getPublishedAt());
        assertNull(e.getLastError());
        assertNull(e.getLockedBy());
        assertFalse(e.isDispatchable(NOW.plusYears(1)), "a published event is never re-dispatched");
    }

    @Test
    @DisplayName("Two repeated requests by the same person are two events, not one")
    void repeatedRequestsAreNotDeduplicated() {
        // Deliberately the same recipient and template: a second genuine password
        // reset must still be delivered.
        EmailOutboxEvent first = event(5);
        first.setId(java.util.UUID.randomUUID());
        EmailOutboxEvent second = event(5);
        second.setId(java.util.UUID.randomUUID());

        assertNotEquals(first.getId(), second.getId());
        assertEquals(first.getRecipient(), second.getRecipient());
        assertEquals(first.getTemplateName(), second.getTemplateName());
        assertNull(first.getIdempotencyKey(),
                "an ordinary request carries no key, so nothing collapses it");
        assertNull(second.getIdempotencyKey());
    }
}