package tz.elmkusoma.finance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.finance.domain.ReconciliationException;
import tz.elmkusoma.finance.repository.ReconciliationExceptionRepository;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.payments.gateway.PaymentGatewayException;
import tz.elmkusoma.payments.gateway.StakabaGatewayClient;
import tz.elmkusoma.payments.gateway.StakabaProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private EntitlementRepository entitlementRepository;
    @Mock private ReconciliationExceptionRepository exceptionRepository;
    @Mock private StakabaGatewayClient gatewayClient;
    @Mock private StakabaProperties gatewayProperties;
    @InjectMocks private ReconciliationService reconciliationService;

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    private Payment payment(String status, LocalDateTime paidAt, BigDecimal amount) {
        return Payment.builder()
                .id(PAYMENT_ID)
                .studentId(STUDENT_ID)
                .serviceType("TUITION")
                .status(status)
                .paidAt(paidAt)
                .updatedAt(paidAt)
                .amount(amount)
                .grossAmount(amount)
                .discountAmount(BigDecimal.ZERO)
                .netAmount(amount)
                .currency("TZS")
                // A real confirmed payment carries the allocation stamped at verification.
                // Leaving it null would make every fixture look like a broken one.
                .commissionStatus("RESOLVED")
                .commissionAmount(amount.movePointLeft(1))
                .providerShareAmount(amount.subtract(amount.movePointLeft(1)))
                .commissionPolicyKey("TUITION_STANDARD")
                .commissionPolicyVersion(1)
                .build();
    }

    private Payment completedPayment(String providerReference) {
        Payment payment = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        payment.setProvider("stakaba");
        payment.setProviderReference(providerReference);
        return payment;
    }

    private void allowSave() {
        when(exceptionRepository.save(any(ReconciliationException.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    private void stubGatewayScan(Payment... payments) {
        // The gateway sweep only consults a server that is actually configured for it.
        when(gatewayProperties.isEnabled()).thenReturn(true);
        when(gatewayProperties.hasApiKey()).thenReturn(true);
        when(paymentRepository.findByStatusAndIsDeletedFalse(eq("COMPLETED"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(payments)));
    }

    /**
     * Wires a fixture for the local scan. A hand-built payment has no entitlement row, so that
     * finding is marked already-open; everything else returns false by default, which leaves the
     * test asserting on exactly the discrepancy it is about.
     */
    private void stubLocalScan(Payment... payments) {
        when(paymentRepository.findAll()).thenReturn(List.of(payments));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                eq(ReconciliationException.TYPE_MISSING_ENTITLEMENT), any(), any())).thenReturn(true);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);
        allowSave();
    }

    // ------------------------------------------------------------------
    // The controlled failure: money taken, nothing granted
    // ------------------------------------------------------------------

    @Test
    void scan_flagsACompletedPaymentThatGrantedNoEntitlement() {
        Payment completed = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        when(paymentRepository.findAll()).thenReturn(List.of(completed));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);
        allowSave();

        var result = reconciliationService.scan();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        // The discrepancy is made visible. It is never auto-granted and never hidden.
        assertEquals(ReconciliationException.TYPE_MISSING_ENTITLEMENT, raised.getExceptionType());
        assertEquals(ReconciliationException.STATUS_OPEN, raised.getStatus());
        assertEquals(PAYMENT_ID, raised.getPaymentId());
        assertTrue(raised.getDescription().contains("no entitlement"));
    }

    @Test
    void scan_isSatisfiedWhenTheEntitlementReferencesThePayment() {
        Payment completed = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        Entitlement entitlement = Entitlement.builder()
                .paymentId(PAYMENT_ID)
                .studentId(STUDENT_ID)
                .serviceType("TUITION")
                .status("ACTIVE")
                .build();

        when(paymentRepository.findAll()).thenReturn(List.of(completed));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                STUDENT_ID, "TUITION", null)).thenReturn(Optional.of(entitlement));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        var result = reconciliationService.scan();

        assertTrue(result.newlyRaised().isEmpty());
        verify(exceptionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Stale pending
    // ------------------------------------------------------------------

    @Test
    void scan_flagsAPaymentStuckForOverTwentyFourHours() {
        Payment stuck = payment("PENDING", LocalDateTime.now().minusHours(48),
                new BigDecimal("50000"));
        when(paymentRepository.findAll()).thenReturn(List.of(stuck));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);
        allowSave();

        var result = reconciliationService.scan();

        assertEquals(1, result.newlyRaised().size());
        assertEquals(ReconciliationException.TYPE_PENDING_UNRESOLVED,
                result.newlyRaised().get(0).getExceptionType());
    }

    @Test
    void scan_leavesAFreshPendingPaymentAlone() {
        // A checkout started a minute ago has not failed yet; flagging it would be noise.
        Payment fresh = payment("PENDING", LocalDateTime.now().minusMinutes(5),
                new BigDecimal("50000"));
        when(paymentRepository.findAll()).thenReturn(List.of(fresh));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        assertTrue(reconciliationService.scan().newlyRaised().isEmpty());
    }

    // ------------------------------------------------------------------
    // Duplicates and idempotence
    // ------------------------------------------------------------------

    @Test
    void scan_doesNotDuplicateAnExceptionThatIsAlreadyOpen() {
        Payment completed = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        when(paymentRepository.findAll()).thenReturn(List.of(completed));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(true);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);

        assertTrue(reconciliationService.scan().newlyRaised().isEmpty());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    void scan_flagsADuplicateProviderReference() {
        String reference = "gateway-ref-1";
        Payment first = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        Payment second = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        second.setId(UUID.randomUUID());
        first.setProviderReference(reference);
        second.setProviderReference(reference);

        // The missing-entitlement findings for these fixtures are already open, so the only new
        // finding is the shared gateway reference.
        when(paymentRepository.findAll()).thenReturn(List.of(first, second));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                eq(ReconciliationException.TYPE_MISSING_ENTITLEMENT), any(), any()))
                .thenReturn(true);
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                eq(ReconciliationException.TYPE_DUPLICATE_EXTERNAL_REF), any(), any()))
                .thenReturn(false);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);
        allowSave();

        var result = reconciliationService.scan();

        assertEquals(1, result.newlyRaised().size());
        assertEquals(ReconciliationException.TYPE_DUPLICATE_EXTERNAL_REF,
                result.newlyRaised().get(0).getExceptionType());
        assertEquals(reference, result.newlyRaised().get(0).getReference());
    }

    // ------------------------------------------------------------------
    // A receipt failure never rolls the payment back
    // ------------------------------------------------------------------

    @Test
    void raiseReceiptFailure_recordsItButLeavesThePaymentCompleted() {
        Payment completed = payment("COMPLETED", LocalDateTime.now(), new BigDecimal("50000"));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        allowSave();

        reconciliationService.raiseReceiptFailure(completed, new IllegalStateException("store down"));

        // The money is real, so the transaction stands; only the document is recorded as missing.
        assertEquals("COMPLETED", completed.getStatus());
        verify(exceptionRepository).save(any(ReconciliationException.class));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void raiseReceiptFailure_isIdempotentPerPayment() {
        Payment completed = payment("COMPLETED", LocalDateTime.now(), new BigDecimal("50000"));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(true);

        reconciliationService.raiseReceiptFailure(completed, new IllegalStateException("down"));

        verify(exceptionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Currency (OP-06): reported, never converted, no rate ever invented
    // ------------------------------------------------------------------

    @Test
    void scan_flagsARecordedCurrencyThatIsNotIssued() {
        Payment payment = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        payment.setCurrency("USD");
        stubLocalScan(payment);

        var result = reconciliationService.scan();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        assertEquals(ReconciliationException.TYPE_CURRENCY_MISMATCH, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("USD"));
        assertTrue(raised.getDescription().contains("no rate is invented"));
    }

    @Test
    void scan_flagsAMissingCurrencyRatherThanAssumingOne() {
        Payment payment = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        payment.setCurrency(null);
        stubLocalScan(payment);

        var result = reconciliationService.scan();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        assertEquals(ReconciliationException.TYPE_CURRENCY_MISMATCH, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("none is assumed"));
        // An unknown currency stays unknown in the record too - it is not quietly filled in.
        assertNull(raised.getCurrency());
    }

    // ------------------------------------------------------------------
    // Earnings allocation (OP-10): unresolved is expected, broken arithmetic is not
    // ------------------------------------------------------------------

    @Test
    void scan_flagsACompletedPaymentThatCarriesNoAllocationStatus() {
        Payment payment = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        payment.setCommissionStatus(null);
        payment.setCommissionAmount(null);
        payment.setProviderShareAmount(null);
        payment.setCommissionPolicyKey(null);
        payment.setCommissionPolicyVersion(null);
        stubLocalScan(payment);

        var result = reconciliationService.scan();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        assertEquals(ReconciliationException.TYPE_FEE_MISMATCH, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("never recorded"));
    }

    @Test
    void scan_leavesAnUnapprovedCommissionRateAlone() {
        // Payment.md section 2 - the commission rate is still an open owner decision, so
        // NO_ACTIVE_POLICY / RATE_NOT_APPROVED with no figures is the honest expected state.
        // Flagging it would raise an exception on every single sale until the business decides.
        Payment noPolicy = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        noPolicy.setCommissionStatus("NO_ACTIVE_POLICY");
        noPolicy.setCommissionAmount(null);
        noPolicy.setProviderShareAmount(null);
        noPolicy.setCommissionPolicyKey(null);
        noPolicy.setCommissionPolicyVersion(null);

        Payment noRate = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        noRate.setId(UUID.randomUUID());
        noRate.setCommissionStatus("RATE_NOT_APPROVED");
        noRate.setCommissionAmount(null);
        noRate.setProviderShareAmount(null);

        when(paymentRepository.findAll()).thenReturn(List.of(noPolicy, noRate));
        // Only the entitlement finding is considered already open; a fee finding would return
        // false by default and be saved, which is what this test must prove does not happen.
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                eq(ReconciliationException.TYPE_MISSING_ENTITLEMENT), any(), any())).thenReturn(true);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(2L);

        assertTrue(reconciliationService.scan().newlyRaised().isEmpty());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    void scan_flagsAllocationFiguresThatDoNotSumToTheNet() {
        Payment payment = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        payment.setCommissionAmount(new BigDecimal("1000"));
        payment.setProviderShareAmount(new BigDecimal("9000"));
        stubLocalScan(payment);

        var result = reconciliationService.scan();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        assertEquals(ReconciliationException.TYPE_FEE_MISMATCH, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("TZS 10000"));
        assertTrue(raised.getDescription().contains("net amount is TZS 50000"));
    }

    @Test
    void scan_flagsFiguresRecordedUnderAnUnresolvedAllocation() {
        Payment payment = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        payment.setCommissionStatus("RATE_EXCEEDS_AMOUNT");
        payment.setCommissionAmount(new BigDecimal("60000"));
        payment.setProviderShareAmount(null);
        stubLocalScan(payment);

        var result = reconciliationService.scan();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        assertEquals(ReconciliationException.TYPE_FEE_MISMATCH, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("must carry no figures"));
    }

    @Test
    void scan_flagsAResolvedAllocationWithNoPolicyTrace() {
        Payment payment = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        payment.setCommissionPolicyKey(null);
        payment.setCommissionPolicyVersion(null);
        stubLocalScan(payment);

        var result = reconciliationService.scan();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        assertEquals(ReconciliationException.TYPE_FEE_MISMATCH, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("cannot be traced"));
    }

    @Test
    void scan_flagsANegativeAllocationAmount() {
        Payment payment = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        payment.setCommissionAmount(new BigDecimal("-1"));
        payment.setProviderShareAmount(new BigDecimal("50001"));
        stubLocalScan(payment);

        var result = reconciliationService.scan();

        assertEquals(1, result.newlyRaised().size());
        assertTrue(result.newlyRaised().get(0).getDescription().contains("negative"));
    }

    @Test
    void scan_leavesAllocationAloneForPaymentsThatDidNotComplete() {
        // Allocation only happens at confirmation, so an in-flight payment legitimately has none.
        Payment fresh = payment("PENDING", LocalDateTime.now().minusMinutes(5),
                new BigDecimal("50000"));
        fresh.setCommissionStatus(null);
        fresh.setCommissionAmount(null);
        fresh.setProviderShareAmount(null);
        when(paymentRepository.findAll()).thenReturn(List.of(fresh));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        assertTrue(reconciliationService.scan().newlyRaised().isEmpty());
        verify(exceptionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Gateway re-verification (OP-03): uncertain is not the same as denied
    // ------------------------------------------------------------------

    @Test
    void scanGatewayOutcomes_doesNothingWhenTheGatewayIsNotConfigured() {
        // isEnabled()/hasApiKey() are false by default here. A gateway we were never given
        // credentials for cannot confirm or deny anything, so nothing is reported as uncertain.
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        var result = reconciliationService.scanGatewayOutcomes();

        assertTrue(result.newlyRaised().isEmpty());
        verifyNoInteractions(gatewayClient);
        verify(paymentRepository, never())
                .findByStatusAndIsDeletedFalse(any(), any());
    }

    @Test
    void scanGatewayOutcomes_leavesAGatewayConfirmedPaymentAlone() {
        Payment payment = completedPayment("ref-ok");
        stubGatewayScan(payment);
        when(gatewayClient.getTransaction("ref-ok")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("ref-ok", "SUCCESS",
                        new BigDecimal("50000"), "TZS", "external-1", null));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        var result = reconciliationService.scanGatewayOutcomes();

        assertTrue(result.newlyRaised().isEmpty());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    void scanGatewayOutcomes_consultsOnlyStakabaPaymentsWithAReference() {
        Payment manual = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        manual.setProvider("manual");
        manual.setProviderReference("ref-manual");

        Payment noReference = payment("COMPLETED", LocalDateTime.now().minusHours(2),
                new BigDecimal("50000"));
        noReference.setId(UUID.randomUUID());
        noReference.setProvider("stakaba");
        noReference.setProviderReference("   ");

        stubGatewayScan(manual, noReference);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        assertTrue(reconciliationService.scanGatewayOutcomes().newlyRaised().isEmpty());
        verify(gatewayClient, never()).getTransaction(any());
    }

    @Test
    void scanGatewayOutcomes_flagsALocalCompletionTheGatewayDenies() {
        Payment payment = completedPayment("ref-failed");
        stubGatewayScan(payment);
        when(gatewayClient.getTransaction("ref-failed")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("ref-failed", "FAILED",
                        new BigDecimal("50000"), "TZS", "external-2", null));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);
        allowSave();

        var result = reconciliationService.scanGatewayOutcomes();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        assertEquals(ReconciliationException.TYPE_INCONSISTENT_STATUS, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("gateway reports FAILED"));
        // The payment itself is untouched - this method only records the disagreement.
        assertEquals("COMPLETED", payment.getStatus());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(entitlementRepository);
    }

    @Test
    void scanGatewayOutcomes_treatsAMissingGatewayRecordAsInconsistent() {
        Payment payment = completedPayment("ref-unknown");
        stubGatewayScan(payment);
        when(gatewayClient.getTransaction("ref-unknown")).thenThrow(new PaymentGatewayException(
                PaymentGatewayException.Code.NOT_FOUND, "Transaction not found"));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);
        allowSave();

        var result = reconciliationService.scanGatewayOutcomes();

        assertEquals(1, result.newlyRaised().size());
        assertEquals(ReconciliationException.TYPE_INCONSISTENT_STATUS,
                result.newlyRaised().get(0).getExceptionType());
        assertTrue(result.newlyRaised().get(0).getDescription().contains("no transaction"));
    }

    @Test
    void scanGatewayOutcomes_recordsUncertaintyWhenTheGatewayIsUnreachable() {
        Payment payment = completedPayment("ref-down");
        stubGatewayScan(payment);
        when(gatewayClient.getTransaction("ref-down")).thenThrow(new PaymentGatewayException(
                PaymentGatewayException.Code.NETWORK, "Could not reach the payment provider."));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);
        allowSave();

        var result = reconciliationService.scanGatewayOutcomes();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        // "We could not ask" must never be recorded as "they said no".
        assertEquals(ReconciliationException.TYPE_GATEWAY_UNCERTAIN, raised.getExceptionType());
        assertNotEquals(ReconciliationException.TYPE_INCONSISTENT_STATUS, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("unconfirmed"));
        assertTrue(raised.getDescription().contains("NETWORK"));
    }

    @Test
    void scanGatewayOutcomes_recordsUncertaintyWhenTheLookupBlowsUpUnexpectedly() {
        Payment payment = completedPayment("ref-broken");
        stubGatewayScan(payment);
        when(gatewayClient.getTransaction("ref-broken"))
                .thenThrow(new IllegalStateException("unexpected parse failure"));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);
        allowSave();

        var result = reconciliationService.scanGatewayOutcomes();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        assertEquals(ReconciliationException.TYPE_GATEWAY_UNCERTAIN, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("IllegalStateException"));
        assertTrue(raised.getDescription().contains("nothing was changed"));
    }

    @Test
    void scanGatewayOutcomes_flagsAnAmountTheGatewayDisputes() {
        Payment payment = completedPayment("ref-amount");
        stubGatewayScan(payment);
        when(gatewayClient.getTransaction("ref-amount")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("ref-amount", "SUCCESS",
                        new BigDecimal("40000"), "TZS", "external-3", null));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);
        allowSave();

        var result = reconciliationService.scanGatewayOutcomes();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        assertEquals(ReconciliationException.TYPE_AMOUNT_MISMATCH, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("TZS 40000"));
        // The stored figure stays authoritative; the disagreement is reported, not applied.
        assertEquals(0, new BigDecimal("50000").compareTo(raised.getAmount()));
        assertEquals(0, new BigDecimal("50000").compareTo(payment.getAmount()));
    }

    @Test
    void scanGatewayOutcomes_flagsACurrencyTheGatewayDoesNotIssue() {
        Payment payment = completedPayment("ref-currency");
        stubGatewayScan(payment);
        when(gatewayClient.getTransaction("ref-currency")).thenReturn(
                new StakabaGatewayClient.GatewayTransaction("ref-currency", "SUCCESS",
                        new BigDecimal("50000"), "USD", "external-4", null));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(false);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);
        allowSave();

        var result = reconciliationService.scanGatewayOutcomes();

        assertEquals(1, result.newlyRaised().size());
        ReconciliationException raised = result.newlyRaised().get(0);
        assertEquals(ReconciliationException.TYPE_CURRENCY_MISMATCH, raised.getExceptionType());
        assertTrue(raised.getDescription().contains("USD"));
        assertTrue(raised.getDescription().contains("no rate is invented"));
        assertEquals("TZS", payment.getCurrency());
    }

    @Test
    void scanGatewayOutcomes_doesNotDuplicateAnAlreadyOpenException() {
        Payment payment = completedPayment("ref-open");
        stubGatewayScan(payment);
        when(gatewayClient.getTransaction("ref-open")).thenThrow(new PaymentGatewayException(
                PaymentGatewayException.Code.PROVIDER, "The payment provider is unavailable."));
        when(exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                any(), any(), any())).thenReturn(true);
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(1L);

        assertTrue(reconciliationService.scanGatewayOutcomes().newlyRaised().isEmpty());
        verify(exceptionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Human resolution
    // ------------------------------------------------------------------

    @Test
    void resolve_closesAnExceptionWithANote_andRecordsWho() {
        UUID adminId = UUID.randomUUID();
        ReconciliationException exception = ReconciliationException.builder()
                .id(UUID.randomUUID())
                .status(ReconciliationException.STATUS_OPEN)
                .build();
        when(exceptionRepository.findById(exception.getId())).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(any(ReconciliationException.class)))
                .thenAnswer(i -> i.getArgument(0));

        ReconciliationException resolved = reconciliationService.resolve(
                exception.getId(), adminId, "Receipt reissued manually");

        assertEquals(ReconciliationException.STATUS_RESOLVED, resolved.getStatus());
        assertEquals(adminId, resolved.getResolvedBy());
        assertNotNull(resolved.getResolvedAt());
        assertEquals("Receipt reissued manually", resolved.getResolutionNote());
    }

    @Test
    void assign_movesAnExceptionToInProgressUnderAnOwner() {
        UUID analystId = UUID.randomUUID();
        ReconciliationException exception = ReconciliationException.builder()
                .id(UUID.randomUUID())
                .status(ReconciliationException.STATUS_OPEN)
                .build();
        when(exceptionRepository.findById(exception.getId())).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(any(ReconciliationException.class)))
                .thenAnswer(i -> i.getArgument(0));

        ReconciliationException assigned = reconciliationService.assign(exception.getId(), analystId);

        assertEquals(ReconciliationException.STATUS_IN_PROGRESS, assigned.getStatus());
        assertEquals(analystId, assigned.getAssignedTo());
    }

    @Test
    void openExceptions_listsOnlyOpenOnes() {
        when(exceptionRepository.findByStatusAndIsDeletedFalseOrderByDiscoveredAtDesc(
                ReconciliationException.STATUS_OPEN)).thenReturn(List.of(
                ReconciliationException.builder().status("OPEN").build()));

        assertEquals(1, reconciliationService.openExceptions().size());
    }
}
