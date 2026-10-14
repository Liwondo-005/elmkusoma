package tz.elmkusoma.parent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.PaymentReceipt;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The duplicate-refund defect, proved end to end against a real database rather than mocks.
 *
 * <p>Two independent paths could move money on one payment. The legacy admin endpoint set
 * {@code status = REFUNDED} and revoked the entitlement, but never recorded the amount in
 * {@code refunded_amount} and never wrote a credit note. Because the refundable balance is
 * computed as {@code amount - refunded_amount}, a payment refunded that way still looked fully
 * refundable, so the governed lifecycle would refund it a second time: the same money returned
 * twice, with the receipt showing no reversal at all.</p>
 *
 * <p>Deliberately not {@code @Transactional}: a shared test transaction would give every thread
 * the same snapshot and hide exactly the race the row lock exists to prevent. Each caller runs
 * its own transaction here, which is what production does.</p>
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentRefundIntegrityTest {

    private static final BigDecimal CHARGE = new BigDecimal("50000");
    private static final UUID ACTOR = UUID.randomUUID();

    @Autowired private ParentPaymentService parentPaymentService;
    @Autowired private RefundService refundService;
    @Autowired private ReceiptService receiptService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private EntitlementRepository entitlementRepository;
    @Autowired private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    // ------------------------------------------------------------------
    // Fixtures: a completed payment, the receipt it issued and the
    // entitlement it granted - the shape a real confirmed payment has.
    // ------------------------------------------------------------------

    private Payment seedCompletedPayment() {
        UUID studentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        // The id is left to Hibernate: BaseEntity generates it, so assigning one here would make
        // the row look detached and the insert would be mistaken for an update.
        Payment payment = Payment.builder()
                .parentId(UUID.randomUUID())
                .studentId(studentId)
                .serviceType("TUITION")
                .serviceId(serviceId)
                .amount(CHARGE)
                .currency("TZS")
                .status("COMPLETED")
                .paidAt(LocalDateTime.now().minusDays(2))
                .build();
        payment.setInstitutionId(TestDataSeeder.INSTITUTION_ID);

        // One transaction, because issueFor is MANDATORY-propagated: a completed payment always
        // has its receipt, and a credit note can only be recorded against an existing receipt.
        return transactionTemplate.execute(status -> {
            Payment saved = paymentRepository.saveAndFlush(payment);
            entitlementRepository.saveAndFlush(Entitlement.builder()
                    .userId(saved.getParentId())
                    .studentId(studentId)
                    .serviceType("TUITION")
                    .serviceId(serviceId)
                    .paymentId(saved.getId())
                    .status("ACTIVE")
                    .startsAt(LocalDateTime.now().minusDays(2))
                    .expiresAt(LocalDateTime.now().plusDays(28))
                    .build());
            receiptService.issueFor(saved, "Tuition");
            return saved;
        });
    }

    private Payment reload(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow();
    }

    private Entitlement entitlementFor(Payment payment) {
        return entitlementRepository
                .findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                        payment.getStudentId(), payment.getServiceType(), payment.getServiceId())
                .orElseThrow();
    }

    // ------------------------------------------------------------------
    // The accounting fix: the legacy path records what it gave back
    // ------------------------------------------------------------------

    @Test
    void legacyRefund_recordsTheAmountAndACreditNote_ratherThanOnlyTheStatus() {
        Payment payment = seedCompletedPayment();

        Payment refunded = parentPaymentService.refundPayment(payment.getId(), ACTOR, "card stolen");

        // The refundable balance is what keeps a second refund impossible, so it has to be
        // written here - precisely what the old path omitted.
        assertEquals(0, CHARGE.compareTo(refunded.getRefundedAmount()),
                "the legacy path must record the refunded amount, not just the status");
        assertEquals("REFUNDED", refunded.getStatus());

        // Revoke, not delete: the history of what was granted has to survive.
        Entitlement entitlement = entitlementFor(payment);
        assertEquals("REVOKED", entitlement.getStatus());
        assertNotNull(entitlement.getExpiresAt());

        // The financial record is annotated with a credit note, never left showing full payment.
        PaymentReceipt receipt = receiptService.findByPayment(payment.getId()).orElse(null);
        assertNotNull(receipt, "a credit note must be recorded for the refund");
        assertTrue(receipt.hasCreditNote());
    }

    // ------------------------------------------------------------------
    // The defect this increment exists to close
    // ------------------------------------------------------------------

    @Test
    void legacyRefund_thenAGovernedRefund_isRefusedBecauseTheBalanceIsExhausted() {
        Payment payment = seedCompletedPayment();

        parentPaymentService.refundPayment(payment.getId(), ACTOR, "duplicate charge");

        // Before the fix this succeeded: the payment still looked fully refundable, so the same
        // 50,000 was handed back a second time through the governed lifecycle.
        RefundService.RefundException ex = assertThrows(RefundService.RefundException.class, () ->
                refundService.requestRefund(payment.getId(), ACTOR, CHARGE, "again", null));

        assertTrue(ex.getMessage().contains("refundable"),
                "refusal should name the exhausted balance, said: " + ex.getMessage());
        assertEquals(0, CHARGE.compareTo(reload(payment.getId()).getRefundedAmount()),
                "nothing may be written a second time");
    }

    @Test
    void legacyRefund_twice_isRefusedTheSecondTime() {
        Payment payment = seedCompletedPayment();

        parentPaymentService.refundPayment(payment.getId(), ACTOR, "first");

        // A full refund leaves the status REFUNDED, so the preserved "only completed" rule
        // refuses the second call before the balance guard is ever needed. Either guard is a
        // refusal; what matters is that the money is not returned twice.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                parentPaymentService.refundPayment(payment.getId(), ACTOR, "second"));

        assertEquals("Only completed payments can be refunded", ex.getMessage());
        assertEquals(0, CHARGE.compareTo(reload(payment.getId()).getRefundedAmount()),
                "the amount must be one refund, not doubled");
    }

    @Test
    void concurrentLegacyRefunds_exactlyOneSucceedsAndTheAmountIsNeverDoubled() throws Exception {
        Payment payment = seedCompletedPayment();
        int callers = 4;
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Object>> outcomes = new ArrayList<>();

        try {
            for (int i = 0; i < callers; i++) {
                outcomes.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        parentPaymentService.refundPayment(payment.getId(), ACTOR, "race");
                        return "ok";
                    } catch (RuntimeException e) {
                        return e.getClass().getSimpleName();
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "callers should be ready");
            go.countDown();

            int succeeded = 0;
            for (Future<Object> outcome : outcomes) {
                if ("ok".equals(outcome.get(30, TimeUnit.SECONDS))) {
                    succeeded++;
                }
            }

            assertEquals(1, succeeded, "exactly one concurrent refund may succeed");
            Payment after = reload(payment.getId());
            assertEquals(0, CHARGE.compareTo(after.getRefundedAmount()),
                    "the amount refunded must be one refund, not one per caller");
            assertEquals("REFUNDED", after.getStatus());
        } finally {
            pool.shutdownNow();
        }
    }

    // ------------------------------------------------------------------
    // Behaviour that had to be preserved
    // ------------------------------------------------------------------

    @Test
    void legacyRefund_stillRefusesAPaymentThatIsNotCompleted() {
        // Preserved legacy contract: only a completed payment can be refunded here. A payment
        // left PARTIALLY_REFUNDED by the governed lifecycle is refused, not silently topped up.
        Payment payment = seedCompletedPayment();
        payment.setStatus("PENDING");
        paymentRepository.saveAndFlush(payment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                parentPaymentService.refundPayment(payment.getId(), ACTOR, "too early"));

        assertEquals("Only completed payments can be refunded", ex.getMessage());
    }

    @Test
    void governedPartialRefund_leavesTheRemainderRefundable_andTheLegacyPathRefusesIt() {
        // A genuine partial refund must still work and leave the remainder available to the
        // governed lifecycle - but the legacy single-step endpoint is full-refund only, so it
        // refuses a payment that is no longer COMPLETED. That distinction is the contract.
        Payment payment = seedCompletedPayment();
        BigDecimal partial = new BigDecimal("20000");

        var request = refundService.requestRefund(
                payment.getId(), ACTOR, partial, "partial correction", null);
        refundService.approve(request.getId(), ACTOR, "ok", true);
        refundService.completeRefund(request.getId(), ACTOR, "gw-part-1");

        Payment after = reload(payment.getId());
        assertEquals("PARTIALLY_REFUNDED", after.getStatus());
        assertEquals(0, partial.compareTo(after.getRefundedAmount()));
        assertEquals(0, new BigDecimal("30000").compareTo(after.refundableAmount()),
                "the remaining balance stays refundable");

        assertThrows(IllegalStateException.class, () ->
                parentPaymentService.refundPayment(payment.getId(), ACTOR, "legacy top-up"));
    }
}
