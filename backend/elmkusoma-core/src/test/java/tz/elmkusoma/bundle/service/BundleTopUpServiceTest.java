package tz.elmkusoma.bundle.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.bundle.domain.Bundle;
import tz.elmkusoma.bundle.domain.BundleItem;
import tz.elmkusoma.bundle.repository.BundleItemRepository;
import tz.elmkusoma.bundle.repository.BundleRepository;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.pricing.domain.PricingPolicy;
import tz.elmkusoma.pricing.repository.PricingPolicyRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BR-03: a learner who already holds part of a bundle must not be charged twice for it.
 *
 * <p>The interesting cases are the ones where a naive "does a matching row exist" check would be
 * wrong - an expired grant, a different service type, a not-yet-started grant - because treating
 * any of those as coverage would silently withhold a charge the learner owes.
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class BundleTopUpServiceTest {

    @Autowired private BundleTopUpService bundleTopUpService;
    @Autowired private BundleRepository bundleRepository;
    @Autowired private BundleItemRepository bundleItemRepository;
    @Autowired private EntitlementRepository entitlementRepository;
    @Autowired private PricingPolicyRepository pricingPolicyRepository;

    private Bundle publishedBundle() {
        Bundle b = bundleRepository.saveAndFlush(Bundle.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .name("Top-up probe " + UUID.randomUUID())
                .description("overlap probe")
                .serviceType("BUNDLE_TOPUP_" + UUID.randomUUID())
                .accessDays(30)
                .status(Bundle.STATUS_PUBLISHED)
                .build());
        return b;
    }

    private BundleItem item(Bundle b, String serviceType, UUID serviceId) {
        return bundleItemRepository.saveAndFlush(BundleItem.builder()
                .bundleId(b.getId())
                .serviceType(serviceType)
                .serviceId(serviceId)
                .position(0)
                .build());
    }

    private void grant(UUID studentId, String serviceType, UUID serviceId,
                       LocalDateTime starts, LocalDateTime expires, String status) {
        entitlementRepository.saveAndFlush(Entitlement.builder()
                .userId(UUID.randomUUID())
                .studentId(studentId)
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .serviceType(serviceType)
                .serviceId(serviceId)
                .status(status)
                .startsAt(starts)
                .expiresAt(expires)
                .build());
    }


    /**
     * An approved ACTIVE pricing policy for a service type.
     *
     * <p>Without one, PricingService refuses with PricingNotConfiguredException - which is
     * correct: a price is never invented to make a total work out. The fixture therefore has to
     * approve a price the way the business would.
     */
    private void activatePolicy(String serviceType, String amount) {
        pricingPolicyRepository.saveAndFlush(PricingPolicy.builder()
                .policyKey("TOPUP_PROBE_" + serviceType)
                .policyVersion(1)
                .status(PricingPolicy.STATUS_ACTIVE)
                .serviceType(serviceType)
                .defaultAmount(new java.math.BigDecimal(amount))
                .currency("TZS")
                .accessDays(30)
                .description("top-up probe policy")
                .build());
    }
    @Test
    void nothingAlreadyHeldIsCharged() {
        Bundle b = publishedBundle();
        UUID studentId = UUID.randomUUID();
        UUID held = UUID.randomUUID();
        activatePolicy("PREMIUM_ACCESS", "50000.00");
        item(b, "PREMIUM_ACCESS", held);

        // The learner already holds the only item in the bundle.
        grant(studentId, "PREMIUM_ACCESS", held,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(20), "ACTIVE");

        var quote = bundleTopUpService.quote(b.getId(), studentId, LocalDateTime.now());

        assertTrue(quote.fullyCovered(), "a bundle the learner already holds costs nothing more");
        assertEquals(1, quote.coveredCount());
        assertEquals(0, quote.chargeableCount());
        assertEquals(0, java.math.BigDecimal.ZERO.compareTo(quote.total()));
    }

    @Test
    void anExpiredEntitlementIsNotCoverage() {
        // Treating an elapsed grant as coverage would withhold a charge the learner owes. Whether
        // a grace period should apply is an unapproved question, so this takes the straightforward
        // reading and says so.
        Bundle b = publishedBundle();
        UUID studentId = UUID.randomUUID();
        UUID service = UUID.randomUUID();
        activatePolicy("PREMIUM_ACCESS", "50000.00");
        item(b, "PREMIUM_ACCESS", service);

        grant(studentId, "PREMIUM_ACCESS", service,
                LocalDateTime.now().minusDays(40), LocalDateTime.now().minusDays(10), "ACTIVE");

        var quote = bundleTopUpService.quote(b.getId(), studentId, LocalDateTime.now());

        assertFalse(quote.fullyCovered(), "an elapsed entitlement must not suppress the charge");
        assertEquals(1, quote.chargeableCount());
    }

    @Test
    void anInactiveOrFutureGrantIsNotCoverage() {
        Bundle b1 = publishedBundle();
        UUID studentId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        activatePolicy("PREMIUM_ACCESS", "50000.00");
        item(b1, "PREMIUM_ACCESS", s1);
        grant(studentId, "PREMIUM_ACCESS", s1,
                LocalDateTime.now().minusDays(40), LocalDateTime.now().plusDays(10), "REVOKED");
        assertFalse(bundleTopUpService.quote(b1.getId(), studentId, LocalDateTime.now()).fullyCovered(),
                "a revoked entitlement is not coverage");

        Bundle b2 = publishedBundle();
        UUID s2 = UUID.randomUUID();
        activatePolicy("PREMIUM_ACCESS", "50000.00");
        item(b2, "PREMIUM_ACCESS", s2);
        grant(studentId, "PREMIUM_ACCESS", s2,
                LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(30), "ACTIVE");
        assertFalse(bundleTopUpService.quote(b2.getId(), studentId, LocalDateTime.now()).fullyCovered(),
                "access that has not started yet is not coverage");
    }

    @Test
    void aDifferentServiceTypeIsNeverTreatedAsCoverage() {
        // Identity is (serviceType, serviceId). A different tier is a different thing to buy, even
        // if the access it grants looks similar.
        Bundle b = publishedBundle();
        UUID studentId = UUID.randomUUID();
        UUID service = UUID.randomUUID();
        activatePolicy("PREMIUM_ACCESS", "50000.00");
        item(b, "PREMIUM_ACCESS", service);

        grant(studentId, "BASIC_ACCESS", service,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(20), "ACTIVE");

        var quote = bundleTopUpService.quote(b.getId(), studentId, LocalDateTime.now());

        assertFalse(quote.fullyCovered(),
                "a BASIC_ACCESS grant must not suppress a PREMIUM_ACCESS charge");
    }

    @Test
    void anUnpublishedBundleCannotBeQuoted() {
        Bundle b = publishedBundle();
        item(b, "PREMIUM_ACCESS", UUID.randomUUID());
        b.setStatus(Bundle.STATUS_SUSPENDED);
        bundleRepository.saveAndFlush(b);

        assertThrows(IllegalStateException.class,
                () -> bundleTopUpService.quote(b.getId(), UUID.randomUUID(), LocalDateTime.now()),
                "a suspended bundle must not be quotable");
    }

    @Test
    void anEmptyBundleIsRefusedRatherThanPricedAtZero() {
        // A zero total for an empty bundle would look like a free purchase rather than a defect.
        Bundle b = publishedBundle();

        assertThrows(IllegalStateException.class,
                () -> bundleTopUpService.quote(b.getId(), UUID.randomUUID(), LocalDateTime.now()));
    }
}
