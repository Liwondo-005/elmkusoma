package tz.elmkusoma.parent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.PaymentReceipt;
import tz.elmkusoma.parent.repository.PaymentReceiptRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock private PaymentReceiptRepository paymentReceiptRepository;
    @InjectMocks private ReceiptService receiptService;

    private static final UUID PAYMENT_ID = UUID.randomUUID();

    private Payment completedPayment() {
        return Payment.builder()
                .id(PAYMENT_ID)
                .parentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .serviceType("ONLINE_COURSE")
                .amount(new BigDecimal("2000"))
                .currency("TZS")
                .status("COMPLETED")
                .providerReference("gw-ref-1")
                .paidAt(LocalDateTime.now())
                .policyKey("SERVICE_DEFAULT_PRICE")
                .policyVersion(1)
                .build();
    }

    @Test
    void issueFor_issuesAReceiptForACompletedPayment() {
        Payment payment = completedPayment();
        when(paymentReceiptRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());
        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(i -> {
            PaymentReceipt r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        PaymentReceipt receipt = receiptService.issueFor(payment, "Launch offer");

        assertTrue(receipt.getReceiptNumber().startsWith("RCPT-"));
        assertEquals(new BigDecimal("2000"), receipt.getAmount());
        assertEquals("gw-ref-1", receipt.getProviderReference());
        assertEquals(0, BigDecimal.ZERO.compareTo(receipt.getRefundedAmount()));
        // Stamped back so a lookup in either direction is one query.
        assertEquals(receipt.getReceiptNumber(), payment.getReceiptNumber());
    }

    @Test
    void issueFor_isIdempotent_soADuplicateCallbackCannotDuplicateDocuments() {
        Payment payment = completedPayment();
        PaymentReceipt existing = PaymentReceipt.builder()
                .receiptNumber("RCPT-2026-000001")
                .paymentId(PAYMENT_ID)
                .build();
        when(paymentReceiptRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.of(existing));

        PaymentReceipt receipt = receiptService.issueFor(payment, null);

        assertEquals("RCPT-2026-000001", receipt.getReceiptNumber());
        verify(paymentReceiptRepository, never()).save(any());
    }

    @Test
    void issueFor_refusesToDocumentMoneyThatWasNeverReceived() {
        Payment pending = completedPayment();
        pending.setStatus("PENDING");

        // A receipt is proof of payment. Issuing one for an unpaid payment would document
        // money that never arrived.
        assertThrows(IllegalStateException.class, () -> receiptService.issueFor(pending, null));
        verify(paymentReceiptRepository, never()).save(any());
    }

    @Test
    void issueFor_requiresAPaymentId() {
        assertThrows(IllegalArgumentException.class,
                () -> receiptService.issueFor(Payment.builder().build(), null));
    }

    @Test
    void recordCreditNote_annotatesTheReceiptWithoutRewritingIt() {
        Payment payment = completedPayment();
        PaymentReceipt receipt = PaymentReceipt.builder()
                .id(UUID.randomUUID())
                .paymentId(PAYMENT_ID)
                .receiptNumber("RCPT-2026-000001")
                .amount(new BigDecimal("2000"))
                .build();
        when(paymentReceiptRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.of(receipt));
        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(i -> i.getArgument(0));

        Optional<PaymentReceipt> credited = receiptService.recordCreditNote(
                payment, new BigDecimal("500"), "Partial refund");

        assertTrue(credited.isPresent());
        // The original amount and number survive; only the refund figures are added.
        assertEquals("RCPT-2026-000001", credited.get().getReceiptNumber());
        assertEquals(new BigDecimal("2000"), credited.get().getAmount());
        assertEquals(new BigDecimal("500"), credited.get().getRefundedAmount());
        assertNotNull(credited.get().getCreditNoteNumber());
        assertEquals("Partial refund", credited.get().getCreditNoteReason());
    }

    @Test
    void recordCreditNote_isANoopWhenNoReceiptExists() {
        Payment payment = completedPayment();
        when(paymentReceiptRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());

        assertTrue(receiptService.recordCreditNote(payment, BigDecimal.ONE, "x").isEmpty());
        verify(paymentReceiptRepository, never()).save(any());
    }

    @Test
    void findByPayment_returnsEmptyWhenNoneIssued() {
        when(paymentReceiptRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());

        assertTrue(receiptService.findByPayment(PAYMENT_ID).isEmpty());
    }
}
