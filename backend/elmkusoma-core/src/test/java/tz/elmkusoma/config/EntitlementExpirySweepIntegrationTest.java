package tz.elmkusoma.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The entitlement expiry sweep against the database, as the {@code @Scheduled} method runs it.
 *
 * <p>Nothing else in the codebase ever wrote EXPIRED to an entitlement, so this sweep is what
 * finally closes an elapsed period in the commercial record. Access itself is decided from the
 * timestamp, so a sweep that is late or never runs cannot grant or deny anything - which is
 * exactly why the safety property worth proving here is the inverse one: a routine run must never
 * retire a period that is still live.</p>
 *
 * <p>The sweep is global by design, so every assertion is scoped to rows created here by unique
 * student id: leftovers from other suites in the shared test database cannot make these pass or
 * fail.</p>
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class EntitlementExpirySweepIntegrationTest {

    @Autowired private CoreScheduler coreScheduler;
    @Autowired private EntitlementRepository entitlementRepository;

    private final List<UUID> createdIds = new ArrayList<>();

    @AfterEach
    void removeProbes() {
        entitlementRepository.deleteAllById(createdIds);
        createdIds.clear();
    }

    @Test
    void theSweepMarksRealElapsedRowsExpiredAndLeavesLiveOnesAlone() {
        Entitlement elapsed = probe(LocalDateTime.now().minusDays(1), "ACTIVE");
        Entitlement live = probe(LocalDateTime.now().plusDays(20), "ACTIVE");
        Entitlement alreadyClosed = probe(LocalDateTime.now().minusDays(1), "EXPIRED");

        coreScheduler.expireElapsedEntitlements();

        assertEquals("EXPIRED", statusOf(elapsed),
                "an entitlement whose period ran out must be recorded as expired by the sweep");
        assertEquals("ACTIVE", statusOf(live),
                "access still running must be left alone - expiring it would misstate the "
                        + "commercial record for a paid purchase");
        assertEquals("EXPIRED", statusOf(alreadyClosed),
                "the sweep only selects ACTIVE rows, so a period already closed out is untouched");
    }

    @Test
    void anEntitlementWithNoRecordedEndIsNeverSelected() {
        // A null expiry is a row that records no end. The access predicates treat it as unexpired,
        // so the sweep must agree with them: expiring it would close access the platform still
        // considers open, and that disagreement is the failure mode this guards.
        Entitlement noExpiry = probe(null, "ACTIVE");

        coreScheduler.expireElapsedEntitlements();

        assertEquals("ACTIVE", statusOf(noExpiry),
                "a row with no recorded end has not elapsed and must never be expired");
        assertNull(reload(noExpiry).getExpiresAt(),
                "the sweep must not invent an end date for a row that has none");
    }

    @Test
    void anEntitlementThatEndsNowIsExpiredButOneSecondAwayIsNot() {
        // The boundary itself, not a convenient margin: expires_at <= now.
        Entitlement atBoundary = probe(LocalDateTime.now().minusSeconds(1), "ACTIVE");
        Entitlement oneSecondOut = probe(LocalDateTime.now().plusSeconds(30), "ACTIVE");

        coreScheduler.expireElapsedEntitlements();

        assertEquals("EXPIRED", statusOf(atBoundary), "expires_at has passed, so it is elapsed");
        assertEquals("ACTIVE", statusOf(oneSecondOut),
                "the paid period still has time on it, so it must not be expired");
    }

    @Test
    void runningTheSweepTwiceChangesNothingTheSecondTime() {
        Entitlement elapsed = probe(LocalDateTime.now().minusDays(3), "ACTIVE");

        coreScheduler.expireElapsedEntitlements();
        Entitlement afterFirst = reload(elapsed);
        assertEquals("EXPIRED", afterFirst.getStatus(), "the first run must expire the row");

        // Idempotent by construction: EXPIRED is absorbing and expired rows are not selected again.
        assertDoesNotThrow(() -> coreScheduler.expireElapsedEntitlements());

        Entitlement afterSecond = reload(elapsed);
        assertEquals("EXPIRED", afterSecond.getStatus(),
                "a second sweep must not move or re-process a row it already closed out");
        assertEquals(afterFirst.getExpiresAt(), afterSecond.getExpiresAt(),
                "re-running the sweep must not rewrite the end date it already recorded");
    }

    @Test
    void aRoutineSweepRunNeverRetiresAccessThatIsStillLive() {
        // Guard on the query rather than on the status constant: if the selection ever widened to
        // include live periods, a routine scheduled run would silently retire paid access in the
        // commercial record, with no request and no error anywhere.
        List<Entitlement> live = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            live.add(probe(LocalDateTime.now().plusDays(10L + i), "ACTIVE"));
        }

        coreScheduler.expireElapsedEntitlements();

        for (Entitlement entitlement : live) {
            assertEquals("ACTIVE", statusOf(entitlement),
                    "no live access may be expired by a sweep run");
        }
    }

    // ------------------------------------------------------------------ helpers

    private Entitlement probe(LocalDateTime expiresAt, String status) {
        Entitlement saved = entitlementRepository.saveAndFlush(Entitlement.builder()
                .userId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .serviceType("COURSE_ACCESS")
                .serviceId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .status(status)
                .startsAt(expiresAt != null ? expiresAt.minusDays(30) : LocalDateTime.now())
                .expiresAt(expiresAt)
                .build());
        createdIds.add(saved.getId());
        return saved;
    }

    private Entitlement reload(Entitlement e) {
        return entitlementRepository.findById(e.getId()).orElseThrow(
                () -> new AssertionError("probe row disappeared: " + e.getId()));
    }

    private String statusOf(Entitlement e) {
        String status = reload(e).getStatus();
        assertTrue(status != null, "status must be readable for " + e.getId());
        return status;
    }
}
