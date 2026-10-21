package tz.elmkusoma.finance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.finance.domain.ReconciliationException;
import tz.elmkusoma.finance.repository.ReconciliationExceptionRepository;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.payments.gateway.PaymentGatewayException;
import tz.elmkusoma.payments.gateway.StakabaGatewayClient;
import tz.elmkusoma.payments.gateway.StakabaProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final StakabaGatewayClient gatewayClient;
    private final StakabaProperties gatewayProperties;

    /** Payments stuck this long are worth a human's attention. */
    public static final int PENDING_THRESHOLD_HOURS = 24;

    /**
     * The single currency the platform issues and accepts. {@code Payment.md} forbids converting,
     * and forbids assuming a rate that was never approved - so anything else is surfaced as a
     * discrepancy instead of being translated.
     */
    public static final String SUPPORTED_CURRENCY = "TZS";

    /**
     * Upper bound on gateway lookups per sweep. Reconciliation must never turn into a load
     * problem or an unbounded fan-out against the provider, and the batch is taken most-recent
     * first so the newest money is re-verified before older money.
     */
    public static final int GATEWAY_SCAN_LIMIT = 25;

    public record ScanResult(int openExceptions, List<ReconciliationException> newlyRaised) {
    }

    @Transactional
    public ScanResult scan() {
        List<ReconciliationException> raised = new ArrayList<>();

        raised.addAll(scanStalePending());
        raised.addAll(scanMissingEntitlements());
        raised.addAll(scanAmountInconsistencies());
        raised.addAll(scanRefundInconsistencies());
        raised.addAll(scanCurrencyInconsistencies());
        raised.addAll(scanEarningsAllocations());
        raised.addAll(scanDuplicateProviderReferences());

        long open = exceptionRepository.countByStatusAndIsDeletedFalse(ReconciliationException.STATUS_OPEN);
        log.info("Reconciliation scan raised {} new exception(s); {} now open",
                raised.size(), open);
        return new ScanResult((int) open, raised);
    }

    /**
     * Re-verifies locally-completed payments against the gateway (OP-03, §70).
     *
     * <p>Kept out of {@link #scan()} on purpose: {@code scan()} is purely local and deterministic,
     * while this one performs bounded outbound HTTP. The sweep runs both back to back, but an
     * operator who wants the cheap integrity checks can still get them while the provider is
     * unreachable.</p>
     *
     * <p>Three outcomes, none of which ever edits the payment:</p>
     * <ul>
     *   <li>gateway confirms success - the record stands, nothing is raised;</li>
     *   <li>gateway answers {@code FAILED} or says the reference does not exist - a locally
     *       COMPLETED payment that the provider denies is {@code INCONSISTENT_STATUS};</li>
     *   <li>gateway cannot be reached - {@code GATEWAY_UNCERTAIN}, because "we could not ask"
     *       is a different fact from "they said no".</li>
     * </ul>
     *
     * <p>Amount and currency are compared on the same round trip, so a provider-side figure that
     * disagrees with the stored one is caught here rather than at the next webhook.</p>
     *
     * <p>No entitlement, receipt or notification is ever emitted from this method. Fulfilment
     * already happened (or did not) at verification time; re-deriving it from a read would risk
     * granting the same purchase twice.</p>
     */
    @Transactional
    public ScanResult scanGatewayOutcomes() {
        List<ReconciliationException> raised = new ArrayList<>();
        int consulted = 0;

        // A gateway that was never configured cannot confirm or deny anything, and reporting
        // "unconfirmed" for every payment would be noise rather than a finding. This is a
        // configuration state, not a discrepancy, so the sweep simply has nothing to ask.
        if (!gatewayProperties.isEnabled() || !gatewayProperties.hasApiKey()) {
            log.info("Gateway reconciliation skipped: the payment gateway is not configured here");
            return new ScanResult((int) exceptionRepository.countByStatusAndIsDeletedFalse(
                    ReconciliationException.STATUS_OPEN), raised);
        }

        for (Payment payment : paymentRepository.findByStatusAndIsDeletedFalse(
                "COMPLETED",
                PageRequest.of(0, GATEWAY_SCAN_LIMIT,
                        Sort.by(Sort.Direction.DESC, "createdAt")))) {
            if (isDeleted(payment)) {
                continue;
            }
            String providerReference = payment.getProviderReference();
            if (providerReference == null || providerReference.isBlank()
                    || !"stakaba".equalsIgnoreCase(payment.getProvider())) {
                continue; // nothing on the provider side to consult
            }
            consulted++;
            consultGateway(raised, payment, providerReference.trim());
        }

        long open = exceptionRepository.countByStatusAndIsDeletedFalse(ReconciliationException.STATUS_OPEN);
        log.info("Gateway reconciliation consulted {} payment(s), raised {} new exception(s); {} open",
                consulted, raised.size(), open);
        return new ScanResult((int) open, raised);
    }

    private void consultGateway(List<ReconciliationException> raised, Payment payment,
                                String providerReference) {
        StakabaGatewayClient.GatewayTransaction transaction;
        try {
            transaction = gatewayClient.getTransaction(providerReference);
        } catch (PaymentGatewayException ex) {
            if (PaymentGatewayException.Code.NOT_FOUND.equals(ex.getCode())) {
                raise(raised, ReconciliationException.TYPE_INCONSISTENT_STATUS,
                        reference(payment), payment, payment.getAmount(),
                        "Payment is COMPLETED but the gateway reports no transaction for "
                                + providerReference + "; fulfilment already happened and is left "
                                + "untouched for human review");
                return;
            }
            // Auth, rate limit, 5xx, transport, malformed body. Record the uncertainty; inventing
            // a verdict in either direction is exactly what must not happen.
            raise(raised, ReconciliationException.TYPE_GATEWAY_UNCERTAIN,
                    reference(payment), payment, payment.getAmount(),
                    "Gateway could not be consulted for " + providerReference + " ("
                            + ex.getCode() + "), so this COMPLETED payment's outcome is unconfirmed. "
                            + "No status, amount or access was changed");
            return;
        } catch (RuntimeException ex) {
            raise(raised, ReconciliationException.TYPE_GATEWAY_UNCERTAIN,
                    reference(payment), payment, payment.getAmount(),
                    "Gateway lookup failed unexpectedly for " + providerReference + " ("
                            + ex.getClass().getSimpleName() + "); the outcome stays unconfirmed "
                            + "and nothing was changed");
            return;
        }

        if (transaction == null) {
            raise(raised, ReconciliationException.TYPE_GATEWAY_UNCERTAIN,
                    reference(payment), payment, payment.getAmount(),
                    "Gateway returned no transaction for " + providerReference
                            + "; the outcome stays unconfirmed and nothing was changed");
            return;
        }

        if (transaction.isFailed()) {
            raise(raised, ReconciliationException.TYPE_INCONSISTENT_STATUS,
                    reference(payment), payment, payment.getAmount(),
                    "Payment is COMPLETED locally but the gateway reports FAILED for "
                            + providerReference + "; the payment stands and is flagged for review");
            return;
        }

        // An in-flight or otherwise unrecognised provider status is not evidence of anything;
        // it becomes reportable only when it disagrees on money.
        Mismatch mismatch = describeGatewayMismatch(payment, transaction);
        if (mismatch != null) {
            raise(raised, mismatch.type(), reference(payment), payment, payment.getAmount(),
                    mismatch.description());
        }
    }

    private record Mismatch(String type, String description) {
    }

    /**
     * Compares the stored payment with what the provider says it took, without ever converting.
     *
     * @return the disagreement to report, or null when the figures agree or the provider supplied
     *         nothing to compare.
     */
    private Mismatch describeGatewayMismatch(Payment payment,
                                             StakabaGatewayClient.GatewayTransaction transaction) {
        BigDecimal gatewayAmount = transaction.grossAmount();
        String gatewayCurrency = transaction.currency();

        if (payment.getAmount() != null && gatewayAmount != null
                && gatewayAmount.compareTo(payment.getAmount()) != 0) {
            return new Mismatch(ReconciliationException.TYPE_AMOUNT_MISMATCH,
                    "Payment records TZS " + payment.getAmount() + " but the gateway reports TZS "
                            + gatewayAmount + " for the same transaction; neither figure is "
                            + "changed and no conversion is performed");
        }
        if (payment.getAmount() != null && gatewayAmount == null && transaction.isSuccess()) {
            return new Mismatch(ReconciliationException.TYPE_AMOUNT_MISMATCH,
                    "Payment records TZS " + payment.getAmount() + " but the gateway confirmed "
                            + "success without reporting an amount; the payment stands and the "
                            + "missing figure is flagged rather than guessed");
        }

        if (gatewayCurrency != null && !gatewayCurrency.isBlank()
                && !SUPPORTED_CURRENCY.equalsIgnoreCase(gatewayCurrency)) {
            return new Mismatch(ReconciliationException.TYPE_CURRENCY_MISMATCH,
                    "Gateway reports currency " + gatewayCurrency + " but only " + SUPPORTED_CURRENCY
                            + " is issued or accepted; no conversion is performed and no rate is "
                            + "invented");
        }
        if (payment.getCurrency() != null && gatewayCurrency != null
                && !payment.getCurrency().equalsIgnoreCase(gatewayCurrency)) {
            return new Mismatch(ReconciliationException.TYPE_CURRENCY_MISMATCH,
                    "Payment records currency " + payment.getCurrency() + " while the gateway "
                            + "reports " + gatewayCurrency + "; the two are left as they stand "
                            + "for human review");
        }
        return null;
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

    /**
     * Currency the platform never issued, or no currency at all (OP-06, §70).
     *
     * <p>Stored currency is set from an approved policy at initiation and the gateway's own
     * figure is reconciled against it, so a value that is missing or not TZS here means the
     * record was altered after the fact or written outside the rules. Both cases are reported;
     * neither is converted, and no exchange rate is ever looked up.</p>
     */
    private List<ReconciliationException> scanCurrencyInconsistencies() {
        List<ReconciliationException> raised = new ArrayList<>();
        for (Payment payment : paymentRepository.findAll()) {
            if (isDeleted(payment)) {
                continue;
            }
            String currency = payment.getCurrency();
            if (currency == null || currency.isBlank()) {
                raise(raised, ReconciliationException.TYPE_CURRENCY_MISMATCH,
                        reference(payment), payment, payment.getAmount(),
                        "Payment records no currency; " + SUPPORTED_CURRENCY
                                + " is the only currency issued and none is assumed here");
                continue;
            }
            if (!SUPPORTED_CURRENCY.equalsIgnoreCase(currency.trim())) {
                raise(raised, ReconciliationException.TYPE_CURRENCY_MISMATCH,
                        reference(payment), payment, payment.getAmount(),
                        "Payment records currency " + currency + " but only " + SUPPORTED_CURRENCY
                                + " is issued or accepted; it is reported, not converted, and no "
                                + "rate is invented");
            }
        }
        return raised;
    }

    /**
     * Commission and provider share that do not add up to the transaction (OP-10, §70).
     *
     * <p>Allocation is stamped once, at confirmation, from the policy live at that moment. Four
     * things can go wrong afterwards and all four are reportable: a resolved allocation whose
     * figures are missing or do not sum to the net amount, figures recorded under a status that
     * never resolved, a negative figure, and a completed payment that carries no allocation
     * status at all.</p>
     *
     * <p>Deliberately <b>not</b> flagged: {@code NO_ACTIVE_POLICY}, {@code RATE_NOT_APPROVED} and
     * {@code RATE_EXCEEDS_AMOUNT} with null figures. The commission rate is still an open owner
     * decision ({@code Payment.md} §2), so "unresolved" is the honest expected state - reporting
     * it as a discrepancy would raise an exception on every payment until the business decides.
     * A {@code RATE_EXCEEDS_AMOUNT} that <i>did</i> stamp a partial figure is flagged, because a
     * number without a resolved allocation must never reach an earnings report.</p>
     */
    private List<ReconciliationException> scanEarningsAllocations() {
        List<ReconciliationException> raised = new ArrayList<>();
        for (Payment payment : paymentRepository.findAll()) {
            if (isDeleted(payment) || !"COMPLETED".equals(payment.getStatus())) {
                continue;
            }
            String status = payment.getCommissionStatus();
            if (status == null || status.isBlank()) {
                raise(raised, ReconciliationException.TYPE_FEE_MISMATCH,
                        reference(payment), payment, payment.getAmount(),
                        "Payment is COMPLETED but carries no commission allocation status; the "
                                + "allocation was never recorded");
                continue;
            }

            boolean resolved = "RESOLVED".equals(status.trim());
            BigDecimal commission = payment.getCommissionAmount();
            BigDecimal share = payment.getProviderShareAmount();

            if (resolved) {
                if (commission == null || share == null) {
                    raise(raised, ReconciliationException.TYPE_FEE_MISMATCH,
                            reference(payment), payment, payment.getAmount(),
                            "Allocation is RESOLVED but commission TZS " + commission
                                    + " and provider share TZS " + share + " are not both recorded");
                    continue;
                }
                if (payment.getCommissionPolicyKey() == null
                        || payment.getCommissionPolicyVersion() == null) {
                    raise(raised, ReconciliationException.TYPE_FEE_MISMATCH,
                            reference(payment), payment, payment.getAmount(),
                            "Allocation is RESOLVED but names no commission policy or version; "
                                    + "the figures cannot be traced to an approved rule");
                    continue;
                }
                if (commission.signum() < 0 || share.signum() < 0) {
                    raise(raised, ReconciliationException.TYPE_FEE_MISMATCH,
                            reference(payment), payment, payment.getAmount(),
                            "Allocation records a negative amount: commission TZS " + commission
                                    + ", provider share TZS " + share);
                    continue;
                }
                BigDecimal allocated = commission.add(share);
                if (payment.getNetAmount() != null && allocated.compareTo(payment.getNetAmount()) != 0) {
                    raise(raised, ReconciliationException.TYPE_FEE_MISMATCH,
                            reference(payment), payment, payment.getNetAmount(),
                            "Commission TZS " + commission + " plus provider share TZS " + share
                                    + " = TZS " + allocated + " but the payment's net amount is TZS "
                                    + payment.getNetAmount());
                }
                continue;
            }

            if (commission != null || share != null) {
                raise(raised, ReconciliationException.TYPE_FEE_MISMATCH,
                        reference(payment), payment, payment.getAmount(),
                        "Allocation is " + status + " yet records commission TZS " + commission
                                + " and provider share TZS " + share + "; an unresolved allocation "
                                + "must carry no figures so it can never be reported as earnings");
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
