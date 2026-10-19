package tz.elmkusoma.finance.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Per-persona earnings are only safe to expose because the beneficiary is recorded on the payment
 * and read from the authenticated caller rather than the request.
 *
 * <p>These tests cover the two properties that make the feature trustworthy: money is only ever
 * attributed when a beneficiary was genuinely recorded, and one person can never see another's
 * earnings. The second is not a menu-visibility concern; it has to hold in the query itself.
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class BeneficiaryEarningsTest {

    @Autowired private EarningsService earningsService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;

    private Payment completedFor(UUID beneficiaryId, BigDecimal amount) {
        return paymentRepository.saveAndFlush(Payment.builder()
                .purchaserId(UUID.randomUUID())
                .purchaserType("USER")
                .beneficiaryId(beneficiaryId)
                .beneficiaryType("ORGANIZER")
                .serviceType("EVENT_TICKET")
                .amount(amount)
                .currency("TZS")
                .grossAmount(amount)
                .netAmount(amount)
                .status("COMPLETED")
                .paidAt(LocalDateTime.now())
                .commissionAmount(amount.multiply(new BigDecimal("0.10")))
                .providerShareAmount(amount.multiply(new BigDecimal("0.90")))
                // hasResolvedAllocation() requires an approved, policy-bearing allocation. Without
                // it the amount lands in the unallocated bucket, which is the correct behaviour but
                // is not what this test is about.
                .commissionPolicyKey("EARNINGS_PROBE_POLICY")
                .commissionPolicyVersion(1)
                .build());
    }

    @Test
    void earningsAreStatedOnlyForTheRecordedBeneficiary() {
        UUID organiser = userRepository.saveAndFlush(
                tz.elmkusoma.shared.domain.User.builder()
                        .email("earnings-owner-" + UUID.randomUUID() + "@test.go.tz")
                        .passwordHash("{noop}x")
                        .firstName("Earn")
                        .lastName("Owner")
                        .role(tz.elmkusoma.shared.domain.User.Role.TEACHER)
                        .institutionId(TestDataSeeder.INSTITUTION_ID)
                        .isActive(true)
                        .isEmailVerified(true)
                        .build()).getId();

        BigDecimal mine = new BigDecimal("100000.00");
        completedFor(organiser, mine);

        EarningsService.EarningsSummary summary =
                earningsService.summaryForBeneficiary(organiser, null, null);

        assertNotNull(summary, "a recorded beneficiary must produce a summary");
        assertEquals(1L, summary.completedPaymentCount());
        assertEquals(0, mine.compareTo(summary.grossCollected().value()),
                "the organiser must see the full gross they were credited with");
        assertEquals(0, new BigDecimal("90000.00").compareTo(summary.providerShare().value()),
                "the provider share must be the allocated 90%, not the gross");
    }

    @Test
    void oneBeneficiaryCanNeverSeeAnotherBeneficiarysEarnings() {
        UUID mine = UUID.randomUUID();
        UUID theirs = UUID.randomUUID();
        BigDecimal theirAmount = new BigDecimal("250000.00");
        completedFor(theirs, theirAmount);

        EarningsService.EarningsSummary summary =
                earningsService.summaryForBeneficiary(mine, null, null);

        assertEquals(0L, summary.completedPaymentCount(),
                "another beneficiary's completed payment must not appear in this summary");
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.grossCollected().value()),
                "a non-beneficiary must see zero, never somebody else's revenue");
        assertTrue(summary.grossCollected().value().signum() == 0,
                "no part of another person's earnings may leak");
    }

    @Test
    void paymentsWithNoRecordedBeneficiaryAreNeverAttributedToSomebody() {
        // The honesty guarantee. A payment with no beneficiary is unattributed, so it must not be
        // folded into anyone's earnings - not into a current role holder's, not into anyone's.
        Payment unattributed = paymentRepository.saveAndFlush(Payment.builder()
                .purchaserId(UUID.randomUUID())
                .purchaserType("PARENT")
                .serviceType("TUITION")
                .amount(new BigDecimal("500000.00"))
                .currency("TZS")
                .status("COMPLETED")
                .paidAt(LocalDateTime.now())
                .build());
        assertNotNull(unattributed.getId());

        UUID somebody = UUID.randomUUID();
        EarningsService.EarningsSummary summary =
                earningsService.summaryForBeneficiary(somebody, null, null);

        assertEquals(0L, summary.completedPaymentCount(),
                "an unattributed payment must not be assigned to any beneficiary");
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.grossCollected().value()),
                "an unattributed payment must contribute no earnings to anybody");
    }

    @Test
    void aMissingBeneficiaryIsRefusedRatherThanReturningEverything() {
        // Guards against a future caller passing null and having the filter quietly widen to
        // "no beneficiary filter", which would return every payment in the system.
        assertThrows(IllegalArgumentException.class,
                () -> earningsService.summaryForBeneficiary(null, null, null),
                "a null beneficiary must be refused, never treated as no filter");
    }
}