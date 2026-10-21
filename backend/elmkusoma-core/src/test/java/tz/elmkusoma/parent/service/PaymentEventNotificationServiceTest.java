package tz.elmkusoma.parent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.RefundRequest;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.subscription.domain.Subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The four payment lifecycle headlines that Payment.md section 61 asks for and that the
 * derived parent inbox cannot project.
 *
 * <p>Pure unit tests: no Spring context, no database. What they hold to is the contract
 * this service exists for - it addresses the payer and nobody else, it refuses to repeat a
 * headline it already emitted, it stays silent where a derived projection already speaks,
 * and it never lets an inbox failure reach the money event it describes.</p>
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventNotificationServiceTest {

    private static final UUID PARENT_ID = UUID.randomUUID();
    private static final UUID PAYER_USER_ID = UUID.randomUUID();

    @Mock
    private NotificationService notificationService;
    @Mock
    private LearnerNotificationRepository notificationRepository;
    @Mock
    private ParentRepository parentRepository;

    private PaymentEventNotificationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentEventNotificationService(
                notificationService, notificationRepository, parentRepository);
    }

    // ------------------------------------------------------------------
    // Recipient
    // ------------------------------------------------------------------

    @Test
    void aPaymentInitiatedHeadlineGoesToThePayerAndNobodyElse() {
        payerExists();
        notAlreadySent();
        Payment payment = pendingPayment();

        service.paymentInitiated(payment);

        verify(notificationService).notifyUser(
                eq(PAYER_USER_ID),
                eq("Payment initiated"),
                contains("awaiting confirmation"),
                eq(PaymentEventNotificationService.TYPE_PAYMENT),
                eq(PaymentEventNotificationService.TARGET_PAYMENT),
                eq(payment.getId()));
    }

    @Test
    void aHeadlineIsNotWrittenWhenThePayerRowCannotBeResolved() {
        when(parentRepository.findById(PARENT_ID)).thenReturn(Optional.empty());

        service.paymentInitiated(pendingPayment());

        verify(notificationService, never())
                .notifyUser(any(), any(), any(), any(), any(), any());
        // The duplicate check is not even worth asking: there is no recipient to ask about.
        verify(notificationRepository, never()).existsByUserIdAndTargetIdAndNotificationTypeAndTitleAndIsDeletedFalse(
                any(), any(), any(), any());
    }

    @Test
    void aPaymentWithNoPayerAtAllIsNotBroadcast() {
        Payment payment = pendingPayment();
        payment.setParentId(null);

        service.paymentInitiated(payment);

        verify(parentRepository, never()).findById(any());
        verify(notificationService, never())
                .notifyUser(any(), any(), any(), any(), any(), any());
    }

    // ------------------------------------------------------------------
    // Duplicate safety
    // ------------------------------------------------------------------

    @Test
    void theSameHeadlineIsNeverToldTwice() {
        payerExists();
        when(notificationRepository
                .existsByUserIdAndTargetIdAndNotificationTypeAndTitleAndIsDeletedFalse(
                        eq(PAYER_USER_ID), any(UUID.class),
                        eq(PaymentEventNotificationService.TYPE_PAYMENT),
                        eq("Payment initiated"))).thenReturn(true);

        service.paymentInitiated(pendingPayment());

        verify(notificationService, never())
                .notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aReplayedWebhookStillReachesTheGuardRatherThanFiringTwice() {
        payerExists();
        when(notificationRepository
                .existsByUserIdAndTargetIdAndNotificationTypeAndTitleAndIsDeletedFalse(
                        any(UUID.class), any(UUID.class), anyString(), anyString()))
                .thenReturn(false);

        Payment payment = pendingPayment();
        service.paymentInitiated(payment);
        service.paymentInitiated(payment);
        service.paymentInitiated(payment);

        // The guard runs on every attempt; what stops the second and third write is the row
        // the first one leaves behind, which the repository answers for from then on.
        verify(notificationRepository, org.mockito.Mockito.times(3))
                .existsByUserIdAndTargetIdAndNotificationTypeAndTitleAndIsDeletedFalse(
                        eq(PAYER_USER_ID), eq(payment.getId()),
                        eq(PaymentEventNotificationService.TYPE_PAYMENT),
                        eq("Payment initiated"));
    }

    // ------------------------------------------------------------------
    // Subscription periods
    // ------------------------------------------------------------------

    @Test
    void anOpeningPeriodIsReportedAsItsOwnEvent() {
        payerExists();
        notAlreadySent();
        Subscription subscription = subscription(Subscription.STATUS_ACTIVE);

        service.subscriptionActivated(subscription);

        verify(notificationService).notifyUser(
                eq(PAYER_USER_ID),
                eq("Subscription activated"),
                contains("PREMIUM_ACCESS"),
                eq(PaymentEventNotificationService.TYPE_SUBSCRIPTION),
                eq(PaymentEventNotificationService.TARGET_SUBSCRIPTION),
                eq(subscription.getId()));
    }

    @Test
    void aClosingPeriodIsReportedToo() {
        payerExists();
        notAlreadySent();
        Subscription subscription = subscription(Subscription.STATUS_EXPIRED);

        service.subscriptionExpired(subscription);

        verify(notificationService).notifyUser(
                eq(PAYER_USER_ID),
                eq("Subscription expired"),
                anyString(),
                eq(PaymentEventNotificationService.TYPE_SUBSCRIPTION),
                eq(PaymentEventNotificationService.TARGET_SUBSCRIPTION),
                eq(subscription.getId()));
    }

    // ------------------------------------------------------------------
    // Refund status changes, and the one state deliberately left silent
    // ------------------------------------------------------------------

    @Test
    void eachIntermediateRefundStateIsReportedWithItsOwnHeadline() {
        payerExists();
        notAlreadySent();

        service.refundStatusChanged(refund(RefundRequest.STATUS_APPROVED), completedPayment());

        verify(notificationService).notifyUser(
                eq(PAYER_USER_ID),
                eq("Refund Approved"),
                contains("APPROVED"),
                eq(PaymentEventNotificationService.TYPE_REFUND),
                eq(PaymentEventNotificationService.TARGET_REFUND),
                any(UUID.class));
    }

    @Test
    void aCompletedRefundOfAFullRefundSaysNothingBecauseTheProjectionAlreadyDid() {
        Payment fullyRefunded = completedPayment();
        fullyRefunded.setAmount(new BigDecimal("5000"));
        fullyRefunded.setRefundedAmount(new BigDecimal("5000"));

        service.refundStatusChanged(refund(RefundRequest.STATUS_COMPLETED), fullyRefunded);

        verify(notificationService, never())
                .notifyUser(any(), any(), any(), any(), any(), any());
        verify(notificationRepository, never()).existsByUserIdAndTargetIdAndNotificationTypeAndTitleAndIsDeletedFalse(
                any(), any(), any(), any());
        // Decided before the payer is even looked up: nothing about this event is worth a query.
        verify(parentRepository, never()).findById(any());
    }

    @Test
    void aCompletedPartialRefundIsReportedHereBecauseTheProjectionStaysSilent() {
        payerExists();
        notAlreadySent();
        Payment partiallyRefunded = completedPayment();
        partiallyRefunded.setAmount(new BigDecimal("5000"));
        partiallyRefunded.setRefundedAmount(new BigDecimal("1000"));

        service.refundStatusChanged(refund(RefundRequest.STATUS_COMPLETED), partiallyRefunded);

        verify(notificationService).notifyUser(
                eq(PAYER_USER_ID),
                eq("Refund Completed"),
                contains("COMPLETED"),
                eq(PaymentEventNotificationService.TYPE_REFUND),
                eq(PaymentEventNotificationService.TARGET_REFUND),
                any(UUID.class));
    }

    // ------------------------------------------------------------------
    // A failure here must never reach the money event it describes
    // ------------------------------------------------------------------

    @Test
    void anInboxFailureNeverBecomesAFailureInTheCaller() {
        payerExists();
        notAlreadySent();
        org.mockito.Mockito.doThrow(new IllegalStateException("inbox unavailable"))
                .when(notificationService)
                .notifyUser(any(), any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> service.paymentInitiated(pendingPayment()));
    }

    @Test
    void aRefundHeadlineStillNamesTheStatusItReports() {
        payerExists();
        notAlreadySent();

        service.refundStatusChanged(refund(RefundRequest.STATUS_UNDER_REVIEW), completedPayment());

        ArgumentCaptor<String> headline = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUser(
                eq(PAYER_USER_ID), headline.capture(), anyString(), anyString(), anyString(), any(UUID.class));
        assertEquals("Refund Under Review", headline.getValue());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private void payerExists() {
        when(parentRepository.findById(PARENT_ID))
                .thenReturn(Optional.of(Parent.builder().userId(PAYER_USER_ID).build()));
    }

    private void notAlreadySent() {
        when(notificationRepository
                .existsByUserIdAndTargetIdAndNotificationTypeAndTitleAndIsDeletedFalse(
                        any(UUID.class), any(UUID.class), anyString(), anyString()))
                .thenReturn(false);
    }

    private Payment pendingPayment() {
        Payment payment = Payment.builder()
                .parentId(PARENT_ID)
                .institutionId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .amount(new BigDecimal("3500"))
                .currency("TZS")
                .serviceType("TUITION")
                .status("PENDING")
                .build();
        payment.setId(UUID.randomUUID());
        return payment;
    }

    private Payment completedPayment() {
        Payment payment = pendingPayment();
        payment.setStatus("COMPLETED");
        payment.setRefundedAmount(BigDecimal.ZERO);
        return payment;
    }

    private Subscription subscription(String status) {
        Subscription subscription = Subscription.builder()
                .payerId(PARENT_ID)
                .serviceType("PREMIUM_ACCESS")
                .status(status)
                .periodStart(LocalDateTime.of(2026, 1, 1, 0, 0))
                .periodEnd(LocalDateTime.of(2026, 2, 1, 0, 0))
                .build();
        subscription.setId(UUID.randomUUID());
        return subscription;
    }

    private RefundRequest refund(String status) {
        RefundRequest request = RefundRequest.builder()
                .paymentId(UUID.randomUUID())
                .requestedBy(UUID.randomUUID())
                .amount(new BigDecimal("3500"))
                .reason("Changed plans")
                .status(status)
                .build();
        request.setId(UUID.randomUUID());
        return request;
    }
}
