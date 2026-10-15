package tz.elmkusoma.parent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.RefundRequest;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.parent.repository.RefundRequestRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private EntitlementRepository entitlementRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ReceiptService receiptService;

    @InjectMocks private RefundService refundService;

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID PARENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID REFUND_ID = UUID.randomUUID();

    private Payment completedPayment(BigDecimal amount) {
        Payment payment = Payment.builder()
                .id(PAYMENT_ID)
                .parentId(PARENT_ID)
                .studentId(STUDENT_ID)
                .serviceType("TUITION")
                .amount(amount)
                .currency("TZS")
                .status("COMPLETED")
                .refundedAmount(BigDecimal.ZERO)
                .build();
        return payment;
    }

    private RefundRequest refundInStatus(String status) {
        return RefundRequest.builder()
                .id(REFUND_ID)
                .paymentId(PAYMENT_ID)
                .amount(new BigDecimal("10000"))
                .reason("Overcharge")
                .status(status)
                .build();
    }

    // ------------------------------------------------------------------
    // Requesting
    // ------------------------------------------------------------------

    @Test
    void requestRefund_opensARequestAgainstACompletedPayment() {
        Payment payment = completedPayment(new BigDecimal("50000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRequestRepository.existsByPaymentIdAndStatusInAndIsDeletedFalse(any(), any()))
                .thenReturn(false);
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> {
            RefundRequest r = i.getArgument(0);
            r.setId(REFUND_ID);
            return r;
        });

        RefundRequest request = refundService.requestRefund(
                PAYMENT_ID, PARENT_ID, new BigDecimal("10000"), "Duplicate charge", null);

        assertEquals(RefundRequest.STATUS_REQUESTED, request.getStatus());
        // Opening a request is not a refund. No money has moved and access is untouched.
        assertEquals(BigDecimal.ZERO, payment.getRefundedAmount());
        assertEquals("COMPLETED", payment.getStatus());
        verify(receiptService, never()).recordCreditNote(any(), any(), any());
        verify(auditLogRepository).save(any());
    }

    @Test
    void requestRefund_refusesToOpenASecondRefundOnTheSamePayment() {
        // A second reviewer must not be able to open a competing refund while one is in flight.
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(
                Optional.of(completedPayment(new BigDecimal("50000"))));
        when(refundRequestRepository.existsByPaymentIdAndStatusInAndIsDeletedFalse(any(), any()))
                .thenReturn(true);

        assertThrows(RefundService.RefundException.class, () ->
                refundService.requestRefund(PAYMENT_ID, PARENT_ID,
                        new BigDecimal("10000"), "Duplicate charge", null));
        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void requestRefund_refusesAnAmountAboveTheRefundableBalance() {
        Payment payment = completedPayment(new BigDecimal("50000"));
        payment.setRefundedAmount(new BigDecimal("45000")); // only 5000 left to refund
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        assertThrows(RefundService.RefundException.class, () ->
                refundService.requestRefund(PAYMENT_ID, PARENT_ID,
                        new BigDecimal("10000"), "Too much", null));
    }

    @Test
    void requestRefund_refusesToRefundMoneyThatWasNeverReceived() {
        Payment payment = completedPayment(new BigDecimal("50000"));
        payment.setStatus("PENDING");
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        assertThrows(RefundService.RefundException.class, () ->
                refundService.requestRefund(PAYMENT_ID, PARENT_ID,
                        new BigDecimal("10000"), "Too early", null));
    }

    @Test
    void requestRefund_rejectsANonPositiveAmountAndAMissingReason() {
        assertThrows(RefundService.RefundException.class, () ->
                refundService.requestRefund(PAYMENT_ID, PARENT_ID, BigDecimal.ZERO, "x", null));
        assertThrows(RefundService.RefundException.class, () ->
                refundService.requestRefund(PAYMENT_ID, PARENT_ID, new BigDecimal("100"), "  ", null));
    }

    // ------------------------------------------------------------------
    // Approval is a decision, not a completion
    // ------------------------------------------------------------------

    @Test
    void approve_recordsADecisionWithoutMovingAnyMoney() {
        RefundRequest request = refundInStatus(RefundRequest.STATUS_UNDER_REVIEW);
        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(
                Optional.of(completedPayment(new BigDecimal("50000"))));

        RefundRequest approved = refundService.approve(REFUND_ID, UUID.randomUUID(),
                "Legitimate duplicate", false);

        assertEquals(RefundRequest.STATUS_APPROVED, approved.getStatus());
        assertNotNull(approved.getDecidedAt());
        assertEquals(RefundRequest.ENTITLEMENT_RETAIN, approved.getEntitlementAction());
        // Approval alone never marks money as returned - only COMPLETED does that.
        verify(receiptService, never()).recordCreditNote(any(), any(), any());
    }

    @Test
    void approve_cannotApproveATerminalRefund() {
        RefundRequest request = refundInStatus(RefundRequest.STATUS_COMPLETED);
        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));

        assertThrows(RefundService.RefundException.class, () ->
                refundService.approve(REFUND_ID, UUID.randomUUID(), "late", true));
    }

    @Test
    void reject_recordsADecision() {
        RefundRequest request = refundInStatus(RefundRequest.STATUS_UNDER_REVIEW);
        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(
                Optional.of(completedPayment(new BigDecimal("50000"))));

        assertEquals(RefundRequest.STATUS_REJECTED,
                refundService.reject(REFUND_ID, UUID.randomUUID(), "Outside policy").getStatus());
    }

    // ------------------------------------------------------------------
    // Completion: the only transition that moves money
    // ------------------------------------------------------------------

    @Test
    void completeRefund_accumulatesTheRefundedAmountAndReachesREFUNDED() {
        RefundRequest request = refundInStatus(RefundRequest.STATUS_APPROVED);
        request.setAmount(new BigDecimal("50000"));
        request.setEntitlementAction(RefundRequest.ENTITLEMENT_REVOKE);
        Payment payment = completedPayment(new BigDecimal("50000"));

        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                STUDENT_ID, "TUITION", null)).thenReturn(Optional.empty());

        RefundRequest completed = refundService.completeRefund(REFUND_ID, UUID.randomUUID(), "gw-1");

        assertEquals(RefundRequest.STATUS_COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertEquals(0, new BigDecimal("50000").compareTo(payment.getRefundedAmount()));
        assertEquals("REFUNDED", payment.getStatus());
        // The financial record is amended, never rewritten: the receipt is annotated with a
        // credit note rather than edited.
        verify(receiptService).recordCreditNote(eq(payment), eq(new BigDecimal("50000")), any());
        verify(auditLogRepository).save(any());
    }

    @Test
    void completeRefund_partialRefundLeavesThePaymentPartiallyRefunded() {
        RefundRequest request = refundInStatus(RefundRequest.STATUS_APPROVED);
        request.setAmount(new BigDecimal("20000"));
        Payment payment = completedPayment(new BigDecimal("50000"));

        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        refundService.completeRefund(REFUND_ID, UUID.randomUUID(), null);

        assertEquals(0, new BigDecimal("20000").compareTo(payment.getRefundedAmount()));
        assertEquals("PARTIALLY_REFUNDED", payment.getStatus());
    }

    @Test
    void completeRefund_revokesTheEntitlementThisPaymentGranted() {
        RefundRequest request = refundInStatus(RefundRequest.STATUS_APPROVED);
        request.setAmount(new BigDecimal("50000"));
        request.setEntitlementAction(RefundRequest.ENTITLEMENT_REVOKE);
        Payment payment = completedPayment(new BigDecimal("50000"));

        Entitlement entitlement = Entitlement.builder()
                .id(UUID.randomUUID())
                .paymentId(PAYMENT_ID)
                .studentId(STUDENT_ID)
                .serviceType("TUITION")
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusDays(20))
                .build();

        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                STUDENT_ID, "TUITION", null)).thenReturn(Optional.of(entitlement));
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(i -> i.getArgument(0));

        refundService.completeRefund(REFUND_ID, UUID.randomUUID(), null);

        assertEquals("REVOKED", entitlement.getStatus());
        // Access ends now, not at the original expiry - the money was returned.
        assertTrue(entitlement.getExpiresAt().isBefore(LocalDateTime.now().plusSeconds(5)));
    }

    @Test
    void completeRefund_keepsEntitlementWhenTheApproverChoseToRetain() {
        RefundRequest request = refundInStatus(RefundRequest.STATUS_APPROVED);
        request.setAmount(new BigDecimal("20000"));
        request.setEntitlementAction(RefundRequest.ENTITLEMENT_RETAIN);
        Payment payment = completedPayment(new BigDecimal("50000"));

        Entitlement entitlement = Entitlement.builder()
                .id(UUID.randomUUID())
                .paymentId(PAYMENT_ID)
                .studentId(STUDENT_ID)
                .serviceType("TUITION")
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusDays(20))
                .build();

        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        refundService.completeRefund(REFUND_ID, UUID.randomUUID(), null);

        assertEquals("ACTIVE", entitlement.getStatus());
        // The entitlement is never even looked up, so it cannot be revoked by accident.
        verify(entitlementRepository, never()).save(any());
    }

    @Test
    void completeRefund_refusesARefundThatIsNotApprovedOrProcessing() {
        RefundRequest request = refundInStatus(RefundRequest.STATUS_REQUESTED);
        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));

        assertThrows(RefundService.RefundException.class, () ->
                refundService.completeRefund(REFUND_ID, UUID.randomUUID(), null));
    }

    // ------------------------------------------------------------------
    // Completion: the duplicate-refund defect this increment exists to close
    // ------------------------------------------------------------------

    @Test
    void completeRefund_readsThePaymentUnderARowLock() {
        // The balance check is only worth anything if no other refund can change the balance
        // between reading it and writing the new total. That requires the locked read.
        RefundRequest request = refundInStatus(RefundRequest.STATUS_APPROVED);
        Payment payment = completedPayment(new BigDecimal("50000"));

        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        refundService.completeRefund(REFUND_ID, UUID.randomUUID(), null);

        verify(paymentRepository).findByIdForUpdate(PAYMENT_ID);
        verify(paymentRepository, never()).findById(PAYMENT_ID);
    }

    @Test
    void completeRefund_refusesWhenTheBalanceWasAlreadyExhausted() {
        // The exact defect: a payment refunded through the legacy endpoint recorded no
        // refundedAmount, so it still looked fully refundable and this path would have handed
        // back the same 50,000 a second time. The completion re-check catches it.
        RefundRequest request = refundInStatus(RefundRequest.STATUS_APPROVED);
        request.setAmount(new BigDecimal("50000"));
        Payment payment = completedPayment(new BigDecimal("50000"));
        payment.setRefundedAmount(new BigDecimal("50000"));

        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));

        assertThrows(RefundService.RefundException.class, () ->
                refundService.completeRefund(REFUND_ID, UUID.randomUUID(), "gw-2"));

        // Nothing was written: no second financial adjustment, no second credit note.
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(receiptService, never()).recordCreditNote(any(), any(), any());
    }

    @Test
    void completeRefund_refusesWhenAPriorPartialRefundHasConsumedPartOfTheBalance() {
        // 30,000 of a 50,000 payment is already gone, so only 20,000 remains refundable.
        RefundRequest request = refundInStatus(RefundRequest.STATUS_APPROVED);
        request.setAmount(new BigDecimal("30000"));
        Payment payment = completedPayment(new BigDecimal("50000"));
        payment.setRefundedAmount(new BigDecimal("30000"));

        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));

        RefundService.RefundException ex = assertThrows(RefundService.RefundException.class, () ->
                refundService.completeRefund(REFUND_ID, UUID.randomUUID(), "gw-2"));

        assertTrue(ex.getMessage().contains("exceeds the refundable balance"));
        assertTrue(ex.getMessage().contains("20000"), "message should name the remaining balance");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void completeRefund_refusesANonPositiveAmount() {
        // A zero or negative amount would not move money but would mark the refund COMPLETED
        // and produce a credit note for nothing, which is worse than a clean refusal.
        RefundRequest request = refundInStatus(RefundRequest.STATUS_APPROVED);
        request.setAmount(BigDecimal.ZERO);
        Payment payment = completedPayment(new BigDecimal("50000"));

        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));

        assertThrows(RefundService.RefundException.class, () ->
                refundService.completeRefund(REFUND_ID, UUID.randomUUID(), null));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void cancel_stopsANonTerminalRefund() {
        RefundRequest request = refundInStatus(RefundRequest.STATUS_UNDER_REVIEW);
        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(
                Optional.of(completedPayment(new BigDecimal("50000"))));

        assertEquals(RefundRequest.STATUS_CANCELLED,
                refundService.cancel(REFUND_ID, UUID.randomUUID()).getStatus());
    }

    @Test
    void cancel_refusesToCancelATerminalRefund() {
        // A COMPLETED refund is history; cancelling it would erase what actually happened.
        RefundRequest request = refundInStatus(RefundRequest.STATUS_COMPLETED);
        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));

        assertThrows(RefundService.RefundException.class,
                () -> refundService.cancel(REFUND_ID, UUID.randomUUID()));
    }

    @Test
    void lifecycle_cannotSkipReviewStraightToProcessing() {
        RefundRequest request = refundInStatus(RefundRequest.STATUS_REQUESTED);
        when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(request));

        // markProcessing requires APPROVED; REQUESTED is not it.
        assertThrows(RefundService.RefundException.class, () ->
                refundService.markProcessing(REFUND_ID, UUID.randomUUID(), "gw-9"));
    }

    @Test
    void openRequests_onlyListsRequestedOnes() {
        when(refundRequestRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtAsc(
                RefundRequest.STATUS_REQUESTED)).thenReturn(List.of(refundInStatus("REQUESTED")));

        assertEquals(1, refundService.openRequests().size());
    }
}
