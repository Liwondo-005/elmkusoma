package tz.elmkusoma.finance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.finance.domain.ReconciliationException;
import tz.elmkusoma.finance.repository.ReconciliationExceptionRepository;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detects financial inconsistencies and records them for a human to resolve.
 *
 * <p>This service is read-and-record only. It never edits a payment, never rewrites an amount
 * and never closes an exception on its own - a discrepancy that has been silently "corrected"
 * leaves no trace that it ever existed, which is precisely how a real mismatch disappears. Every
 * finding becomes a {@link ReconciliationException} with a type and a reference, and stays open
 * until somebody decides what to do with it.</p>
 *
 * <p>Detection is idempotent: one open exception per (type, reference), backed by a partial
 * unique index, so re-running a scan does not produce a wall of duplicates.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final PaymentRepository paymentRepository;
    private final EntitlementRepository entitlementRepository;
    private final ReconciliationExceptionRepository exceptionRepository;

    /** Payments stuck this long are worth a human's attention. */
    public static final int PENDING_THRESHOLD_HOURS = 24;

    public record ScanResult(int openExceptions, List<ReconciliationException> newlyRaised) {
    }

    @Transactional
    public ScanResult scan() {
        List<ReconciliationException> raised = new ArrayList<>();

        raised.addAll(scanStalePending());
        raised.addAll(scanMissingEntitlements());
        raised.addAll(scanAmountInconsistencies());
        raised.addAll(scanRefundInconsistencies());
        raised.addAll(scanDuplicateProviderReferences());

        long open = exceptionRepository.countByStatusAndIsDeletedFalse(ReconciliationException.STATUS_OPEN);
        log.info("Reconciliation scan raised {} new exception(s); {} now open",
                raised.size(), open);
        return new ScanResult((int) open, raised);
    }

    /** A payment that never completed and never failed has simply been left hanging. */
    private List<ReconciliationException> scanStalePending() {
        List<ReconciliationException> raised = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(PENDING_THRESHOLD_HOURS);
        for (Payment payment : paymentRepository.findAll()) {
            if (isDeleted(payment)) {
                continue;
            }
            boolean stuckStatus = "PENDING".equals(payment.getStatus())
                    || "PROCESSING".equals(payment.getStatus());
            if (!stuckStatus) {
                continue;
            }
            LocalDateTime reference = payment.getPaidAt() != null ? payment.getPaidAt() : payment.getUpdatedAt();
            if (reference == null || reference.isAfter(cutoff)) {
                continue;
            }
            raise(raised, ReconciliationException.TYPE_PENDING_UNRESOLVED,
                    reference(payment), payment, payment.getAmount(),
                    "Payment has been " + payment.getStatus() + " since " + reference
                            + " and never reached a terminal state");
        }
        return raised;
    }

    /**
     * A confirmed payment with no entitlement is the controlled failure Payment.md calls out:
     * money was taken and nothing was granted. It must be visible, never auto-granted and never
     * hidden.
     */
    private List<ReconciliationException> scanMissingEntitlements() {
        List<ReconciliationException> raised = new ArrayList<>();
        for (Payment payment : paymentRepository.findAll()) {
            if (isDeleted(payment) || !"COMPLETED".equals(payment.getStatus())) {
                continue;
            }
            boolean entitled = entitlementRepository
                    .findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                            payment.getStudentId(), payment.getServiceType(), payment.getServiceId())
                    .filter(e -> payment.getId().equals(e.getPaymentId()))
                    .isPresent();
            if (!entitled) {
                raise(raised, ReconciliationException.TYPE_MISSING_ENTITLEMENT,
                        reference(payment), payment, payment.getAmount(),
                        "Payment is COMPLETED but no entitlement references it");
            }
        }
        return raised;
    }

    /**
     * The charged amount must agree with the terms snapshotted at initiation. A disagreement
     * means something changed the record after the fact, which is never legitimate.
     */
    private List<ReconciliationException> scanAmountInconsistencies() {
        List<ReconciliationException> raised = new ArrayList<>();
        for (Payment payment : paymentRepository.findAll()) {
            if (isDeleted(payment)) {
                continue;
            }
            if (payment.getNetAmount() != null && payment.getAmount() != null
                    && payment.getNetAmount().compareTo(payment.getAmount()) != 0) {
                raise(raised, ReconciliationException.TYPE_AMOUNT_MISMATCH,
                        reference(payment), payment, payment.getAmount(),
                        "Stored amount TZS " + payment.getAmount()
                                + " disagrees with the snapshotted net amount TZS " + payment.getNetAmount());
            }
            if (payment.getGrossAmount() != null && payment.getDiscountAmount() != null
                    && payment.getNetAmount() != null) {
                BigDecimal expected = payment.getGrossAmount().subtract(payment.getDiscountAmount());
                if (expected.compareTo(payment.getNetAmount()) != 0) {
                    raise(raised, ReconciliationException.TYPE_AMOUNT_MISMATCH,
                            reference(payment), payment, payment.getNetAmount(),
                            "gross TZS " + payment.getGrossAmount() + " minus discount TZS "
                                    + payment.getDiscountAmount() + " = TZS " + expected
                                    + " but the payment records net TZS " + payment.getNetAmount());
                }
            }
        }
        return raised;
    }

    /** More refunded than charged, or marked refunded with nothing refunded, are both wrong. */
    private List<ReconciliationException> scanRefundInconsistencies() {
        List<ReconciliationException> raised = new ArrayList<>();
        for (Payment payment : paymentRepository.findAll()) {
            if (isDeleted(payment)) {
                continue;
            }
            BigDecimal refunded = payment.getRefundedAmount() == null
                    ? BigDecimal.ZERO : payment.getRefundedAmount();
            if (payment.getAmount() != null && refunded.compareTo(payment.getAmount()) > 0) {
                raise(raised, ReconciliationException.TYPE_REFUND_MISMATCH,
                        reference(payment), payment, refunded,
                        "Refunded TZS " + refunded + " exceeds the payment of TZS " + payment.getAmount());
            }
            if ("REFUNDED".equals(payment.getStatus()) && refunded.signum() == 0) {
                raise(raised, ReconciliationException.TYPE_REFUND_MISMATCH,
                        reference(payment), payment, BigDecimal.ZERO,
                        "Payment is marked REFUNDED but records no refunded amount");
            }
        }
        return raised;
    }

    /** Two payments carrying the same gateway reference means the callback was matched twice. */
    private List<ReconciliationException> scanDuplicateProviderReferences() {
        List<ReconciliationException> raised = new ArrayList<>();
        Map<String, Long> counts = paymentRepository.findAll().stream()
                .filter(p -> !isDeleted(p))
                .filter(p -> p.getProviderReference() != null && !p.getProviderReference().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        Payment::getProviderReference, java.util.stream.Collectors.counting()));

        counts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .forEach(e -> {
                    Payment exemplar = paymentRepository.findAll().stream()
                            .filter(p -> !isDeleted(p))
                            .filter(p -> e.getKey().equals(p.getProviderReference()))
                            .findFirst().orElse(null);
                    raise(raised, ReconciliationException.TYPE_DUPLICATE_EXTERNAL_REF,
                            e.getKey(), exemplar, exemplar == null ? null : exemplar.getAmount(),
                            e.getValue() + " payments share provider reference " + e.getKey());
                });
        return raised;
    }

    private void raise(List<ReconciliationException> raised, String type, String reference,
                       Payment payment, BigDecimal amount, String description) {
        if (exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                type, reference, ReconciliationException.STATUS_OPEN)) {
            return; // already open; re-running a scan must not duplicate it
        }
        ReconciliationException exception = ReconciliationException.builder()
                .exceptionType(type)
                .reference(reference)
                .paymentId(payment == null ? null : payment.getId())
                .institutionId(payment == null ? null : payment.getInstitutionId())
                .amount(amount)
                .currency(payment == null ? "TZS" : payment.getCurrency())
                .description(description)
                .status(ReconciliationException.STATUS_OPEN)
                .discoveredAt(LocalDateTime.now())
                .build();
        raised.add(exceptionRepository.save(exception));
        log.warn("Reconciliation exception {} for {}: {}", type, reference, description);
    }

    /**
     * Records that a receipt could not be issued for a payment that <b>did</b> complete.
     *
     * <p>Critical distinction: the payment is fine and stays COMPLETED. Only the document is
     * missing. Rolling the payment back because a receipt failed would erase a real confirmed
     * transaction, which is precisely the failure mode Payment.md forbids - so the loss is
     * recorded here as an exception for a human to reissue, and the money stays accounted for.</p>
     */
    @Transactional
    public void raiseReceiptFailure(Payment payment, Exception cause) {
        String reference = reference(payment);
        if (exceptionRepository.existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
                ReconciliationException.TYPE_MISSING_INTERNAL_RECORD, reference,
                ReconciliationException.STATUS_OPEN)) {
            return;
        }
        exceptionRepository.save(ReconciliationException.builder()
                .exceptionType(ReconciliationException.TYPE_MISSING_INTERNAL_RECORD)
                .reference(reference)
                .paymentId(payment.getId())
                .institutionId(payment.getInstitutionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .description("Payment is COMPLETED but its receipt could not be issued: "
                        + cause.getClass().getSimpleName() + " - " + cause.getMessage()
                        + ". The payment stands; reissue the receipt.")
                .status(ReconciliationException.STATUS_OPEN)
                .discoveredAt(LocalDateTime.now())
                .build());
        log.error("Receipt failure recorded for completed payment {}", payment.getId(), cause);
    }

    @Transactional
    public ReconciliationException resolve(UUID exceptionId, UUID resolvedBy, String note) {
        ReconciliationException exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException(
                        "Reconciliation exception not found"));
        exception.setStatus(ReconciliationException.STATUS_RESOLVED);
        exception.setResolvedBy(resolvedBy);
        exception.setResolvedAt(LocalDateTime.now());
        exception.setResolutionNote(note);
        return exceptionRepository.save(exception);
    }

    @Transactional
    public ReconciliationException assign(UUID exceptionId, UUID owner) {
        ReconciliationException exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException(
                        "Reconciliation exception not found"));
        exception.setAssignedTo(owner);
        exception.setStatus(ReconciliationException.STATUS_IN_PROGRESS);
        return exceptionRepository.save(exception);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationException> openExceptions() {
        return exceptionRepository.findByStatusAndIsDeletedFalseOrderByDiscoveredAtDesc(
                ReconciliationException.STATUS_OPEN);
    }

    private static boolean isDeleted(Payment payment) {
        return Boolean.TRUE.equals(payment.getIsDeleted());
    }

    private static String reference(Payment payment) {
        if (payment.getId() != null) {
            return payment.getId().toString();
        }
        return payment.getProviderReference() != null
                ? payment.getProviderReference() : "unknown";
    }
}
