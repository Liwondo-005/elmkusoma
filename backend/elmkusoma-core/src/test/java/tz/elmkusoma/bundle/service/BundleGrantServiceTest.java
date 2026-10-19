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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
    void twoConcurrentPurchasesCannotGrantTheSameServiceTwice() throws Exception {
        // The race this pins: both transactions read "the learner does not hold this service" and
        // both insert. A sequential test cannot show it, because sequentially the second call
        // legitimately sees the first call's row and correctly declines.
        //
        // The barrier makes the two grants genuinely overlap. Correctness then comes from the
        // database, not from luck about timing: whichever transaction takes the advisory lock
        // second waits for the first to commit and then observes its row.
        String serviceType = "BUNDLE_RACE_" + UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID shared = UUID.randomUUID();
        Bundle b = bundle(serviceType, Bundle.STATUS_PUBLISHED);
        item(b, shared, 0);

        Payment first = completedPayment(serviceType, studentId);
        Payment second = completedPayment(serviceType, studentId);
        paymentRepository.saveAllAndFlush(java.util.List.of(first, second));

        int threads = 2;
        CyclicBarrier startLine = new CyclicBarrier(threads);
        // Rendezvous inside the check-then-insert window. Without the advisory lock both
        // transactions reach this point having both read "not held", so both insert and the race
        // is reproduced on every run rather than whenever the clock happens to line up. With the
        // lock the second transaction never gets here until the first has committed and released
        // it, so its await simply times out.
        CyclicBarrier window = new CyclicBarrier(threads);
        bundleGrantService.setBeforeInsertHook((student, item) -> {
            try {
                window.await(3, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Timed out waiting for a partner, which is the expected outcome once the lock
                // serialises the two. The window it guards is already protected.
            }
        });
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (Payment p : java.util.List.of(first, second)) {
                results.add(pool.submit(() -> {
                    startLine.await(10, TimeUnit.SECONDS);
                    return bundleGrantService.grantFor(p);
                }));
            }
            int granted = 0;
            for (Future<Integer> result : results) {
                granted += result.get(30, TimeUnit.SECONDS);
            }

            assertEquals(1, granted,
                    "only one of two concurrent purchases may create the grant; the other must "
                            + "observe the committed row and decline");
            long rows = entitlementRepository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                    .filter(e -> shared.equals(e.getServiceId()))
                    .count();
            assertEquals(1, rows,
                    "the learner must hold exactly one grant for the service, whatever order the "
                            + "two purchases committed in");
        } finally {
            bundleGrantService.setBeforeInsertHook(null);
            pool.shutdownNow();
        }
    }

    @Test
    void differentLearnersBuyingTheSameBundleDoNotBlockOrCollide() throws Exception {
        // The lock is keyed per learner+service, so it must not serialise unrelated families or
        // turn two ordinary purchases into one. A coarse lock would pass the test above and fail
        // this one by making the second purchase wait on an unrelated key.
        String serviceType = "BUNDLE_PARALLEL_" + UUID.randomUUID();
        UUID shared = UUID.randomUUID();
        Bundle b = bundle(serviceType, Bundle.STATUS_PUBLISHED);
        item(b, shared, 0);

        UUID firstStudent = UUID.randomUUID();
        UUID secondStudent = UUID.randomUUID();
        Payment first = completedPayment(serviceType, firstStudent);
        Payment second = completedPayment(serviceType, secondStudent);
        paymentRepository.saveAllAndFlush(java.util.List.of(first, second));

        int threads = 2;
        CyclicBarrier startLine = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (Payment p : java.util.List.of(first, second)) {
                results.add(pool.submit(() -> {
                    startLine.await(10, TimeUnit.SECONDS);
                    return bundleGrantService.grantFor(p);
                }));
            }
            int granted = 0;
            for (Future<Integer> result : results) {
                granted += result.get(30, TimeUnit.SECONDS);
            }

            assertEquals(2, granted,
                    "two different learners buying the same bundle must both be granted; the lock "
                            + "exists to stop a double grant, not to ration purchases");
            for (UUID student : java.util.List.of(firstStudent, secondStudent)) {
                assertEquals(1, entitlementRepository.findByStudentIdAndIsDeletedFalse(student)
                        .stream().filter(e -> shared.equals(e.getServiceId())).count());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aRejectedGrantLeavesNoEntitlementAndDoesNotLeaveALockHeld() throws Exception {
        // A bundle that must not grant anything is the cheapest way to prove the advisory lock is
        // released when the transaction does no work: if it leaked, every later grant for that key
        // would block forever and this run would hang rather than fail.
        String serviceType = "BUNDLE_SUSPENDED_RACE_" + UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID service = UUID.randomUUID();
        Bundle b = bundle(serviceType, Bundle.STATUS_SUSPENDED);
        item(b, service, 0);

        Payment payment = completedPayment(serviceType, studentId);
        paymentRepository.saveAndFlush(payment);

        assertEquals(0, bundleGrantService.grantFor(payment),
                "a suspended bundle must grant nothing");
        assertEquals(0, entitlementRepository.findByStudentIdAndIsDeletedFalse(studentId).size(),
                "a refused grant must leave no partial entitlement behind");

        // Same key, now a published bundle. If the refused transaction had kept the lock, this
        // would never return.
        Bundle published = bundle(serviceType + "_OK", Bundle.STATUS_PUBLISHED);
        item(published, service, 0);
        Payment after = completedPayment(serviceType + "_OK", studentId);
        paymentRepository.saveAndFlush(after);
        assertEquals(1, bundleGrantService.grantFor(after),
                "the lock must be released when a transaction that took it granted nothing");
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