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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private EntitlementRepository entitlementRepository;
    @Mock private ReconciliationExceptionRepository exceptionRepository;
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
                .build();
    }

    private void allowSave() {
        when(exceptionRepository.save(any(ReconciliationException.class)))
                .thenAnswer(i -> i.getArgument(0));
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
