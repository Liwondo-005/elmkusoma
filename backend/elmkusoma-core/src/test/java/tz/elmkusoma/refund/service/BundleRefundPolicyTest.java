package tz.elmkusoma.refund.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BR-02: a partial bundle refund must be deterministic, bounded, and restricted to Platform Admin.
 *
 * <p>The properties under test are the ones that keep money honest - the split must always add
 * back to exactly the refunded amount, an over-refund must be impossible, and authorisation must
 * be enforced on the server rather than by hiding a control.
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class BundleRefundPolicyTest {

    @Autowired private BundleRefundService bundleRefundService;
    @Autowired private PaymentRepository paymentRepository;

    private Payment paidBundle(BigDecimal net, BigDecimal commission, BigDecimal refunded) {
        return paymentRepository.saveAndFlush(Payment.builder()
                .purchaserId(UUID.randomUUID())
                .purchaserType("PARENT")
                .studentId(UUID.randomUUID())
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .serviceType("BUNDLE_TEST")
                .amount(net)
                .grossAmount(net)
                .netAmount(net)
                .commissionAmount(commission)
                .providerShareAmount(net.subtract(commission))
                .refundedAmount(refunded)
                .currency("TZS")
                .status("COMPLETED")
                .accessDays(30)
                .build());
    }

    @Test
    void theSplitAlwaysAddsBackToExactlyTheRefundedAmount() {
        // Rounding is the classic way a refund ledger starts disagreeing with itself. The provider
        // share absorbs the residue so the two parts are exhaustive.
        Payment p = paidBundle(new BigDecimal("120000.00"), new BigDecimal("12000.00"), BigDecimal.ZERO);
        BigDecimal requested = new BigDecimal("33333.33");

        BundleRefundCalculator.RefundBreakdown b = BundleRefundCalculator.breakdown(p, requested);

        assertEquals(0, requested.compareTo(
                b.commissionReversed().add(b.providerReversed())),
                "commission and provider portions must sum to exactly the refunded amount");
        assertEquals(0, requested.compareTo(b.refundedTotal()));
    }

    @Test
    void theSameInputsAlwaysProduceTheSameResult() {
        // Auditable after the fact only if it is reproducible. No clock, no randomness, no policy
        // lookup - the recorded figures are used precisely so today's policy cannot rewrite history.
        Payment p = paidBundle(new BigDecimal("90000.00"), new BigDecimal("9000.00"), BigDecimal.ZERO);

        BundleRefundCalculator.RefundBreakdown first =
                BundleRefundCalculator.breakdown(p, new BigDecimal("25000.00"));
        BundleRefundCalculator.RefundBreakdown second =
                BundleRefundCalculator.breakdown(p, new BigDecimal("25000.00"));

        assertEquals(0, first.commissionReversed().compareTo(second.commissionReversed()));
        assertEquals(0, first.providerReversed().compareTo(second.providerReversed()));
        assertEquals(0, new BigDecimal("2500.00").compareTo(first.commissionReversed()),
                "10% of 25000 against a recorded 10% commission");
    }

    @Test
    void aRefundCanNeverExceedWhatRemains() {
        Payment p = paidBundle(new BigDecimal("100000.00"), new BigDecimal("10000.00"),
                new BigDecimal("40000.00"));

        assertEquals(0, new BigDecimal("60000.00").compareTo(
                        BundleRefundCalculator.eligibleRemaining(p)),
                "eligible is net minus what has already been refunded");

        assertThrows(IllegalArgumentException.class,
                () -> BundleRefundCalculator.breakdown(p, new BigDecimal("60000.01")),
                "one cent over the remaining amount must be refused");
    }

    @Test
    void repeatedRefundsCannotTogetherExceedThePayment() {
        Payment p = paidBundle(new BigDecimal("100000.00"), new BigDecimal("10000.00"),
                new BigDecimal("40000.00"));
        BigDecimal remaining = BundleRefundCalculator.eligibleRemaining(p);

        assertThrows(IllegalArgumentException.class,
                () -> BundleRefundCalculator.breakdown(p, remaining.add(new BigDecimal("0.01"))),
                "successive refunds must not be able to drain more than was collected");
    }

    @Test
    void aNonPositiveRefundIsRefused() {
        Payment p = paidBundle(new BigDecimal("100000.00"), new BigDecimal("10000.00"), BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class,
                () -> BundleRefundCalculator.breakdown(p, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> BundleRefundCalculator.breakdown(p, new BigDecimal("-500.00")));
    }

    @Test
    void onlyAPlatformAdminMayRequestARefund() {
        Payment p = paidBundle(new BigDecimal("100000.00"), new BigDecimal("10000.00"), BigDecimal.ZERO);
        UUID admin = UUID.randomUUID();

        // A hidden button is not access control; the server must refuse regardless of the UI.
        for (String roles : new String[] {"STUDENT", "TEACHER", "INSTITUTION_ADMIN", "PROVIDER_ADMIN",
                "student,teacher", "", null}) {
            assertThrows(ForbiddenException.class,
                    () -> bundleRefundService.requestPartialRefund(
                            p.getId(), new BigDecimal("1000.00"), admin, "probe", roles),
                    "roles [" + roles + "] must not be able to request a refund");
        }
    }

    @Test
    void aPlatformAdminRequestProducesARequestInTheEstablishedLifecycle() {
        Payment p = paidBundle(new BigDecimal("100000.00"), new BigDecimal("10000.00"), BigDecimal.ZERO);

        var request = bundleRefundService.requestPartialRefund(
                p.getId(), new BigDecimal("25000.00"), UUID.randomUUID(),
                "learner withdrew from the bundle", "NATIONAL_ADMIN");

        assertTrue(request != null, "a Platform Admin request must be recorded");
        assertEquals("REQUESTED",
                request.getStatus(),
                "a refund must enter the lifecycle as a request, not be completed inline; approval, "
                        + "processing and failure stay distinct states");
    }

    @Test
    void anUnverifiedPaymentCannotBeRefunded() {
        Payment p = paidBundle(new BigDecimal("100000.00"), new BigDecimal("10000.00"), BigDecimal.ZERO);
        p.setStatus("PENDING");
        paymentRepository.saveAndFlush(p);

        assertThrows(IllegalStateException.class,
                () -> bundleRefundService.requestPartialRefund(
                        p.getId(), new BigDecimal("1000.00"), UUID.randomUUID(), "probe", "ADMIN"),
                "only a completed payment may be refunded");
    }
}