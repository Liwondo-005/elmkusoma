package tz.elmkusoma.parent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.service.PlatformIntegrationService;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.payments.gateway.MobileMoneyNetwork;
import tz.elmkusoma.payments.gateway.MsisdnNormalizer;
import tz.elmkusoma.payments.gateway.PaymentGatewayException;
import tz.elmkusoma.payments.gateway.StakabaGatewayClient;
import tz.elmkusoma.payments.gateway.StakabaProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates Stakaba mobile-money collections against the existing payment
 * domain. Owns every outbound gateway call made on behalf of a {@link Payment}.
 *
 * <p>Invariants this service exists to keep:</p>
 * <ul>
 *   <li>A local PENDING payment always exists <em>before</em> any collection
 *       request leaves this server (the caller creates it via
 *       {@link ParentPaymentService#initiatePayment}); this service only ever
 *       attaches a provider reference to it.</li>
 *   <li>A successful collection response means the USSD prompt was queued -
 *       the payment stays PENDING until
 *       {@link #verifyPaymentWithGateway} sees a SUCCESS transaction from the
 *       gateway's own status API. Nothing here marks a payment COMPLETED.</li>
 *   <li>Verification is idempotent: a COMPLETED payment short-circuits, and
 *       the row is pessimistically locked so concurrent webhooks/status
 *       checks cannot grant the entitlement twice.</li>
 *   <li>Ambiguous outcomes (timeout after the request may have reached
 *       Stakaba) never trigger a blind retry of the non-idempotent collection
 *       call; the payment is flagged and left for verification or a fresh
 *       attempt.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService {

    public static final String PROVIDER_STAKABA = "stakaba";

    private final StakabaProperties properties;
    private final StakabaGatewayClient gatewayClient;
    private final PaymentRepository paymentRepository;
    private final ParentRepository parentRepository;
    private final ParentPaymentService paymentService;
    private final AuditLogRepository auditLogRepository;
    private final PlatformIntegrationService integrationService;
    private final ObjectMapper objectMapper;

    /** Outcome of processing one Stakaba webhook event. */
    public record WebhookOutcome(String paymentId, String status, boolean processed,
                                 boolean fulfilled, String reason) {

        static WebhookOutcome ignored(String reason) {
            return new WebhookOutcome(null, null, false, false, reason);
        }
    }

    // ── Collection initiation ──────────────────────────────────────────

    /**
     * Loads a payment, enforces that the caller owns it, then initiates the
     * collection. Used by the "pay this pending payment with mobile money"
     * endpoint; ownership failures are indistinguishable from not-found to
     * probe IDOR attempts.
     */
    @Transactional
    public Payment initiateCollectionForOwnedPayment(UUID paymentId, java.util.UUID callerUserId,
                                                      String rawMobileNumber, String rawNetwork) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        requireOwnership(payment, callerUserId);
        return initiateCollection(payment, rawMobileNumber, rawNetwork);
    }

    /**
     * Sends a collection request for an already-persisted PENDING payment and
     * stores the returned Stakaba {@code internalReference} as the payment's
     * provider reference. The payment remains PENDING.
     *
     * <p>Idempotent with respect to an already-initiated payment: if a
     * reference is already stored, no second collection is sent (a retry of
     * the same checkout must not queue a second USSD prompt).</p>
     */
    @Transactional
    public Payment initiateCollection(Payment payment, String rawMobileNumber, String rawNetwork) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Mobile money payments are not enabled on this server");
        }
        if ("COMPLETED".equals(payment.getStatus())) {
            throw new IllegalStateException("This payment is already completed");
        }
        if (payment.getProviderReference() != null && !payment.getProviderReference().isBlank()) {
            // Already initiated - replay the same attempt rather than charging twice.
            log.info("Payment {} already has gateway reference {}; not re-initiating",
                    payment.getId(), payment.getProviderReference());
            return payment;
        }
        if (!"PENDING".equals(payment.getStatus())) {
            throw new IllegalStateException(
                    "Only pending payments can be paid (current status: " + payment.getStatus() + ")");
        }
        String currency = payment.getCurrency() == null ? "TZS" : payment.getCurrency();
        if (!"TZS".equalsIgnoreCase(currency)) {
            throw new IllegalStateException("Mobile money collections are only supported in TZS");
        }
        BigDecimal amount = payment.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalStateException("Payment amount must be positive");
        }
        if (amount.remainder(BigDecimal.ONE).signum() != 0) {
            // Operators reject decimals; refuse before the gateway does.
            throw new IllegalStateException(
                    "Mobile money requires a whole TZS amount (this payment is " + amount + ")");
        }
        String msisdn = MsisdnNormalizer.normalize(rawMobileNumber);
        String network = MobileMoneyNetwork.from(rawNetwork).apiValue();

        Map<String, String> metadata = Map.of(
                "paymentId", payment.getId().toString(),
                "serviceType", payment.getServiceType());

        StakabaGatewayClient.CollectionInitiated result;
        try {
            result = gatewayClient.initiateCollection(amount, "TZS", msisdn, network, metadata);
        } catch (PaymentGatewayException ex) {
            if (isAmbiguous(ex)) {
                // The prompt may still reach the payer. Flag the record so
                // nobody treats it as a clean, retryable failure.
                flagAmbiguousInitiation(payment, ex.getCode().name());
            }
            log.warn("Stakaba collection failed for payment {}: code={}",
                    payment.getId(), ex.getCode());
            throw ex;
        }

        payment.setProvider(PROVIDER_STAKABA);
        payment.setProviderReference(result.internalReference());
        payment.setStatus("PENDING");
        mergeMetadata(payment, Map.of(
                "gateway", PROVIDER_STAKABA,
                "mobileNumber", msisdn,
                "network", network,
                "merchantReference", payment.getId().toString(),
                "initiationAmbiguous", "false"));
        payment = paymentRepository.save(payment);

        audit(payment, "Payment Initiation",
                Map.of("status", "PENDING", "provider", PROVIDER_STAKABA,
                        "network", network, "mobileNumber", msisdn),
                Map.of("providerReference", result.internalReference()));
        log.info("Stakaba collection initiated: payment={} internalReference={}",
                payment.getId(), result.internalReference());
        return payment;
    }

    // ── Server-side verification (status endpoint + webhook fulfilment) ─

    /**
     * Verifies a payment against the gateway's transaction-status API and, on
     * a confirmed SUCCESS that matches the stored record, completes it via the
     * existing {@code verifyPayment} pipeline exactly once (entitlement,
     * receipt, commission and audit all unchanged).
     *
     * @param callerUserId authenticated parent, or {@code null} when invoked
     *                     from the unauthenticated webhook path (system actor)
     */
    @Transactional
    public Payment verifyPaymentWithGateway(UUID paymentId, java.util.UUID callerUserId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (callerUserId != null) {
            requireOwnership(payment, callerUserId);
        }
        return applyGatewayVerification(payment, callerUserId);
    }

    /**
     * Loads a payment the caller owns, without touching the gateway. Used to
     * answer a status poll when the gateway itself is unreachable.
     */
    public Payment getOwnedPayment(UUID paymentId, java.util.UUID callerUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        requireOwnership(payment, callerUserId);
        return payment;
    }

    /**
     * Shared verification core. The payment row MUST already be locked by the
     * caller's transaction so concurrent webhook and status-check processing
     * serialise here and the COMPLETED short-circuit is race-free.
     */
    private Payment applyGatewayVerification(Payment payment, java.util.UUID actor) {
        if ("COMPLETED".equals(payment.getStatus())) {
            // Idempotent: a duplicate webhook or a status poll after success
            // must not grant a second entitlement.
            log.info("Payment {} already COMPLETED; verification is a no-op", payment.getId());
            return payment;
        }
        String reference = payment.getProviderReference();
        if (reference == null || reference.isBlank() || !PROVIDER_STAKABA.equals(payment.getProvider())) {
            throw new IllegalStateException(
                    "This payment has no mobile-money reference to verify yet");
        }

        StakabaGatewayClient.GatewayTransaction transaction;
        try {
            transaction = gatewayClient.getTransaction(reference);
        } catch (PaymentGatewayException ex) {
            if (ex.getCode() == PaymentGatewayException.Code.NOT_FOUND) {
                // Provider has not registered the reference (yet). Keep PENDING;
                // the payer's own status poll or a later webhook can retry.
                log.info("Gateway does not know reference {} yet for payment {}",
                        reference, payment.getId());
                return payment;
            }
            throw ex;
        }

        if (transaction.isSuccess()) {
            requireTransactionMatches(payment, transaction);
            if (paymentRepository.existsByProviderReferenceAndIdNotAndIsDeletedFalse(
                    reference, payment.getId())) {
                throw new IllegalStateException(
                        "Gateway reference is already bound to another payment");
            }
            return paymentService.verifyPayment(payment.getId(), reference, actor, PROVIDER_STAKABA);
        }
        if (transaction.isFailed()) {
            if (!"COMPLETED".equals(payment.getStatus())) {
                String oldStatus = payment.getStatus();
                payment.setStatus("FAILED");
                mergeMetadata(payment, Map.of("failureSource", "stakaba-status-lookup"));
                payment = paymentRepository.save(payment);
                audit(payment, "Payment Failure",
                        Map.of("status", oldStatus), Map.of("status", "FAILED"));
                log.info("Payment {} marked FAILED by gateway status lookup", payment.getId());
            }
            return payment;
        }
        // Still in flight (PENDING/PROCESSING/unknown non-final): leave untouched.
        return payment;
    }

    /**
     * A SUCCESS transaction that disagrees with the stored payment is never
     * fulfilled. Amount, currency and reference are all re-checked even though
     * initiation set them, because the stored row - not the gateway - is what
     * entitlement is granted against, and a mismatch means something outside
     * this system's control changed.
     */
    private void requireTransactionMatches(Payment payment,
                                           StakabaGatewayClient.GatewayTransaction transaction) {
        boolean mismatch = false;
        if (transaction.grossAmount() == null) {
            // A claimed success with no amount cannot be reconciled; fulfilment
            // is refused rather than assumed.
            mismatch = true;
        } else if (payment.getAmount() == null
                || transaction.grossAmount().compareTo(payment.getAmount()) != 0) {
            mismatch = true;
        }
        if (!mismatch && transaction.currency() != null && payment.getCurrency() != null
                && !transaction.currency().equalsIgnoreCase(payment.getCurrency())) {
            mismatch = true;
        }
        if (mismatch) {
            log.error("Stakaba transaction {} mismatches payment {} (gateway amount={} {}, "
                            + "stored amount={} {}); payment left unfulfilled",
                    transaction.internalReference(), payment.getId(),
                    transaction.grossAmount(), transaction.currency(),
                    payment.getAmount(), payment.getCurrency());
            integrationService.recordWebhook("PAYMENT", "stakaba_verification", "VERIFIED",
                    "FAILED", "Transaction amount/currency mismatch - payment not fulfilled");
            mergeMetadata(payment, Map.of("verificationMismatch", "true"));
            paymentRepository.save(payment);
            throw new IllegalStateException(
                    "Gateway transaction does not match this payment; it has been left "
                            + "unfulfilled for review");
        }
    }

    // ── Webhook processing ─────────────────────────────────────────────

    /**
     * Processes one documented Stakaba event ({@code transaction.success} /
     * {@code transaction.failed}).
     *
     * <p>Stakaba documents no webhook signature, so authenticity cannot be
     * proven from the request itself. Following the official guidance, an
     * event is only ever a <em>hint</em>: fulfilment happens exclusively after
     * this method re-reads the transaction from the gateway status API and it
     * matches the stored payment. A forged or replayed event therefore cannot
     * grant access on its own.</p>
     *
     * @throws PaymentGatewayException when the mandatory API verification
     *         cannot be completed (controller maps this to a 5xx so Stakaba's
     *         documented retry loop redelivers the event)
     */
    @Transactional
    public WebhookOutcome handleStakabaEvent(String event, String internalReference,
                                             BigDecimal eventAmount, String eventCurrency) {
        if (internalReference == null || internalReference.isBlank()) {
            throw new IllegalArgumentException("internalReference is required");
        }
        java.util.Optional<Payment> found =
                paymentRepository.findByProviderReferenceAndIsDeletedFalse(internalReference);
        if (found.isEmpty()) {
            // Acknowledged but ignored: not one of our references. Never fulfil
            // an unknown reference, and never leak whether it exists elsewhere.
            log.warn("Stakaba webhook for unknown reference {}; acknowledged and ignored",
                    internalReference);
            integrationService.recordWebhook("PAYMENT", "stakaba_webhook", "UNVERIFIED",
                    "SUCCESS", "Unknown internalReference - acknowledged, not processed");
            return WebhookOutcome.ignored("unknown_reference");
        }
        Payment payment = found.get();

        if (!PROVIDER_STAKABA.equals(payment.getProvider())) {
            integrationService.recordWebhook("PAYMENT", "stakaba_webhook", "UNVERIFIED",
                    "FAILED", "Reference belongs to a non-stakaba payment");
            return WebhookOutcome.ignored("reference_not_stakaba");
        }

        String expectedEvent = isSuccessEvent(event) ? "success" : "failed";
        log.info("Stakaba webhook received: event={} reference={} payment={} "
                        + "(payload amount={} {}; treating as {} hint)",
                event, internalReference, payment.getId(), eventAmount, eventCurrency, expectedEvent);

        // Mandatory server-side verification - the docs are explicit that the
        // webhook alone must never fulfil an order. applyGatewayVerification
        // locks nothing itself; this method's transaction holds the row lock
        // via the re-load below.
        Payment locked = paymentRepository.findByIdForUpdate(payment.getId()).orElse(payment);
        Payment verified;
        try {
            verified = applyGatewayVerification(locked, null);
        } catch (IllegalStateException refused) {
            // A SUCCESS that fails reconciliation is acknowledged but never
            // fulfilled. The refusal (and the mismatch flag already written on
            // the payment) is committed so the record survives for review;
            // acking with 200 stops pointless provider retries of a permanent
            // condition.
            log.error("Stakaba webhook verification refused for payment {}: {}",
                    payment.getId(), refused.getMessage());
            integrationService.recordWebhook("PAYMENT", "stakaba_webhook", "VERIFIED",
                    "FAILED", refused.getMessage());
            return new WebhookOutcome(payment.getId().toString(), payment.getStatus(),
                    true, false, "verification_refused");
        }

        integrationService.recordWebhook("PAYMENT", "stakaba_webhook", "VERIFIED",
                "SUCCESS", null);
        return new WebhookOutcome(
                verified.getId().toString(),
                verified.getStatus(),
                true,
                "COMPLETED".equals(verified.getStatus()),
                null);
    }

    /** True when a collection attempt may or may not have reached Stakaba. */
    private static boolean isAmbiguous(PaymentGatewayException ex) {
        return switch (ex.getCode()) {
            case TIMEOUT_AMBIGUOUS, NETWORK, PROVIDER, MALFORMED_RESPONSE -> true;
            default -> false;
        };
    }

    /** Documented success event name. Anything else is treated as a failure hint. */
    private static boolean isSuccessEvent(String event) {
        return "transaction.success".equalsIgnoreCase(event);
    }

    private void flagAmbiguousInitiation(Payment payment, String code) {
        mergeMetadata(payment, Map.of(
                "initiationAmbiguous", "true",
                "initiationAmbiguityCode", code));
        paymentRepository.save(payment);
    }

    /** Ownership rule mirrors {@code cancelPayment}: the parent profile or the raw userId. */
    private void requireOwnership(Payment payment, java.util.UUID userId) {
        Parent callerParent = parentRepository.findByUserIdAndIsDeletedFalse(userId).orElse(null);
        if (callerParent == null
                || (!callerParent.getId().equals(payment.getParentId())
                        && !userId.equals(payment.getParentId()))) {
            log.warn("IDOR blocked: user={} attempted access to payment {} of parent {}",
                    userId, payment.getId(), payment.getParentId());
            throw new ForbiddenException("Not authorized to access this payment");
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeMetadata(Payment payment, Map<String, Object> updates) {
        Map<String, Object> current;
        try {
            current = payment.getMetadata() == null || payment.getMetadata().isBlank()
                    ? new LinkedHashMap<>()
                    : objectMapper.readValue(payment.getMetadata(), new TypeReference<LinkedHashMap<String, Object>>() {
                    });
        } catch (Exception ex) {
            current = new LinkedHashMap<>();
        }
        current.putAll(updates);
        try {
            payment.setMetadata(objectMapper.writeValueAsString(current));
        } catch (Exception ex) {
            log.warn("Could not update payment metadata for {}", payment.getId());
        }
    }

    private void audit(Payment payment, String entityName, Map<String, Object> oldValues,
                       Map<String, Object> newValues) {
        AuditLog auditLog = new AuditLog();
        auditLog.setInstitutionId(payment.getInstitutionId());
        auditLog.setUserId(null);
        auditLog.setUserEmail(ParentPaymentService.WEBHOOK_SYSTEM_ACTOR);
        auditLog.setUserRole("SYSTEM");
        auditLog.setEntityType("Payment");
        auditLog.setEntityId(payment.getId());
        auditLog.setEntityName(entityName);
        auditLog.setAction(AuditLog.AuditAction.UPDATE);
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);
        auditLogRepository.save(auditLog);
    }
}
