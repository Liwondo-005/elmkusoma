package tz.elmkusoma.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.PaymentReceipt;
import tz.elmkusoma.parent.repository.PaymentReceiptRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Issues the receipt for a confirmed payment, and the credit note for a refund against it.
 *
 * <p>Two rules the service exists to keep:</p>
 * <ul>
 *   <li>A receipt is issued only for a payment that actually reached COMPLETED. A pending
 *       payment has not been paid, and issuing a receipt for it would document money that never
 *       arrived.</li>
 *   <li>A refund never rewrites the original receipt. The document stays exactly as issued and
 *       the correction is recorded as a credit note reference and reason, so the historical
 *       record of what was charged survives intact while the reversal is visible beside it.</li>
 * </ul>
 *
 * <p>Receipt numbering is derived from the payment id rather than from a sequence counter. A
 * counter needs a lock on a shared row, which turns a receipt into a contention point on the
 * payment path; deriving it from the payment id is unique by construction and needs no lock.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final PaymentReceiptRepository paymentReceiptRepository;

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy", Locale.ROOT);

    /**
     * Issues the receipt for a newly completed payment.
     *
     * <p>Idempotent: a repeated callback for the same payment returns the receipt already
     * issued rather than creating a second one, so a duplicate gateway callback cannot produce
     * duplicate financial documents.</p>
     *
     * @return the receipt, newly issued or already present
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PaymentReceipt issueFor(Payment payment, String serviceName) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalArgumentException("A completed payment is required to issue a receipt");
        }
        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new IllegalStateException(
                    "A receipt may only be issued for a completed payment, but payment "
                            + payment.getId() + " is " + payment.getStatus());
        }

        Optional<PaymentReceipt> existing = paymentReceiptRepository
                .findByPaymentIdAndIsDeletedFalse(payment.getId());
        if (existing.isPresent()) {
            log.info("Receipt {} already issued for payment {}", existing.get().getReceiptNumber(),
                    payment.getId());
            return existing.get();
        }

        PaymentReceipt receipt = PaymentReceipt.builder()
                .paymentId(payment.getId())
                .receiptNumber(receiptNumber(payment))
                .parentId(payment.getParentId())
                .studentId(payment.getStudentId())
                .serviceType(payment.getServiceType())
                .serviceId(payment.getServiceId())
                .serviceName(serviceName)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentChannel(payment.getProvider())
                .providerReference(payment.getProviderReference())
                .paidAt(payment.getPaidAt())
                .policyKey(payment.getPolicyKey())
                .policyVersion(payment.getPolicyVersion())
                .refundedAmount(payment.getRefundedAmount() == null
                        ? BigDecimal.ZERO : payment.getRefundedAmount())
                .build();
        receipt = paymentReceiptRepository.save(receipt);

        // Stamped back onto the payment so a lookup in either direction is a single query.
        payment.setReceiptNumber(receipt.getReceiptNumber());

        log.info("Receipt {} issued for payment {}", receipt.getReceiptNumber(), payment.getId());
        return receipt;
    }

    /**
     * Records a credit note against an existing receipt.
     *
     * <p>The original receipt is not edited in place; only the refund figures and the credit
     * note reference are set. The amount, payer, service and price terms it was issued under
     * remain exactly as they were.</p>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<PaymentReceipt> recordCreditNote(Payment payment, BigDecimal refundAmount,
                                                     String reason) {
        if (payment == null) {
            return Optional.empty();
        }
        return paymentReceiptRepository.findByPaymentIdAndIsDeletedFalse(payment.getId())
                .map(receipt -> {
                    receipt.setRefundedAmount(refundAmount == null
                            ? BigDecimal.ZERO : refundAmount);
                    receipt.setCreditNoteNumber(creditNoteNumber(payment));
                    receipt.setCreditNoteReason(reason);
                    PaymentReceipt saved = paymentReceiptRepository.save(receipt);
                    log.info("Credit note {} recorded against receipt {} for payment {}",
                            saved.getCreditNoteNumber(), saved.getReceiptNumber(), payment.getId());
                    return saved;
                });
    }

    /** The receipt for a payment, if one has been issued. */
    @Transactional(readOnly = true)
    public Optional<PaymentReceipt> findByPayment(java.util.UUID paymentId) {
        return paymentReceiptRepository.findByPaymentIdAndIsDeletedFalse(paymentId);
    }

    static String receiptNumber(Payment payment) {
        return "RCPT-" + YEAR.format(LocalDateTime.now()) + "-"
                + payment.getId().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    static String creditNoteNumber(Payment payment) {
        return "CN-" + YEAR.format(LocalDateTime.now()) + "-"
                + payment.getId().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
