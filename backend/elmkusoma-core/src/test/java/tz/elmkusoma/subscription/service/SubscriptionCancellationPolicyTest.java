package tz.elmkusoma.subscription.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.subscription.domain.Subscription;
import tz.elmkusoma.subscription.repository.SubscriptionRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BR-01: cancellation prevents the next renewal, access continues to the end of the already-paid
 * period, and nothing is refunded.
 *
 * <p>The property that makes this correct is that access is derived from the period boundary and
 * not from the status. If that ever changed, cancelling would silently cut off access somebody
 * had already paid for - which is exactly what these tests pin.
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class SubscriptionCancellationPolicyTest {

    @Autowired private SubscriptionService subscriptionService;
    @Autowired private SubscriptionRepository subscriptionRepository;

    private Subscription livePeriod(UUID studentId, Duration remaining) {
        LocalDateTime now = LocalDateTime.now();
        Subscription s = Subscription.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .studentId(studentId)
                .payerId(UUID.randomUUID())
                .serviceType("MONTHLY_ACCESS")
                .paymentId(UUID.randomUUID())
                .status(Subscription.STATUS_ACTIVE)
                .periodStart(now.minus(Duration.ofDays(5)))
                .periodEnd(now.plus(remaining))
                .build();
        return subscriptionRepository.saveAndFlush(s);
    }


    /**
     * Truncates to whole seconds.
     *
     * <p>PostgreSQL timestamp holds microseconds, so a LocalDateTime that went through the
     * database comes back without nanoseconds. Asserting nanosecond equality across a round trip
     * would fail on storage precision, not on the behaviour under test.
     */
    private static LocalDateTime seconds(LocalDateTime t) {
        return t == null ? null : t.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    }
    @Test
    void accessContinuesUntilThePaidPeriodEnds() {
        Subscription s = livePeriod(UUID.randomUUID(), Duration.ofDays(25));
        LocalDateTime periodEnd = s.getPeriodEnd();

        Subscription cancelled = subscriptionService.cancelAtPeriodEnd(s.getId(), "BR-01 probe");

        assertEquals(Subscription.STATUS_CANCELLED, cancelled.getStatus());
        assertEquals(seconds(periodEnd), seconds(cancelled.getCancelledAt()),
                "the effective date is the end of the already-paid period, not now");

        // The whole point: cancelling must not revoke access that was paid for.
        LocalDateTime insideThePaidPeriod = periodEnd.minusDays(1);
        assertTrue(cancelled.isWithinPeriod(insideThePaidPeriod),
                "access must continue right up to the end of the period that was paid for");
        assertFalse(cancelled.isWithinPeriod(periodEnd.plusSeconds(1)),
                "access must stop once the paid period is over");
    }

    @Test
    void cancellingDoesNotRefund() {
        // BR-01 explicitly: cancellation alone triggers no refund. The payment is untouched.
        Subscription s = livePeriod(UUID.randomUUID(), Duration.ofDays(20));

        subscriptionService.cancelAtPeriodEnd(s.getId(), "no refund expected");

        Subscription reloaded = subscriptionRepository.findById(s.getId()).orElseThrow();
        assertEquals(Subscription.STATUS_CANCELLED, reloaded.getStatus());
        // Nothing in this service touches a payment; asserting the period is unchanged is the
        // observable proof that cancelling is not a refund in disguise.
        assertEquals(seconds(s.getPeriodStart()), seconds(reloaded.getPeriodStart()),
                "the paid period must not be rewritten by a cancellation");
        assertEquals(seconds(s.getPeriodEnd()), seconds(reloaded.getPeriodEnd()),
                "the paid period must not be shortened by a cancellation");
    }

    @Test
    void repeatedCancellationIsIdempotent() {
        Subscription s = livePeriod(UUID.randomUUID(), Duration.ofDays(15));

        Subscription first = subscriptionService.cancelAtPeriodEnd(s.getId(), "first");
        Subscription second = subscriptionService.cancelAtPeriodEnd(s.getId(), "second");
        Subscription third = subscriptionService.cancelAtPeriodEnd(s.getId(), "third");

        assertEquals(Subscription.STATUS_CANCELLED, second.getStatus());
        assertEquals(seconds(first.getCancelledAt()), seconds(second.getCancelledAt()),
                "a repeat request must not move the effective date");
        assertEquals(seconds(first.getCancelledAt()), seconds(third.getCancelledAt()));
        assertEquals("first", third.getCancellationReason(),
                "the original reason is kept; a repeat must not rewrite the record");
    }

    @Test
    void anElapsedPeriodCannotBeCancelled() {
        // Cancelling something already finished would record an event that affected neither
        // access nor money.
        Subscription s = livePeriod(UUID.randomUUID(), Duration.ofDays(-1));

        assertThrows(IllegalStateException.class,
                () -> subscriptionService.cancelAtPeriodEnd(s.getId(), "too late"),
                "a period that has already run out has nothing left to cancel");
    }

    @Test
    void cancellingOnePeriodDoesNotTouchALaterChainedPeriod() {
        UUID studentId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Subscription first = subscriptionRepository.saveAndFlush(Subscription.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .studentId(studentId)
                .payerId(UUID.randomUUID())
                .serviceType("MONTHLY_ACCESS")
                .paymentId(UUID.randomUUID())
                .status(Subscription.STATUS_ACTIVE)
                .periodStart(now.minusDays(20))
                .periodEnd(now.plusDays(10))
                .build());
        Subscription second = subscriptionRepository.saveAndFlush(Subscription.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .studentId(studentId)
                .payerId(UUID.randomUUID())
                .serviceType("MONTHLY_ACCESS")
                .paymentId(UUID.randomUUID())
                .status(Subscription.STATUS_ACTIVE)
                .periodStart(now.plusDays(10))
                .periodEnd(now.plusDays(40))
                .build());

        subscriptionService.cancelAtPeriodEnd(first.getId(), "stop renewing");

        assertEquals(Subscription.STATUS_ACTIVE,
                subscriptionRepository.findById(second.getId()).orElseThrow().getStatus(),
                "a later chained period is a separate row and must not be cancelled by cancelling "
                        + "the period before it");
    }
}
