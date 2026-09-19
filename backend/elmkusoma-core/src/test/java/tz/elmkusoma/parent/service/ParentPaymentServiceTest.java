package tz.elmkusoma.parent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.dto.ParentPaymentResponse;
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
class ParentPaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private EntitlementRepository entitlementRepository;

    @InjectMocks
    private ParentPaymentService paymentService;

    private static final UUID PARENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();

    private Payment buildPayment(String status, BigDecimal amount) {
        Payment p = new Payment();
        p.setId(PAYMENT_ID);
        p.setParentId(PARENT_ID);
        p.setStudentId(STUDENT_ID);
        p.setInstitutionId(INSTITUTION_ID);
        p.setAmount(amount);
        p.setCurrency("TZS");
        p.setStatus(status);
        p.setServiceType("TUITION");
        p.setCreatedAt(LocalDateTime.now().minusDays(5));
        return p;
    }

    @Test
    void getPayments_returnsCorrectSummary() {
        Payment completed = buildPayment("COMPLETED", new BigDecimal("100000"));
        Payment pending = buildPayment("PENDING", new BigDecimal("50000"));
        when(paymentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(PARENT_ID))
                .thenReturn(List.of(completed, pending));

        ParentPaymentResponse result = paymentService.getPayments(PARENT_ID);

        assertEquals(new BigDecimal("100000"), result.getPaidThisTerm());
        assertEquals(new BigDecimal("50000"), result.getOutstanding());
        assertEquals(1, result.getRecentPayments().size());
        assertEquals(1, result.getPendingPayments().size());
    }

    @Test
    void getPaymentsByStudent_returnsPayments() {
        Payment p = buildPayment("COMPLETED", new BigDecimal("75000"));
        when(paymentRepository.findByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(STUDENT_ID))
                .thenReturn(List.of(p));

        var result = paymentService.getPaymentsByStudent(STUDENT_ID);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("75000"), result.get(0).getAmount());
    }

    @Test
    void initiatePayment_createsNewPayment() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment pay = invocation.getArgument(0);
            pay.setId(UUID.randomUUID());
            return pay;
        });

        Payment result = paymentService.initiatePayment(PARENT_ID, STUDENT_ID, INSTITUTION_ID,
                new BigDecimal("100000"), "TUITION", null, "Term 1 fees");

        assertNotNull(result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals("TZS", result.getCurrency());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void verifyPayment_marksCompleteAndGrantsEntitlement() {
        Payment pending = buildPayment("PENDING", new BigDecimal("100000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                STUDENT_ID, "TUITION", null)).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.verifyPayment(PAYMENT_ID, "ref-123");

        assertEquals("COMPLETED", result.getStatus());
        assertEquals("ref-123", result.getProviderReference());
        assertNotNull(result.getPaidAt());
        verify(entitlementRepository).save(any(Entitlement.class));
    }

    @Test
    void verifyPayment_idempotent_whenAlreadyCompleted() {
        Payment completed = buildPayment("COMPLETED", new BigDecimal("100000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completed));

        Payment result = paymentService.verifyPayment(PAYMENT_ID, "ref-456");

        assertEquals("COMPLETED", result.getStatus());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void hasEntitlement_returnsTrue_whenActive() {
        when(entitlementRepository.existsByStudentIdAndServiceTypeAndServiceIdAndStatusAndIsDeletedFalse(
                STUDENT_ID, "TRANSPORT", null, "ACTIVE")).thenReturn(true);
        assertTrue(paymentService.hasEntitlement(STUDENT_ID, "TRANSPORT", null));
    }

    @Test
    void hasEntitlement_returnsFalse_whenNotActive() {
        when(entitlementRepository.existsByStudentIdAndServiceTypeAndServiceIdAndStatusAndIsDeletedFalse(
                STUDENT_ID, "TRANSPORT", null, "ACTIVE")).thenReturn(false);
        assertFalse(paymentService.hasEntitlement(STUDENT_ID, "TRANSPORT", null));
    }
}
