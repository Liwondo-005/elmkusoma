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
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A bundle sells access to several services, so the properties that matter are about access and
 * money rather than rows: every item is granted, a repeat callback grants nothing twice, an
 * already-held service is not re-created, and a bundle that is not published cannot be bought.
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class BundleGrantServiceTest {

    @Autowired private BundleGrantService bundleGrantService;
    @Autowired private BundleRepository bundleRepository;
    @Autowired private BundleItemRepository bundleItemRepository;
    @Autowired private EntitlementRepository entitlementRepository;
    @Autowired private tz.elmkusoma.parent.repository.PaymentRepository paymentRepository;

    private UUID serviceA;
    private UUID serviceB;

    private Bundle bundle(String serviceType, String status) {
        Bundle b = Bundle.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .name("Bundle " + serviceType)
                .description("bundle grant probe")
                .serviceType(serviceType)
                .accessDays(30)
                .status(status)
                .build();
        return bundleRepository.saveAndFlush(b);
    }

    private void item(Bundle b, UUID serviceId, int position) {
        bundleItemRepository.saveAndFlush(BundleItem.builder()
                .bundleId(b.getId())
                .serviceType("COURSE_ACCESS")
                .serviceId(serviceId)
                .position(position)
                .build());
    }

    private Payment completedPayment(String serviceType, UUID studentId) {
        return Payment.builder()
                .purchaserId(UUID.randomUUID())
                .purchaserType("PARENT")
                .studentId(studentId)
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .serviceType(serviceType)
                .amount(new BigDecimal("120000.00"))
                .currency("TZS")
                .status("COMPLETED")
                .paidAt(LocalDateTime.now())
                .accessDays(30)
                .build();
    }

    @Test
    void everyItemInTheBundleIsGranted() {
        String serviceType = "BUNDLE_ALL_" + UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        serviceA = UUID.randomUUID();
        serviceB = UUID.randomUUID();
        Bundle b = bundle(serviceType, Bundle.STATUS_PUBLISHED);
        item(b, serviceA, 0);
        item(b, serviceB, 1);

        int granted = bundleGrantService.grantFor(completedPayment(serviceType, studentId));

        assertEquals(2, granted, "both items in the bundle must be granted");
        assertTrue(entitlementRepository
                .existsByStudentIdAndServiceTypeAndServiceIdAndStatusAndIsDeletedFalse(
                        studentId, "COURSE_ACCESS", serviceA, "ACTIVE"),
                "the first item must be granted");
        assertTrue(entitlementRepository
                .existsByStudentIdAndServiceTypeAndServiceIdAndStatusAndIsDeletedFalse(
                        studentId, "COURSE_ACCESS", serviceB, "ACTIVE"),
                "the second item must be granted");
    }

    @Test
    void aRepeatedCallbackGrantsNothingTwice() {
        String serviceType = "BUNDLE_REPEAT_" + UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Bundle b = bundle(serviceType, Bundle.STATUS_PUBLISHED);
        item(b, serviceId, 0);
        Payment payment = completedPayment(serviceType, studentId);

        assertEquals(1, bundleGrantService.grantFor(payment));
        // The same verified payment arriving again, as a retried webhook would.
        assertEquals(0, bundleGrantService.grantFor(payment),
                "a duplicate callback must not grant a second copy of the same access");

        long held = entitlementRepository
                .findByStudentIdAndIsDeletedFalse(studentId)
                .stream()
                .filter(e -> serviceId.equals(e.getServiceId()))
                .count();
        assertEquals(1, held, "the learner must hold exactly one grant for the bundle item");
    }

    @Test
    void aServiceAlreadyHeldIsNotReGranted() {
        String serviceType = "BUNDLE_OVERLAP_" + UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID shared = UUID.randomUUID();
        UUID extra = UUID.randomUUID();
        Bundle b = bundle(serviceType, Bundle.STATUS_PUBLISHED);
        item(b, shared, 0);
        item(b, extra, 1);

        // The learner already holds the first service independently.
        entitlementRepository.saveAndFlush(Entitlement.builder()
                .userId(UUID.randomUUID())
                .studentId(studentId)
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .serviceType("COURSE_ACCESS")
                .serviceId(shared)
                .status("ACTIVE")
                .startsAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build());

        int granted = bundleGrantService.grantFor(completedPayment(serviceType, studentId));

        assertEquals(1, granted,
                "only the item not already held may be granted; the held one is left alone "
                        + "rather than silently extended, which would be a commercial decision");
    }

    @Test
    void aBundleThatIsNotPublishedCannotBeGranted() {
        // Suspension and draft are sale rules, not display filters. Discovery hides them; the
        // grant path is what must refuse.
        for (String status : new String[] {Bundle.STATUS_DRAFT, Bundle.STATUS_SUSPENDED}) {
            String serviceType = "BUNDLE_" + status + "_" + UUID.randomUUID();
            UUID studentId = UUID.randomUUID();
            UUID serviceId = UUID.randomUUID();
            Bundle b = bundle(serviceType, status);
            item(b, serviceId, 0);

            assertEquals(0, bundleGrantService.grantFor(completedPayment(serviceType, studentId)),
                    "a " + status + " bundle must not grant access");

            assertFalse(entitlementRepository
                            .existsByStudentIdAndServiceTypeAndServiceIdAndStatusAndIsDeletedFalse(
                                    studentId, "COURSE_ACCESS", serviceId, "ACTIVE"),
                    "a " + status + " bundle must not produce an entitlement");
        }
    }

@Test
    void theDurationIsTheOneSnapshottedOnThePaymentNotTheBundle() {
        // The payer bought a specific number of days. If the bundle is later edited, that figure
        // is what this payment promised, so the grant must follow the payment.
        String serviceType = "BUNDLE_SNAPSHOT_" + UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Bundle b = bundle(serviceType, Bundle.STATUS_PUBLISHED);
        item(b, serviceId, 0);

        Payment payment = completedPayment(serviceType, studentId);
        payment.setAccessDays(7);
        payment = paymentRepository.saveAndFlush(payment);

        assertEquals(1, bundleGrantService.grantFor(payment));
        Entitlement granted = entitlementRepository
                .findByStudentIdAndIsDeletedFalse(studentId).stream()
                .filter(e -> serviceId.equals(e.getServiceId()))
                .findFirst().orElseThrow();

        long days = java.time.Duration.between(granted.getStartsAt(), granted.getExpiresAt()).toDays();
        assertEquals(7L, days,
                "access must run for the days the payer bought, not the days the bundle happens "
                        + "to say now");
    }

    @Test
    void noDurationIsInventedWhenNeitherSourceHasOne() {
        // A published bundle always carries a duration, so this state needs a bad row to reach.
        // It is reachable by a direct write, and the answer must not be a guess: granting access of
        // an invented length is a promise the platform cannot keep and nobody paid for.
        String serviceType = "BUNDLE_NODURATION_" + UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Bundle b = bundle(serviceType, Bundle.STATUS_PUBLISHED);
        b.setAccessDays(null);
        bundleRepository.saveAndFlush(b);
        item(b, serviceId, 0);

        Payment payment = completedPayment(serviceType, studentId);
        payment.setAccessDays(null);
        payment = paymentRepository.saveAndFlush(payment);

        assertEquals(0, bundleGrantService.grantFor(payment),
                "with no recorded duration the grant must be refused, not defaulted");
        assertEquals(0, entitlementRepository.findByStudentIdAndIsDeletedFalse(studentId).size(),
                "a payment with no known period must leave no entitlement behind");
    }

    @Test
    void aPaymentThatIsNotCompleteGrantsNothing() {
        String serviceType = "BUNDLE_PENDING_" + UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Bundle b = bundle(serviceType, Bundle.STATUS_PUBLISHED);
        item(b, UUID.randomUUID(), 0);

        Payment pending = Payment.builder()
                .purchaserId(UUID.randomUUID())
                .purchaserType("PARENT")
                .studentId(studentId)
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .serviceType(serviceType)
                .amount(new BigDecimal("120000.00"))
                .currency("TZS")
                .status("PENDING")
                .accessDays(30)
                .build();

        assertEquals(0, bundleGrantService.grantFor(pending),
                "access must never come from a payment that is not verified");
        assertEquals(0, entitlementRepository.findByStudentIdAndIsDeletedFalse(studentId).size(),
                "an unverified payment must leave no entitlement behind");
    }
}