package tz.elmkusoma.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.RefundRequest;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.parent.repository.RefundRequestRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The governed refund lifecycle.
 *
 * <p>A refund request is not a refund. This service keeps the ten steps of the lifecycle
 * distinct - request, eligibility, review, decision, tracking, financial adjustment, entitlement
 * adjustment, documentation, notification, audit - precisely so that no intermediate state can be
 * mistaken for money that has actually been returned.</p>
 *
 * <p>Two things are deliberate:</p>
 * <ul>
 *   <li><b>COMPLETED is the only state that moves money.</b> REQUESTED, UNDER_REVIEW, APPROVED and
 *       PROCESSING all mean "not yet", and the UI must say so. Because no live gateway is
 *       enabled, reaching COMPLETED is an authorized back-office action confirming that
 *       processing actually happened, not something a callback can do.</li>
 *   <li><b>No approval limit is invented.</b> Payment.md forbids inventing a refund time limit
 *       or threshold without business approval, so every approval names the human who made it
 *       and is audited. The threshold stays an open decision rather than a made-up number.</li>
 * </ul>
 *
 * <p>Duplicate protection is enforced in storage: a partial unique index allows only one open
 * refund per payment, so two concurrent requests cannot both proceed.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final PaymentRepository paymentRepository;
    private final EntitlementRepository entitlementRepository;
    private final AuditLogRepository auditLogRepository;
    private final ReceiptService receiptService;

    private static final List<String> OPEN_STATES = List.of(
            RefundRequest.STATUS_REQUESTED, RefundRequest.STATUS_UNDER_REVIEW,
            RefundRequest.STATUS_APPROVED, RefundRequest.STATUS_PROCESSING);

    public static class RefundException extends IllegalStateException {
        public RefundException(String message) {
            super(message);
        }
    }

    // ------------------------------------------------------------------
    // 1. Request
    // ------------------------------------------------------------------

    /**
     * Opens a refund request against a confirmed payment.
     *
     * <p>Eligibility is checked here rather than by the caller: the payment must be COMPLETED
     * (nothing to refund on money never received), the amount must not exceed what is still
     * refundable, and no other refund may already be open against the payment.</p>
     */
    @Transactional
    public RefundRequest requestRefund(UUID paymentId, UUID requestedBy, BigDecimal amount,
                                       String reason, String evidenceUrl) {
        if (amount == null || amount.signum() <= 0) {
            throw new RefundException("A refund amount must be greater than zero");
        }
        if (reason == null || reason.isBlank()) {
            throw new RefundException("A refund reason is required");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!"COMPLETED".equals(payment.getStatus())
                && !"REFUNDED".equals(payment.getStatus())
                && !"PARTIALLY_REFUNDED".equals(payment.getStatus())) {
            throw new RefundException(
                    "Only a completed payment can be refunded; this payment is " + payment.getStatus());
        }

        BigDecimal refundable = payment.refundableAmount();
        if (amount.compareTo(refundable) > 0) {
            throw new RefundException(
                    "Refund TZS " + amount + " exceeds the refundable balance of TZS " + refundable);
        }

        if (refundRequestRepository.existsByPaymentIdAndStatusInAndIsDeletedFalse(paymentId, OPEN_STATES)) {
            throw new RefundException("A refund is already open for payment " + paymentId);
        }

        RefundRequest request = RefundRequest.builder()
                .paymentId(paymentId)
                .requestedBy(requestedBy)
                .amount(amount)
                .reason(reason.trim())
                .evidenceUrl(evidenceUrl)
                .status(RefundRequest.STATUS_REQUESTED)
                .entitlementAction(RefundRequest.ENTITLEMENT_PENDING)
                .build();
        request = refundRequestRepository.save(request);

        audit(requestedBy, payment, "Refund requested",
                Map.of("refundId", request.getId().toString(),
                        "amount", amount.toPlainString(), "reason", reason.trim()),
                Map.of("status", RefundRequest.STATUS_REQUESTED));

        log.info("Refund {} requested for payment {} by {} amount TZS {}",
                request.getId(), paymentId, requestedBy, amount);
        return request;
    }

    // ------------------------------------------------------------------
    // 2-4. Review and decision
    // ------------------------------------------------------------------

    @Transactional
    public RefundRequest submitForReview(UUID refundId, UUID actor) {
        return transition(refundId, RefundRequest.STATUS_REQUESTED, RefundRequest.STATUS_UNDER_REVIEW,
                actor, "Refund moved to review");
    }

    /**
     * Approves a refund and records what should happen to the entitlement it bought.
     *
     * <p>Approval is a decision, not a completion: no money has moved. The entitlement outcome
     * is chosen here and applied when the refund actually completes, so a partial refund can
     * legitimately retain access while a full one revokes it.</p>
     */
    @Transactional
    public RefundRequest approve(UUID refundId, UUID decidedBy, String note, boolean revokeEntitlement) {
        RefundRequest request = require(refundId);
        if (!RefundRequest.STATUS_REQUESTED.equals(request.getStatus())
                && !RefundRequest.STATUS_UNDER_REVIEW.equals(request.getStatus())) {
            throw new RefundException(
                    "Only a requested or under-review refund can be approved, but it is " + request.getStatus());
        }

        request.setStatus(RefundRequest.STATUS_APPROVED);
        request.setDecidedBy(decidedBy);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecisionNote(note);
        request.setEntitlementAction(revokeEntitlement
                ? RefundRequest.ENTITLEMENT_REVOKE : RefundRequest.ENTITLEMENT_RETAIN);
        request = refundRequestRepository.save(request);

        audit(decidedBy, paymentOf(request), "Refund approved",
                Map.of("refundId", request.getId().toString(),
                        "amount", request.getAmount().toPlainString(),
                        "entitlementAction", request.getEntitlementAction()),
                Map.of("status", RefundRequest.STATUS_APPROVED,
                        "decisionNote", note == null ? "" : note));

        log.info("Refund {} approved by {} (entitlement {})", refundId, decidedBy,
                request.getEntitlementAction());
        return request;
    }

    @Transactional
    public RefundRequest reject(UUID refundId, UUID decidedBy, String note) {
        RefundRequest request = require(refundId);
        if (!RefundRequest.STATUS_REQUESTED.equals(request.getStatus())
                && !RefundRequest.STATUS_UNDER_REVIEW.equals(request.getStatus())) {
            throw new RefundException(
                    "Only a requested or under-review refund can be rejected, but it is " + request.getStatus());
        }

        request.setStatus(RefundRequest.STATUS_REJECTED);
        request.setDecidedBy(decidedBy);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecisionNote(note);
        request = refundRequestRepository.save(request);

        audit(decidedBy, paymentOf(request), "Refund rejected",
                Map.of("refundId", request.getId().toString(),
                        "amount", request.getAmount().toPlainString()),
                Map.of("status", RefundRequest.STATUS_REJECTED,
                        "decisionNote", note == null ? "" : note));

        log.info("Refund {} rejected by {}", refundId, decidedBy);
        return request;
    }

    // ------------------------------------------------------------------
    // 5-8. Processing, completion, financial + entitlement adjustment, documentation
    // ------------------------------------------------------------------

    /** Records that processing has been dispatched to the gateway. Money has still not moved. */
    @Transactional
    public RefundRequest markProcessing(UUID refundId, UUID actor, String refundReference) {
        RefundRequest request = require(refundId);
        if (!RefundRequest.STATUS_APPROVED.equals(request.getStatus())) {
            throw new RefundException(
                    "Only an approved refund can be sent for processing, but it is " + request.getStatus());
        }
        request.setStatus(RefundRequest.STATUS_PROCESSING);
        request.setRefundReference(refundReference);
        request = refundRequestRepository.save(request);
        audit(actor, paymentOf(request), "Refund sent for processing",
                Map.of("refundId", request.getId().toString()),
                Map.of("status", RefundRequest.STATUS_PROCESSING));
        return request;
    }

    /**
     * Completes a refund: applies the financial adjustment, the entitlement decision and the
     * credit note.
     *
     * <p>This is the only transition that represents money actually having been returned. It
     * requires an authorized actor confirming real processing, because with no live gateway
     * nothing else can know that a transfer occurred.</p>
     */
    @Transactional
    public RefundRequest completeRefund(UUID refundId, UUID actor, String refundReference) {
        RefundRequest request = require(refundId);
        if (!RefundRequest.STATUS_APPROVED.equals(request.getStatus())
                && !RefundRequest.STATUS_PROCESSING.equals(request.getStatus())) {
            throw new RefundException(
                    "Only an approved or processing refund can be completed, but it is " + request.getStatus());
        }

        // Read under a row lock, and re-check the balance here rather than trusting the check
        // made when the request was opened. Without this the check is a time-of-check
        // time-of-use gap: a request opened against a balance that another refund has since
        // exhausted would still complete, returning the same money twice. The lock is the same
        // one every refund path takes, so two completions cannot both observe the old balance.
        Payment payment = paymentRepository.findByIdForUpdate(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        BigDecimal refundable = payment.refundableAmount();
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new RefundException("A refund amount must be greater than zero");
        }
        if (request.getAmount().compareTo(refundable) > 0) {
            // Reached when another refund - including one taken through the legacy
            // single-step endpoint - has already consumed part or all of the balance since this
            // request was opened. Refused rather than applied, so a refund can never exceed
            // what was actually charged.
            throw new RefundException(
                    "Refund TZS " + request.getAmount() + " exceeds the refundable balance of TZS "
                            + refundable + " for payment " + request.getPaymentId()
                            + "; the balance is re-checked under lock at completion");
        }

        // 6. Financial adjustment. Accumulated, never overwritten, so partial refunds add up.
        BigDecimal alreadyRefunded = payment.getRefundedAmount() == null
                ? BigDecimal.ZERO : payment.getRefundedAmount();
        BigDecimal newRefunded = alreadyRefunded.add(request.getAmount());
        payment.setRefundedAmount(newRefunded);
        payment.setStatus(payment.refundableAmount().signum() <= 0
                ? "REFUNDED" : "PARTIALLY_REFUNDED");
        payment = paymentRepository.save(payment);

        // 7. Entitlement adjustment, only when the approver decided to revoke and only for the
        // entitlement this payment actually granted.
        if (RefundRequest.ENTITLEMENT_REVOKE.equals(request.getEntitlementAction())) {
            revokeEntitlementFor(payment);
        }

        // 8. Refund documentation - a credit note, never an edit to the original receipt.
        receiptService.recordCreditNote(payment, newRefunded, request.getReason());

        request.setStatus(RefundRequest.STATUS_COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        request.setRefundReference(refundReference != null ? refundReference : request.getRefundReference());
        request = refundRequestRepository.save(request);

        audit(actor, payment, "Refund completed",
                Map.of("refundId", request.getId().toString(),
                        "amount", request.getAmount().toPlainString(),
                        "totalRefunded", newRefunded.toPlainString()),
                Map.of("status", RefundRequest.STATUS_COMPLETED,
                        "paymentStatus", payment.getStatus()));

        log.info("Refund {} completed by {} - payment {} now {} of TZS {} refunded",
                refundId, actor, payment.getId(), newRefunded, payment.getAmount());
        return request;
    }

    @Transactional
    public RefundRequest cancel(UUID refundId, UUID actor) {
        RefundRequest request = require(refundId);
        if (request.isTerminal()) {
            throw new RefundException("Refund " + refundId + " is already " + request.getStatus());
        }
        request.setStatus(RefundRequest.STATUS_CANCELLED);
        request = refundRequestRepository.save(request);
        audit(actor, paymentOf(request), "Refund cancelled",
                Map.of("refundId", request.getId().toString()),
                Map.of("status", RefundRequest.STATUS_CANCELLED));
        return request;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<RefundRequest> find(UUID refundId) {
        return refundRequestRepository.findById(refundId).filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()));
    }

    @Transactional(readOnly = true)
    public List<RefundRequest> forPayment(UUID paymentId) {
        return refundRequestRepository.findByPaymentIdAndIsDeletedFalseOrderByCreatedAtDesc(paymentId);
    }

    @Transactional(readOnly = true)
    public List<RefundRequest> openRequests() {
        return refundRequestRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtAsc(
                RefundRequest.STATUS_REQUESTED);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void revokeEntitlementFor(Payment payment) {
        entitlementRepository
                .findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                        payment.getStudentId(), payment.getServiceType(), payment.getServiceId())
                .filter(e -> payment.getId().equals(e.getPaymentId()))
                .ifPresent(entitlement -> {
                    entitlement.setStatus("REVOKED");
                    entitlement.setExpiresAt(LocalDateTime.now());
                    entitlementRepository.save(entitlement);
                });
    }

    private RefundRequest require(UUID refundId) {
        return refundRequestRepository.findById(refundId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found"));
    }

    private Payment paymentOf(RefundRequest request) {
        return paymentRepository.findById(request.getPaymentId()).orElse(null);
    }

    private RefundRequest transition(UUID refundId, String expectedFrom, String to,
                                     UUID actor, String action) {
        RefundRequest request = require(refundId);
        if (!expectedFrom.equals(request.getStatus())) {
            throw new RefundException("Expected refund to be " + expectedFrom + " but it is "
                    + request.getStatus());
        }
        request.setStatus(to);
        request = refundRequestRepository.save(request);
        audit(actor, paymentOf(request), action,
                Map.of("refundId", request.getId().toString()),
                Map.of("status", to));
        return request;
    }

    private void audit(UUID actor, Payment payment, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        AuditLog auditLog = new AuditLog();
        auditLog.setInstitutionId(payment == null ? null : payment.getInstitutionId());
        auditLog.setUserId(actor);
        auditLog.setEntityType("RefundRequest");
        auditLog.setEntityId(payment == null ? null : payment.getId());
        auditLog.setEntityName("Refund");
        auditLog.setAction(AuditLog.AuditAction.UPDATE);
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);
        auditLogRepository.save(auditLog);
    }
}
