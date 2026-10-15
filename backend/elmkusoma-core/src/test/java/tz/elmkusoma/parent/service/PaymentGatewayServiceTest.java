package tz.elmkusoma.parent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.administration.service.PlatformIntegrationService;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.payments.gateway.PaymentGatewayException;
import tz.elmkusoma.payments.gateway.StakabaGatewayClient;
import tz.elmkusoma.payments.gateway.StakabaProperties;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orchestration contract tests: a collection response is only ever PENDING,
 * verification completes exactly once against the stored record, ambiguous
 * outcomes never retry the non-idempotent call, and cross-parent access is
 * refused before any gateway call happens.
 */
@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID PARENT_USER_ID = UUID.randomUUID();
    private static final UUID PARENT_PROFILE_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @Mock
    private StakabaGatewayClient gatewayClient;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ParentRepository parentRepository;
    @Mock
    private ParentPaymentService paymentService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private PlatformIntegrationService integrationService;

    private PaymentGatewayService service;
    private StakabaProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StakabaProperties();
        properties.setEnabled(true);
        properties.setApiKey("sk_test_unit");
        service = new PaymentGatewayService(properties, gatewayClient, paymentRepository,
                parentRepository, paymentService, auditLogRepository, integrationService,
                new ObjectMapper());
    }

    private Payment pendingPayment() {
        Payment p = new Payment();
        p.setId(PAYMENT_ID);
        p.setParentId(PARENT_USER_ID);
        p.setStudentId(UUID.randomUUID());
        p.setInstitutionId(UUID.randomUUID());
        p.setAmount(new BigDecimal("5000.00"));
        p.setCurrency("TZS");
        p.setServiceType("MONTHLY_ACCESS");
        p.setStatus("PENDING");
        return p;
    }

    private void callerOwnsPayment() {
        Parent parent = new Parent();
        parent.setId(PARENT_PROFILE_ID);
        parent.setUserId(PARENT_USER_ID);
        when(parentRepository.findByUserIdAndIsDeletedFalse(PARENT_USER_ID))
                .thenReturn(Optional.of(parent));
    }

    // ── Collection initiation ──────────────────────────────────────────

    @Test
    void initiate_storesReference_andStaysPending() {
        Payment payment = pendingPayment();
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gatewayClient.initiateCollection(eq(new BigDecimal("5000.00")), eq("TZS"),
                eq("255742000331"), eq("Mpesa"), any()))
                .thenReturn(new StakabaGatewayClient.CollectionInitiated("M_ABC123"));

        Payment result = service.initiateCollection(payment, "0742000331", "Mpesa");

        assertEquals("stakaba", result.getProvider());
        assertEquals("M_ABC123", result.getProviderReference());
        // A queued USSD prompt is NOT payment: the record must stay PENDING.
        assertEquals("PENDING", result.getStatus());
        // Nothing may complete a payment at initiation time.
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
        assertTrue(result.getMetadata().contains("\"mobileNumber\":\"255742000331\""));
    }

    @Test
    void initiate_isIdempotentWhenReferenceAlreadyStored() {
        Payment payment = pendingPayment();
        payment.setProviderReference("M_EXISTING");

        Payment result = service.initiateCollection(payment, "0742000331", "Mpesa");

        assertEquals("M_EXISTING", result.getProviderReference());
        verify(gatewayClient, never()).initiateCollection(any(), any(), any(), any(), any());
    }

    @Test
    void initiate_rejectsDecimalAmountsBeforeTheCall() {
        Payment payment = pendingPayment();
        payment.setAmount(new BigDecimal("4999.50"));

        assertThrows(IllegalStateException.class,
                () -> service.initiateCollection(payment, "0742000331", "Mpesa"));
        verify(gatewayClient, never()).initiateCollection(any(), any(), any(), any(), any());
    }

    @Test
    void initiate_rejectsUnsupportedNetworkAndInvalidPhone() {
        Payment payment = pendingPayment();
        assertThrows(IllegalArgumentException.class,
                () -> service.initiateCollection(payment, "0742000331", "OrangeMoney"));
        assertThrows(IllegalArgumentException.class,
                () -> service.initiateCollection(payment, "12345", "Mpesa"));
        verify(gatewayClient, never()).initiateCollection(any(), any(), any(), any(), any());
    }

    @Test
    void initiate_rejectsNonTzsPayment() {
        Payment payment = pendingPayment();
        payment.setCurrency("USD");
        assertThrows(IllegalStateException.class,
                () -> service.initiateCollection(payment, "0742000331", "Mpesa"));
        verify(gatewayClient, never()).initiateCollection(any(), any(), any(), any(), any());
    }

    @Test
    void initiate_timeoutFlagsAmbiguous_andRethrowsWithoutCompleting() {
        Payment payment = pendingPayment();
        when(gatewayClient.initiateCollection(any(), any(), any(), any(), any()))
                .thenThrow(new PaymentGatewayException(
                        PaymentGatewayException.Code.TIMEOUT_AMBIGUOUS, "timeout"));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(PaymentGatewayException.class,
                () -> service.initiateCollection(payment, "0742000331", "Mpesa"));

        assertEquals("PENDING", payment.getStatus());
        assertEquals(null, payment.getProviderReference());
        assertTrue(payment.getMetadata().contains("initiationAmbiguous"),
                "ambiguous attempts must be flagged for reviewers: " + payment.getMetadata());
        // The non-idempotent collection call must NOT have been retried.
        verify(gatewayClient).initiateCollection(any(), any(), any(), any(), any());
    }

    @Test
    void initiate_whenGatewayDisabled_refuses() {
        properties.setEnabled(false);
        assertThrows(IllegalStateException.class,
                () -> service.initiateCollection(pendingPayment(), "0742000331", "Mpesa"));
        verify(gatewayClient, never()).initiateCollection(any(), any(), any(), any(), any());
    }

    // ── Verification ───────────────────────────────────────────────────

    @Test
    void verify_matchingSuccess_completesExactlyOnceViaExistingPipeline() {
        Payment payment = pendingPayment();
        payment.setProvider("stakaba");
        payment.setProviderReference("M_ABC123");
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(gatewayClient.getTransaction("M_ABC123")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("M_ABC123", "SUCCESS",
                        new BigDecimal("5000.00"), "TZS", "MPESA1", Map.of()));
        when(paymentRepository.existsByProviderReferenceAndIdNotAndIsDeletedFalse(
                "M_ABC123", PAYMENT_ID)).thenReturn(false);
        Payment completed = pendingPayment();
        completed.setStatus("COMPLETED");
        when(paymentService.verifyPayment(PAYMENT_ID, "M_ABC123", PARENT_USER_ID, "stakaba"))
                .thenReturn(completed);
        callerOwnsPayment();

        Payment result = service.verifyPaymentWithGateway(PAYMENT_ID, PARENT_USER_ID);

        assertEquals("COMPLETED", result.getStatus());
        verify(paymentService).verifyPayment(PAYMENT_ID, "M_ABC123", PARENT_USER_ID, "stakaba");
    }

    @Test
    void verify_alreadyCompleted_isIdempotent_noSecondGatewayCall() {
        Payment payment = pendingPayment();
        payment.setStatus("COMPLETED");
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        callerOwnsPayment();

        Payment result = service.verifyPaymentWithGateway(PAYMENT_ID, PARENT_USER_ID);

        assertEquals("COMPLETED", result.getStatus());
        verify(gatewayClient, never()).getTransaction(anyString());
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
    }

    @Test
    void verify_amountMismatch_refusesFulfilment() {
        Payment payment = pendingPayment();
        payment.setProvider("stakaba");
        payment.setProviderReference("M_ABC123");
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(gatewayClient.getTransaction("M_ABC123")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("M_ABC123", "SUCCESS",
                        new BigDecimal("100.00"), "TZS", "MPESA1", Map.of()));
        callerOwnsPayment();

        assertThrows(IllegalStateException.class,
                () -> service.verifyPaymentWithGateway(PAYMENT_ID, PARENT_USER_ID));

        assertEquals("PENDING", payment.getStatus());
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
        verify(integrationService).recordWebhook(eq("PAYMENT"), anyString(), anyString(),
                eq("FAILED"), anyString());
    }

    @Test
    void verify_gatewaySaysFailed_marksPaymentFailed() {
        Payment payment = pendingPayment();
        payment.setProvider("stakaba");
        payment.setProviderReference("M_ABC123");
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(gatewayClient.getTransaction("M_ABC123")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("M_ABC123", "FAILED",
                        null, null, null, Map.of()));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        callerOwnsPayment();

        Payment result = service.verifyPaymentWithGateway(PAYMENT_ID, PARENT_USER_ID);

        assertEquals("FAILED", result.getStatus());
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
    }

    @Test
    void verify_stillPending_leavesPaymentUntouched() {
        Payment payment = pendingPayment();
        payment.setProvider("stakaba");
        payment.setProviderReference("M_ABC123");
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(gatewayClient.getTransaction("M_ABC123")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("M_ABC123", "PENDING",
                        null, null, null, Map.of()));
        callerOwnsPayment();

        Payment result = service.verifyPaymentWithGateway(PAYMENT_ID, PARENT_USER_ID);

        assertEquals("PENDING", result.getStatus());
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
    }

    @Test
    void verify_unknownGatewayReference_keepsPendingForLater() {
        Payment payment = pendingPayment();
        payment.setProvider("stakaba");
        payment.setProviderReference("M_UNKNOWN");
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(gatewayClient.getTransaction("M_UNKNOWN")).thenThrow(new PaymentGatewayException(
                PaymentGatewayException.Code.NOT_FOUND, "not found"));
        callerOwnsPayment();

        Payment result = service.verifyPaymentWithGateway(PAYMENT_ID, PARENT_USER_ID);

        assertEquals("PENDING", result.getStatus());
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
    }

    @Test
    void verify_withoutGatewayReference_refuses() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        callerOwnsPayment();

        assertThrows(IllegalStateException.class,
                () -> service.verifyPaymentWithGateway(PAYMENT_ID, PARENT_USER_ID));
        verify(gatewayClient, never()).getTransaction(anyString());
    }

    @Test
    void verify_crossParentAccess_isRefusedBeforeAnyGatewayCall() {
        Payment payment = pendingPayment();
        payment.setProvider("stakaba");
        payment.setProviderReference("M_ABC123");
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        Parent stranger = new Parent();
        stranger.setId(UUID.randomUUID());
        stranger.setUserId(OTHER_USER_ID);
        when(parentRepository.findByUserIdAndIsDeletedFalse(OTHER_USER_ID))
                .thenReturn(Optional.of(stranger));

        assertThrows(ForbiddenException.class,
                () -> service.verifyPaymentWithGateway(PAYMENT_ID, OTHER_USER_ID));
        verify(gatewayClient, never()).getTransaction(anyString());
    }

    // ── Webhook events ─────────────────────────────────────────────────

    @Test
    void webhook_unknownReference_isAcknowledgedAndIgnored() {
        when(paymentRepository.findByProviderReferenceAndIsDeletedFalse("M_NOPE"))
                .thenReturn(Optional.empty());

        PaymentGatewayService.WebhookOutcome outcome = service.handleStakabaEvent(
                "transaction.success", "M_NOPE", new BigDecimal("5000"), "TZS");

        assertFalse(outcome.processed());
        assertFalse(outcome.fulfilled());
        verify(gatewayClient, never()).getTransaction(anyString());
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
    }

    @Test
    void webhook_successEvent_reverifiesViaApiThenCompletesOnce() {
        Payment payment = pendingPayment();
        payment.setProvider("stakaba");
        payment.setProviderReference("M_ABC123");
        when(paymentRepository.findByProviderReferenceAndIsDeletedFalse("M_ABC123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(gatewayClient.getTransaction("M_ABC123")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("M_ABC123", "SUCCESS",
                        new BigDecimal("5000.00"), "TZS", "MPESA1", Map.of()));
        when(paymentRepository.existsByProviderReferenceAndIdNotAndIsDeletedFalse(
                "M_ABC123", PAYMENT_ID)).thenReturn(false);
        Payment completed = pendingPayment();
        completed.setStatus("COMPLETED");
        when(paymentService.verifyPayment(PAYMENT_ID, "M_ABC123", null, "stakaba"))
                .thenReturn(completed);

        PaymentGatewayService.WebhookOutcome outcome = service.handleStakabaEvent(
                "transaction.success", "M_ABC123", new BigDecimal("5000"), "TZS");

        assertTrue(outcome.processed());
        assertTrue(outcome.fulfilled());
        assertEquals("COMPLETED", outcome.status());
        // Fulfilment happened only through the verified pipeline, once.
        verify(paymentService).verifyPayment(PAYMENT_ID, "M_ABC123", null, "stakaba");
    }

    @Test
    void webhook_forgedSuccess_apiSaysFailed_neverFulfils() {
        Payment payment = pendingPayment();
        payment.setProvider("stakaba");
        payment.setProviderReference("M_ABC123");
        when(paymentRepository.findByProviderReferenceAndIsDeletedFalse("M_ABC123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(gatewayClient.getTransaction("M_ABC123")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("M_ABC123", "FAILED",
                        null, null, null, Map.of()));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentGatewayService.WebhookOutcome outcome = service.handleStakabaEvent(
                "transaction.success", "M_ABC123", new BigDecimal("5000"), "TZS");

        assertTrue(outcome.processed());
        assertFalse(outcome.fulfilled());
        assertEquals("FAILED", outcome.status());
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
    }

    @Test
    void webhook_duplicateDelivery_afterCompletion_isHarmless() {
        Payment payment = pendingPayment();
        payment.setStatus("COMPLETED");
        payment.setProvider("stakaba");
        payment.setProviderReference("M_ABC123");
        when(paymentRepository.findByProviderReferenceAndIsDeletedFalse("M_ABC123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));

        PaymentGatewayService.WebhookOutcome outcome = service.handleStakabaEvent(
                "transaction.success", "M_ABC123", new BigDecimal("5000"), "TZS");

        assertTrue(outcome.fulfilled());
        // No second verification, no second entitlement.
        verify(gatewayClient, never()).getTransaction(anyString());
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
    }

    @Test
    void webhook_verificationInfrastructureFailure_propagatesForRetry() {
        Payment payment = pendingPayment();
        payment.setProvider("stakaba");
        payment.setProviderReference("M_ABC123");
        when(paymentRepository.findByProviderReferenceAndIsDeletedFalse("M_ABC123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(gatewayClient.getTransaction("M_ABC123")).thenThrow(new PaymentGatewayException(
                PaymentGatewayException.Code.NETWORK, "unreachable"));

        assertThrows(PaymentGatewayException.class, () -> service.handleStakabaEvent(
                "transaction.success", "M_ABC123", new BigDecimal("5000"), "TZS"));
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
    }

    @Test
    void webhook_amountMismatch_isAcknowledgedButNeverFulfilled() {
        Payment payment = pendingPayment();
        payment.setProvider("stakaba");
        payment.setProviderReference("M_ABC123");
        when(paymentRepository.findByProviderReferenceAndIsDeletedFalse("M_ABC123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(gatewayClient.getTransaction("M_ABC123")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("M_ABC123", "SUCCESS",
                        new BigDecimal("1.00"), "TZS", "MPESA1", Map.of()));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentGatewayService.WebhookOutcome outcome = service.handleStakabaEvent(
                "transaction.success", "M_ABC123", new BigDecimal("1"), "TZS");

        assertTrue(outcome.processed());
        assertFalse(outcome.fulfilled());
        verify(paymentService, never()).verifyPayment(any(), any(), any(), anyString());
    }

    @Test
    void initiateCollectionForOwnedPayment_enforcesOwnershipFirst() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        Parent stranger = new Parent();
        stranger.setId(UUID.randomUUID());
        stranger.setUserId(OTHER_USER_ID);
        when(parentRepository.findByUserIdAndIsDeletedFalse(OTHER_USER_ID))
                .thenReturn(Optional.of(stranger));

        assertThrows(ForbiddenException.class, () -> service.initiateCollectionForOwnedPayment(
                PAYMENT_ID, OTHER_USER_ID, "0742000331", "Mpesa"));
        verify(gatewayClient, never()).initiateCollection(any(), any(), any(), any(), any());
    }
}
