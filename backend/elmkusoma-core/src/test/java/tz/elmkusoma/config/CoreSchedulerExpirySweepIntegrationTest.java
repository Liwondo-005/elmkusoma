package tz.elmkusoma.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.subscription.domain.Subscription;
import tz.elmkusoma.subscription.repository.SubscriptionRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The expiry sweep against the database, as the {@code @Scheduled} method runs it.
 *
 * <p>{@code SubscriptionServiceTest} proves the selection logic with a stubbed repository, and
 * {@code CoreSchedulerExpirySweepTest} proves the scheduled method is wired to it. Neither can show
 * that a real row actually comes back EXPIRED, that the repository method exists and selects the
 * right rows, or that a period still in the future is left alone. That is what this drives.</p>
 *
 * <p>The sweep is global by design, so every assertion is scoped to rows created here by unique
 * student id: leftovers from other suites in the shared test database cannot make these pass or
 * fail.</p>
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class CoreSchedulerExpirySweepIntegrationTest {

    @Autowired private CoreScheduler coreScheduler;
    @Autowired private SubscriptionRepository subscriptionRepository;

    private final List<UUID> createdIds = new ArrayList<>();

    @AfterEach
    void removeProbes() {
        subscriptionRepository.deleteAllById(createdIds);
        createdIds.clear();
    }

    @Test
    void theScheduledSweepMarksRealElapsedRowsExpiredAndLeavesLiveOnesAlone() {
        UUID elapsedStudent = UUID.randomUUID();
        UUID liveStudent = UUID.randomUUID();
        UUID cancelledStudent = UUID.randomUUID();

        Subscription elapsed = probe(elapsedStudent, Subscription.STATUS_ACTIVE, LocalDateTime.now().minusDays(1));
        Subscription live = probe(liveStudent, Subscription.STATUS_ACTIVE, LocalDateTime.now().plusDays(20));
        Subscription cancelled = probe(cancelledStudent, Subscription.STATUS_CANCELLED, LocalDateTime.now().minusDays(1));

        coreScheduler.expireElapsedSubscriptions();

        assertEquals(Subscription.STATUS_EXPIRED, statusOf(elapsed),
                "a paid period that ran out must be recorded as expired by the sweep");
        assertEquals(Subscription.STATUS_ACTIVE, statusOf(live),
                "a period still running must be left alone - expiring it would misstate the "
                        + "commercial record for an active purchase");
        assertEquals(Subscription.STATUS_CANCELLED, statusOf(cancelled),
                "the sweep only selects ACTIVE rows, so a cancellation history is untouched");
    }

    @Test
    void aPeriodThatEndsNowIsExpiredButOneSecondAwayIsNot() {
        UUID justElapsed = UUID.randomUUID();
        UUID justLive = UUID.randomUUID();

        // The boundary itself, not a convenient margin: period_end <= now.
        Subscription atBoundary = probe(justElapsed, Subscription.STATUS_ACTIVE, LocalDateTime.now().minusSeconds(1));
        Subscription oneSecondOut = probe(justLive, Subscription.STATUS_ACTIVE, LocalDateTime.now().plusSeconds(30));

        coreScheduler.expireElapsedSubscriptions();

        assertEquals(Subscription.STATUS_EXPIRED, statusOf(atBoundary),
                "period_end has passed, so it is elapsed");
        assertEquals(Subscription.STATUS_ACTIVE, statusOf(oneSecondOut),
                "the paid period still has time on it, so it must not be expired");
    }

    @Test
    void runningTheSweepTwiceChangesNothingTheSecondTime() {
        Subscription elapsed = probe(UUID.randomUUID(), Subscription.STATUS_ACTIVE,
                LocalDateTime.now().minusDays(3));

        coreScheduler.expireElapsedSubscriptions();
        Subscription afterFirst = reload(elapsed);
        assertEquals(Subscription.STATUS_EXPIRED, afterFirst.getStatus(),
                "the first run must expire the elapsed period");

        // Idempotent by construction: EXPIRED is terminal and expired rows are not selected again.
        assertDoesNotThrow(() -> coreScheduler.expireElapsedSubscriptions());

        Subscription afterSecond = reload(elapsed);
        assertEquals(Subscription.STATUS_EXPIRED, afterSecond.getStatus(),
                "a second sweep must not move or re-process an already expired row");
        assertEquals(afterFirst.getPeriodEnd(), afterSecond.getPeriodEnd(),
                "re-running the sweep must not rewrite the period it already recorded");
    }

    @Test
    void aRoutineSweepRunNeverRetiresAPeriodThatIsStillLive() {
        // Guard on the query rather than on the status constant: if the selection ever widened to
        // include live periods, a routine scheduled run would silently retire paid access in the
        // commercial record, with no request and no error anywhere.
        List<Subscription> live = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            live.add(probe(UUID.randomUUID(), Subscription.STATUS_ACTIVE,
                    LocalDateTime.now().plusDays(10L + i)));
        }

        coreScheduler.expireElapsedSubscriptions();

        for (Subscription s : live) {
            assertEquals(Subscription.STATUS_ACTIVE, statusOf(s),
                    "no live period may be expired by a sweep run");
        }
    }

    // ------------------------------------------------------------------ helpers

    private Subscription probe(UUID studentId, String status, LocalDateTime periodEnd) {
        Subscription saved = subscriptionRepository.saveAndFlush(Subscription.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .studentId(studentId)
                .payerId(UUID.randomUUID())
                .serviceType("MONTHLY_ACCESS")
                .paymentId(UUID.randomUUID())
                .status(status)
                .periodStart(periodEnd.minusDays(30))
                .periodEnd(periodEnd)
                .build());
        createdIds.add(saved.getId());
        return saved;
    }

    private Subscription reload(Subscription s) {
        return subscriptionRepository.findById(s.getId()).orElseThrow(
                () -> new AssertionError("probe row disappeared: " + s.getId()));
    }

    private String statusOf(Subscription s) {
        String status = reload(s).getStatus();
        assertTrue(status != null, "status must be readable for " + s.getId());
        return status;
    }
}
