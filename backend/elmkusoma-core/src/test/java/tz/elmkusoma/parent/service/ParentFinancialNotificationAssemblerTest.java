package tz.elmkusoma.parent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.parent.domain.ParentNotificationPreference;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.dto.response.ParentNotificationResponse;
import tz.elmkusoma.parent.repository.ParentNotificationPreferenceRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Behaviour of the synthesised financial inbox.
 *
 * <p>Pure unit tests with no Spring context, no database and no shared fixture, so
 * they do not depend on the contended schema. The point of each case is an
 * invariant: who may see a payment, whether a preference is honoured, and whether a
 * repeated callback can produce a second alert.</p>
 */
@ExtendWith(MockitoExtension.class)
class ParentFinancialNotificationAssemblerTest {

    private static final UUID PARENT = UUID.randomUUID();
    private static final UUID CHILD = UUID.randomUUID();

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ParentNotificationPreferenceRepository preferenceRepository;

    private ParentFinancialNotificationAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ParentFinancialNotificationAssembler(paymentRepository, preferenceRepository);
    }

    private Payment payment(String status, String receiptNumber, BigDecimal refunded) {
        Payment payment = Payment.builder()
                .parentId(PARENT)
                .studentId(CHILD)
                .institutionId(UUID.randomUUID())
                .amount(new BigDecimal("5000"))
                .currency("TZS")
                .serviceType("PREMIUM_ACCESS")
                .status(status)
                .receiptNumber(receiptNumber)
                .refundedAmount(refunded)
                .build();
        payment.setId(UUID.randomUUID());
        payment.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        return payment;
    }

    private void givenPayments(Payment... payments) {
        when(preferenceRepository.findByParentId(PARENT))
                .thenReturn(Optional.of(new ParentNotificationPreference()));
        when(paymentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(PARENT))
                .thenReturn(List.of(payments));
    }

    @Test
    void aCompletedPaymentProjectsPaymentAndReceiptItems() {
        givenPayments(payment("COMPLETED", "RCP-001", BigDecimal.ZERO));

        List<ParentNotificationResponse.NotificationItem> items = assembler.assemble(PARENT, CHILD, "Amina");

        assertEquals(2, items.size(), "a settled payment yields both a payment and a receipt item");
        assertTrue(items.stream().allMatch(i -> "Amina".equals(i.getChildName())));
    }

    @Test
    void onlyThePaymentsBelongingToThisParentAreEverRead() {
        givenPayments(payment("COMPLETED", "RCP-001", BigDecimal.ZERO));

        assembler.assemble(PARENT, CHILD, "Amina");

        // The repository is always keyed by this parent, which is the ownership check.
        org.mockito.Mockito.verify(paymentRepository)
                .findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(PARENT);
    }

    @Test
    void paymentsForAnotherChildAreFilteredOut() {
        Payment other = payment("COMPLETED", "RCP-002", BigDecimal.ZERO);
        other.setStudentId(UUID.randomUUID());
        givenPayments(payment("COMPLETED", "RCP-001", BigDecimal.ZERO), other);

        List<ParentNotificationResponse.NotificationItem> items = assembler.assemble(PARENT, CHILD, "Amina");

        assertTrue(items.stream().noneMatch(i -> "RCP-002".equals(i.getDetail()) || "RCP-002".equals(i.getTitle())),
                "a sibling's payment must not appear in this child's inbox");
    }

    @Test
    void aTurnedOffFeePreferenceSuppressesEveryFinancialItem() {
        when(preferenceRepository.findByParentId(PARENT)).thenReturn(Optional.of(
                ParentNotificationPreference.builder().feeAlerts(false).build()));

        assertTrue(assembler.assemble(PARENT, CHILD, "Amina").isEmpty(),
                "a parent who turned fee alerts off must not receive them");
    }

    @Test
    void aFullyRefundedPaymentIsReportedSeparatelyFromItsPayment() {
        givenPayments(payment("COMPLETED", "RCP-001", new BigDecimal("5000")));

        List<ParentNotificationResponse.NotificationItem> items = assembler.assemble(PARENT, CHILD, "Amina");

        assertEquals(3, items.size(), "payment, receipt and refund must all be visible");
        assertTrue(items.stream().anyMatch(i -> "REFUND".equals(i.getType())),
                "a refund the payer never sees is a silent reversal");
        assertTrue(items.stream().filter(i -> "REFUND".equals(i.getType()))
                        .allMatch(i -> "HIGH".equals(i.getPriority())),
                "a reversal deserves the highest priority");
    }

    @Test
    void aPartiallyRefundedPaymentIsNotYetReportedAsRefunded() {
        givenPayments(payment("COMPLETED", "RCP-001", new BigDecimal("1000")));

        List<ParentNotificationResponse.NotificationItem> items = assembler.assemble(PARENT, CHILD, "Amina");

        assertFalse(items.stream().anyMatch(i -> "REFUND".equals(i.getType())),
                "a partial refund is not a reversal of the whole payment");
    }

    @Test
    void aPendingPaymentProducesNoNoise() {
        givenPayments(payment("PENDING", null, BigDecimal.ZERO));

        assertTrue(assembler.assemble(PARENT, CHILD, "Amina").isEmpty(),
                "a payment that has not completed is not yet news");
    }

    @Test
    void theSamePaymentAlwaysProjectsTheSameItemId() {
        Payment payment = payment("COMPLETED", "RCP-001", BigDecimal.ZERO);
        givenPayments(payment);

        String firstId = assembler.assemble(PARENT, CHILD, "Amina").get(0).getId();
        String secondId = assembler.assemble(PARENT, CHILD, "Amina").get(0).getId();

        assertEquals(firstId, secondId,
                "a repeated callback must not mint a second, differently-identified alert");
        assertTrue(firstId.startsWith("payment-") || firstId.startsWith("receipt-"),
                "ids are derived from the payment, not generated: " + firstId);
    }

    @Test
    void highPriorityItemsSortAboveNormalOnes() {
        givenPayments(payment("FAILED", null, BigDecimal.ZERO),
                payment("COMPLETED", "RCP-001", BigDecimal.ZERO));

        List<ParentNotificationResponse.NotificationItem> items = assembler.assemble(PARENT, CHILD, "Amina");

        assertEquals("PAYMENT", items.get(0).getType());
        assertEquals("HIGH", items.get(0).getPriority(),
                "money that did not move must outrank a routine receipt");
    }

    @Test
    void aFailedPaymentIsHighPriority() {
        givenPayments(payment("FAILED", null, BigDecimal.ZERO));

        List<ParentNotificationResponse.NotificationItem> items = assembler.assemble(PARENT, CHILD, "Amina");

        assertEquals(1, items.size());
        assertEquals("HIGH", items.get(0).getPriority());
    }

    @Test
    void anAbsentPreferenceFallsBackToTheDefaultRatherThanHidingMoney() {
        when(preferenceRepository.findByParentId(PARENT)).thenReturn(Optional.empty());
        when(paymentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(PARENT))
                .thenReturn(List.of(payment("COMPLETED", "RCP-001", BigDecimal.ZERO)));

        assertFalse(assembler.assemble(PARENT, CHILD, "Amina").isEmpty(),
                "a parent who never set a preference still gets financial alerts");
    }

    @Test
    void aMissingParentYieldsNothingInsteadOfEveryonesRecords() {
        assertTrue(assembler.assemble(null, CHILD, "Amina").isEmpty());
        org.mockito.Mockito.verifyNoInteractions(paymentRepository);
    }

    @Test
    void anUnreadablePreferenceDegradesToShowingAlerts() {
        when(preferenceRepository.findByParentId(PARENT))
                .thenThrow(new RuntimeException("preference store unavailable"));
        when(paymentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(PARENT))
                .thenReturn(List.of(payment("COMPLETED", "RCP-001", BigDecimal.ZERO)));

        assertFalse(assembler.assemble(PARENT, CHILD, "Amina").isEmpty(),
                "a broken preference lookup must not silently hide a parent's payment");
    }

    @Test
    void assemblingWithoutAChildFilterCoversEveryChild() {
        Payment other = payment("COMPLETED", "RCP-002", BigDecimal.ZERO);
        other.setStudentId(UUID.randomUUID());
        givenPayments(payment("COMPLETED", "RCP-001", BigDecimal.ZERO), other);

        List<ParentNotificationResponse.NotificationItem> items = assembler.assemble(PARENT, null, "your child");

        assertEquals(4, items.size(), "both children's payments belong in the all-children view");
    }

    @Test
    void itemsAreNeverMarkedReadByTheProjection() {
        givenPayments(payment("COMPLETED", "RCP-001", BigDecimal.ZERO));

        assertTrue(assembler.assemble(PARENT, CHILD, "Amina").stream()
                        .allMatch(i -> !Boolean.TRUE.equals(i.getIsRead())),
                "read state is the inbox's concern, not the projection's");
    }

    @Test
    void malformedRecordsDoNotThrow() {
        Payment bare = new Payment();
        bare.setId(UUID.randomUUID());
        bare.setStatus("COMPLETED");
        bare.setCreatedAt(LocalDateTime.now());
        givenPayments(bare);

        List<ParentNotificationResponse.NotificationItem> items = assembler.assemble(PARENT, CHILD, "Amina");

        assertTrue(items.stream().noneMatch(i -> i.getDetail().contains("null")),
                "a record missing its amount must still render something readable");
    }

    @Test
    void aSavedEntityReadBackStillProjects() {
        when(preferenceRepository.findByParentId(PARENT)).thenReturn(Optional.empty());
        when(paymentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(payment("COMPLETED", "RCP-001", BigDecimal.ZERO)));

        assertFalse(assembler.assemble(PARENT, CHILD, "Amina").isEmpty());
    }
}