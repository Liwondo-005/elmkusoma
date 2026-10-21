package tz.elmkusoma.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.RefundRequest;
import tz.elmkusoma.parent.dto.ParentPaymentResponse;
import tz.elmkusoma.parent.dto.ParentPaymentResponse.PaymentItem;
import tz.elmkusoma.parent.dto.ParentPaymentResponse.TimelineEvent;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.pricing.service.CommissionService;
import tz.elmkusoma.pricing.service.DiscountService;
import tz.elmkusoma.pricing.service.PricingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentPaymentService {

    private final PaymentRepository paymentRepository;
    private final ParentRepository parentRepository;
    private final EntitlementRepository entitlementRepository;
    private final AuditLogRepository auditLogRepository;
    private final PricingService pricingService;
    private final DiscountService discountService;
    private final CommissionService commissionService;
    private final ReceiptService receiptService;
    private final tz.elmkusoma.finance.service.ReconciliationService reconciliationService;
    private final RefundService refundService;
    private final tz.elmkusoma.subscription.service.SubscriptionService subscriptionService;
    private final tz.elmkusoma.event.service.EventBookingService eventBookingService;
    private final tz.elmkusoma.event.repository.EventRegistrationRepository eventRegistrationRepository;
    private final tz.elmkusoma.bundle.service.BundleGrantService bundleGrantService;
    private final tz.elmkusoma.bundle.service.BundleTopUpService bundleTopUpService;
    private final PaymentEventNotificationService paymentEventNotificationService;

    /** Actor label stamped on audit rows created from the unauthenticated gateway webhook
     * (which passes verifiedBy=null). Reuses the repo's string-actor convention for
     * non-user system activity; stored in the existing nullable userEmail/userRole columns. */
    public static final String WEBHOOK_SYSTEM_ACTOR = "payment-webhook";

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tz.elmkusoma.administration.service.PlatformPolicyService platformPolicyService;

    public ParentPaymentResponse getPayments(UUID parentId) {
        List<Payment> allPayments = paymentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(parentId);
        List<Payment> completedPayments = allPayments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .collect(Collectors.toList());

        BigDecimal totalPaid = completedPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> pendingPayments = allPayments.stream()
                .filter(p -> "PENDING".equals(p.getStatus()) || "PROCESSING".equals(p.getStatus()))
                .collect(Collectors.toList());

        return ParentPaymentResponse.builder()
                .outstanding(pendingPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .paidThisTerm(totalPaid)
                .recentPayments(completedPayments.stream().limit(10).map(this::toPaymentItem).collect(Collectors.toList()))
                .pendingPayments(pendingPayments.stream().map(this::toPaymentItem).collect(Collectors.toList()))
                .build();
    }

    public List<PaymentItem> getPaymentsByStudent(UUID studentId) {
        return paymentRepository.findByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId)
                .stream().map(this::toPaymentItem).collect(Collectors.toList());
    }

    @Transactional
    public Payment initiatePayment(UUID parentId, UUID studentId, UUID institutionId,
                                   BigDecimal amount, String serviceType, UUID serviceId,
                                   String description) {
        return initiatePayment(parentId, studentId, institutionId, amount, serviceType,
                serviceId, description, null);
    }

    /**
     * Initiates a purchase, optionally under a client-supplied idempotency key.
     *
     * <p>The key exists so a browser retry, a lost response or a double-click cannot create two
     * payments for one purchase. It is enforced by a partial unique index, so two truly
     * concurrent requests are resolved by the database rather than by a race in this method.</p>
     */
    @Transactional
    public Payment initiatePayment(UUID parentId, UUID studentId, UUID institutionId,
                                   BigDecimal amount, String serviceType, UUID serviceId,
                                   String description, String idempotencyKey) {
        if (platformPolicyService != null && !platformPolicyService.chargeEnabled()) {
            throw new IllegalStateException("Platform policy forbids charging users");
        }
        // The amount to charge is resolved from an ACTIVE pricing policy, never taken from the
        // request. A caller that can name its own price can buy anything for TZS 1, and the
        // gateway webhook's reconciliation does not catch it: it only compares the gateway figure
        // against this already-tainted stored amount. A caller figure that disagrees with the
        // authoritative one is refused rather than used.
        PricingService.ResolvedPrice resolved = pricingService.resolvePrice(serviceType, null);

        // BR-03. A bundle a learner partly already holds is not worth its headline price, so the
        // authoritative amount for a bundle is the server-side top-up quote, recomputed here at the
        // moment of purchase. Comparing the caller against the headline policy price instead is what
        // made partial top-ups impossible: the correct figure was rejected as a mismatch with the
        // very policy that produced it. Recomputing rather than trusting the quote the client saw
        // is also what closes the gap between quoting and paying - a learner who gains coverage
        // between the two is charged the smaller correct amount, not the larger displayed one.
        BigDecimal authoritativeAmount = resolved.amount();
        boolean bundlePurchase = bundleGrantService.isBundleServiceType(serviceType);
        if (bundlePurchase) {
            tz.elmkusoma.bundle.service.BundleTopUpService.TopUpQuote topUp =
                    bundleTopUpService.quoteByServiceType(serviceType, studentId, LocalDateTime.now());
            if (topUp.fullyCovered()) {
                // Nothing to buy. Charging anyway would take money for access the learner has.
                throw new IllegalStateException(
                        "This learner already holds every service in this bundle; there is nothing "
                                + "left to pay for");
            }
            authoritativeAmount = topUp.total();
        }
        if (bundlePurchase) {
            pricingService.requireAmountMatches(amount, authoritativeAmount,
                    "the bundle top-up quote for this learner");
        } else {
            pricingService.requireAmountMatches(amount, resolved);
        }

        // An approved discount, if one applies. Derived server-side from the catalogue price, so
        // a client cannot name its own discount any more than it can name its own price.
        // Not applied to a bundle: its total is already the sum of per-item policy prices, and
        // spreading a bundle-level discount across those components is a commercial decision that
        // has not been approved.
        DiscountService.ResolvedDiscount discount = bundlePurchase
                ? DiscountService.ResolvedDiscount.none()
                : discountService.resolve(serviceType, resolved.amount());
        BigDecimal net = authoritativeAmount.subtract(discount.amount());

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Payment> replayed = paymentRepository
                    .findByIdempotencyKeyAndIsDeletedFalse(idempotencyKey.trim());
            if (replayed.isPresent()) {
                log.info("Payment {} replayed for idempotency key", replayed.get().getId());
                return replayed.get();
            }
        }

        Payment payment = Payment.builder()
                .parentId(parentId)
                .studentId(studentId)
                .institutionId(institutionId)
                .amount(net)
                .currency(resolved.currency())
                .serviceType(serviceType)
                .serviceId(serviceId)
                .description(description)
                .status("PENDING")
                // Terms snapshotted now, so a later price or policy change cannot rewrite what
                // the payer actually agreed to.
                .policyKey(resolved.policyKey())
                .policyVersion(resolved.policyVersion())
                .grossAmount(resolved.amount())
                .discountAmount(discount.amount())
                .netAmount(net)
                .accessDays(resolved.accessDays())
                .refundedAmount(BigDecimal.ZERO)
                .idempotencyKey(idempotencyKey == null || idempotencyKey.isBlank()
                        ? null : idempotencyKey.trim())
                .build();
        payment = paymentRepository.save(payment);
        log.info("Payment initiated: {} for student {} amount {} {} (policy {} v{})",
                payment.getId(), studentId, net, serviceType,
                resolved.policyKey(), resolved.policyVersion());
        // The derived inbox is silent about PENDING rows on purpose, so without this the payer
        // would hear nothing at all until the gateway answered. Reports only: it touches no
        // entitlement, and a failure here never fails the payment that was just written.
        paymentEventNotificationService.paymentInitiated(payment);
        return payment;
    }

    @Transactional
    public Payment verifyPayment(UUID paymentId, String providerReference, UUID verifiedBy) {
        return verifyPayment(paymentId, providerReference, verifiedBy, "manual");
    }

    /**
     * Confirms a payment and grants its entitlement exactly once.
     *
     * @param provider which gateway confirmed this payment; the historical
     *                 default is {@code "manual"} (admin verification). The
     *                 Stakaba adapter passes {@code "stakaba"} so the audit
     *                 trail and reports show how the confirmation happened.
     *                 Callers MUST have already reconciled amount/currency
     *                 against the stored record (the webhook controller and
     *                 {@code PaymentGatewayService} both do this before
     *                 calling).
     */
    @Transactional
    public Payment verifyPayment(UUID paymentId, String providerReference, UUID verifiedBy,
                                 String provider) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException("Payment not found"));

        String oldStatus = payment.getStatus();

        if ("COMPLETED".equals(payment.getStatus())) {
            log.warn("Duplicate callback for payment {}", paymentId);
            return payment;
        }

        // NOTE: amount/currency are intentionally never modified here. The stored initiated
        // record is authoritative; the webhook controller reconciles any gateway-sent
        // amount/currency against it before calling verify.
        payment.setProviderReference(providerReference);
        payment.setProvider(provider == null || provider.isBlank() ? "manual" : provider);
        payment.setStatus("COMPLETED");
        payment.setPaidAt(LocalDateTime.now());

        // Allocate revenue now, from the policy that is ACTIVE at this moment, and stamp the
        // result onto the transaction. Recomputing later from a newer policy would silently
        // rewrite history. An unresolved allocation is recorded as unresolved, never as zero -
        // no commission rate is invented if the business has not approved one.
        CommissionService.Allocation allocation = commissionService.allocate(
                payment.getServiceType(), null, payment.getGrossAmount(), payment.getDiscountAmount());
        payment.setCommissionAmount(allocation.commissionAmount());
        payment.setProviderShareAmount(allocation.providerShare());
        payment.setCommissionStatus(allocation.status().name());
        payment.setCommissionPolicyKey(allocation.policyKey());
        payment.setCommissionPolicyVersion(allocation.policyVersion());
        if (!allocation.resolved()) {
            log.warn("Payment {} confirmed with UNRESOLVED commission allocation ({}): {}",
                    paymentId, allocation.status(), allocation.reason());
        }

        payment = paymentRepository.save(payment);

        Entitlement grantedEntitlement = grantEntitlement(payment);

        // The subscription is the auditable record of the period that was actually sold, and it is
        // what cancellation and expiry act on.
        openSubscriptionFor(payment, grantedEntitlement);

        // An event ticket's "product" is a seat. Entitlements and subscriptions do not apply, so
        // nothing else in this flow would ever grant the place the attendee paid for: the seat was
        // deliberately left unheld until collection landed, which meant confirmSeat had no caller
        // and a paying attendee was charged and still refused entry. The verified, persisted payment
        // is the authority for the seat, so the handover happens here and only here.
        confirmEventSeatsFor(payment);

        // A bundle grants several services. Same authoritative entry point, so the duplicate-callback
        // protection the payment already has applies here too.
        try {
            int granted = bundleGrantService.grantFor(payment);
            if (granted > 0) {
                log.info("Granted {} bundle entitlement(s) for payment {}", granted, payment.getId());
            }
        } catch (RuntimeException ex) {
            // Never roll back a confirmed payment for a derived grant. The money arrived; the
            // learner keeps a refundable payment and operators get a visible reconciliation task.
            log.error("Bundle grant failed for payment {}; payment stands", payment.getId(), ex);
            reconciliationService.raiseReceiptFailure(payment, ex);
        }

        // A receipt documents money that was actually received, so it is only issued here, once
        // the payment is confirmed. Idempotent: a duplicate callback returns the existing one.
        try {
            receiptService.issueFor(payment, null);
            paymentRepository.save(payment);
        } catch (Exception ex) {
            // A receipt failure must never turn a confirmed payment into a failed one. The
            // payment stands; the receipt is reissued through the reconciliation workflow.
            log.error("Receipt issuance failed for completed payment {}; payment is NOT rolled back",
                    paymentId, ex);
            reconciliationService.raiseReceiptFailure(payment, ex);
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setInstitutionId(payment.getInstitutionId());
        auditLog.setUserId(verifiedBy);
        if (verifiedBy == null) {
            auditLog.setUserEmail(WEBHOOK_SYSTEM_ACTOR);
            auditLog.setUserRole("SYSTEM");
        }
        auditLog.setEntityType("Payment");
        auditLog.setEntityId(paymentId);
        auditLog.setEntityName("Payment Verification");
        auditLog.setAction(AuditLog.AuditAction.UPDATE);
        auditLog.setOldValues(Map.of("status", oldStatus, "providerReference", providerReference != null ? providerReference : "manual"));
        auditLog.setNewValues(Map.of("status", "COMPLETED", "paidAt", payment.getPaidAt().toString()));
        auditLogRepository.save(auditLog);

        log.info("Payment verified and entitlement granted: {} by {}", paymentId,
                verifiedBy != null ? verifiedBy : WEBHOOK_SYSTEM_ACTOR);
        return payment;
    }

    private Entitlement grantEntitlement(Payment payment) {
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration granted = accessDurationFor(payment);

        Entitlement existing = entitlementRepository
                .findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                        payment.getStudentId(), payment.getServiceType(), payment.getServiceId())
                .orElse(null);

        if (existing != null) {
            // A renewal adds to whatever expiry already runs, it does not restart the clock.
            // Recomputing as now+duration would let a renewal that lands early silently truncate
            // a longer entitlement that is still running.
            LocalDateTime from = existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now)
                    ? existing.getExpiresAt()
                    : now;
            existing.setExpiresAt(from.plus(granted));
            existing.setStatus("ACTIVE");
            existing.setPaymentId(payment.getId());
            return entitlementRepository.save(existing);
        } else {
            Entitlement entitlement = Entitlement.builder()
                    .userId(payment.getParentId())
                    .studentId(payment.getStudentId())
                    .institutionId(payment.getInstitutionId())
                    .serviceType(payment.getServiceType())
                    .serviceId(payment.getServiceId())
                    .paymentId(payment.getId())
                    .status("ACTIVE")
                    .startsAt(now)
                    .expiresAt(now.plus(granted))
                    .build();
            return entitlementRepository.save(entitlement);
        }
    }

    /**
     * Marks entitlements whose period has run out as EXPIRED.
     *
     * <p>Nothing in the codebase ever wrote EXPIRED to an entitlement, so an elapsed row reported
     * itself ACTIVE forever. Access itself was never at risk - every read path checks the
     * timestamp as well as the status - but anything counting or filtering on the string
     * overstated live access, and the bundle grant gate trusted the string alone, which is what
     * let a repurchase after expiry be charged while its grant was refused.
     *
     * <p>Idempotent: a row already EXPIRED or REVOKED is not selected again, EXPIRED is absorbing,
     * and only rows whose own {@code expiresAt} has passed are touched. A null expiry records no
     * end and is therefore never selected. A delayed run can only leave a status stale for a
     * report; it cannot grant or deny access, which is decided from the boundary itself.
     *
     * @return how many rows were closed out
     */
    @Transactional
    public int expireElapsedEntitlements(LocalDateTime now) {
        List<Entitlement> elapsed = entitlementRepository
                .findAllByStatusAndExpiresAtLessThanEqualAndIsDeletedFalse("ACTIVE", now);
        for (Entitlement entitlement : elapsed) {
            entitlement.setStatus("EXPIRED");
            entitlementRepository.save(entitlement);
            AuditLog auditLog = new AuditLog();
            auditLog.setInstitutionId(entitlement.getInstitutionId());
            auditLog.setUserId(null);
            auditLog.setUserEmail(WEBHOOK_SYSTEM_ACTOR);
            auditLog.setUserRole("SYSTEM");
            auditLog.setEntityType("Entitlement");
            auditLog.setEntityId(entitlement.getId());
            auditLog.setEntityName("Entitlement Expiry");
            auditLog.setAction(AuditLog.AuditAction.UPDATE);
            auditLog.setOldValues(Map.of("status", "ACTIVE"));
            auditLog.setNewValues(Map.of("status", "EXPIRED",
                    "expiresAt", String.valueOf(entitlement.getExpiresAt())));
            auditLogRepository.save(auditLog);
        }
        if (!elapsed.isEmpty()) {
            log.info("Marked {} elapsed entitlement(s) EXPIRED", elapsed.size());
        }
        return elapsed.size();
    }

    /**
     * Hands over the event seats a verified ticket payment paid for.
     *
     * <p>Idempotent: the payment reaches COMPLETED once, and confirmSeat itself returns early when
     * the registration is no longer PAYMENT_PENDING, so a retried webhook cannot add a second seat.
     *
     * <p>A failure here does not roll the payment back. The money really was collected, and
     * discarding a confirmed transaction because the event filled up in the meantime would be worse
     * for both sides than an unresolved seat: the payer keeps a confirmed, refundable payment and
     * operators get a visible reconciliation task instead of a silently broken booking.
     */
    private void confirmEventSeatsFor(Payment payment) {
        if (!tz.elmkusoma.event.service.EventBookingService.SERVICE_TYPE
                .equals(payment.getServiceType())) {
            return;
        }
        try {
            eventRegistrationRepository
                    .findByPaymentIdAndIsDeletedFalse(payment.getId())
                    .ifPresent(registration -> eventBookingService.confirmSeat(registration.getId()));
        } catch (RuntimeException ex) {
            log.error("Seat confirmation failed for verified event payment {}; the payment stands "
                    + "and the seat is left for reconciliation", payment.getId(), ex);
            reconciliationService.raiseReceiptFailure(payment, ex);
        }
    }

    /**
     * Opens the auditable {@code Subscription} record for a subscription-tier payment.
     *
     * <p>{@link SubscriptionService} was fully implemented but nothing ever called it: a learner
     * paid for a month, an {@code Entitlement} was granted, and no subscription row existed at
     * all. That left the period actually sold to be reconstructible only from the entitlement, and
     * left cancellation and expiry with nothing to act on.
     *
     * <p>Only subscription tiers reach this. A single-service purchase such as an event ticket has
     * no recurring period, and {@code SubscriptionService} would rightly refuse it for carrying no
     * approved access duration - so it is not offered one.
     *
     * <p>A failure here never rolls back the payment. The money really was collected and the
     * entitlement really was granted; losing a confirmed payment because a derived reporting record
     * could not be written would be far worse than the missing row, so the failure is logged and
     * surfaced for reconciliation instead.</p>
     */
    private void openSubscriptionFor(Payment payment, Entitlement entitlement) {
        String serviceType = payment.getServiceType();
        if (serviceType == null || serviceType.isBlank()) {
            return;
        }
        if (!ACCESS_DURATIONS.containsKey(serviceType.trim().toUpperCase())) {
            return;
        }
        try {
            tz.elmkusoma.subscription.domain.Subscription opened =
                    subscriptionService.activateFromVerifiedPayment(payment, entitlement);
            paymentEventNotificationService.subscriptionActivated(opened);
        } catch (RuntimeException ex) {
            log.error("Subscription activation failed for completed payment {} (serviceType={}); "
                            + "payment is NOT rolled back and the entitlement stands",
                    payment.getId(), serviceType, ex);
            reconciliationService.raiseReceiptFailure(payment, ex);
        }
    }

    /** Service types whose access length is fixed by the business, keyed on normalised name. */
    private static final Map<String, java.time.Duration> ACCESS_DURATIONS = Map.ofEntries(
            Map.entry("DAILY_ACCESS", java.time.Duration.ofHours(24)),
            Map.entry("WEEKLY_ACCESS", java.time.Duration.ofDays(7)),
            Map.entry("MONTHLY_ACCESS", java.time.Duration.ofDays(30)),
            Map.entry("BASIC_ACCESS", java.time.Duration.ofDays(30)),
            Map.entry("PLUS_ACCESS", java.time.Duration.ofDays(30)),
            Map.entry("PREMIUM_ACCESS", java.time.Duration.ofDays(30)));

    private static final java.time.Duration DEFAULT_SERVICE_ACCESS = java.time.Duration.ofDays(30);

    /**
     * Access length granted by one verified payment.
     *
     * <p>The access passes are rolling durations decided by the business: daily is exactly 24
     * hours from verified activation, weekly exactly 7 days, and monthly plus every subscription
     * tier exactly 30 days - deliberately not "until the end of the month", because that would
     * make the same purchase worth between 1 and 30 days depending on when it was bought.</p>
     *
     * <p>Anything else is a single-service purchase and keeps the historical 30-day grant, so no
     * existing buyer loses access. New durations belong here rather than as literals at call
     * sites, which is how every service ended up on the same hard-coded 30 days before.</p>
     */
    static java.time.Duration accessDurationFor(String serviceType) {
        if (serviceType == null) {
            return DEFAULT_SERVICE_ACCESS;
        }
        return ACCESS_DURATIONS.getOrDefault(serviceType.trim().toUpperCase(java.util.Locale.ROOT),
                DEFAULT_SERVICE_ACCESS);
    }

    /**
     * Access length a specific payment buys.
     *
     * <p>The duration snapshotted onto the payment at initiation wins, because that is the term
     * the payer actually bought. Falling back to the service-type table only covers payments
     * initiated before the snapshot column existed, so existing buyers keep exactly the access
     * they already had.</p>
     */
    static java.time.Duration accessDurationFor(Payment payment) {
        if (payment != null && payment.getAccessDays() != null && payment.getAccessDays() > 0) {
            return java.time.Duration.ofDays(payment.getAccessDays());
        }
        return accessDurationFor(payment == null ? null : payment.getServiceType());
    }

    public boolean hasEntitlement(UUID studentId, String serviceType, UUID serviceId) {
        return entitlementRepository
                .findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(studentId, serviceType, serviceId)
                .filter(this::isCurrentlyValid)
                .isPresent();
    }

    /**
     * Whether an entitlement is granting access right now.
     *
     * <p>Status alone is not enough: an entitlement past its expiry keeps status ACTIVE until
     * something flips it, so the old status-only check kept serving paid content to learners whose
     * access had already lapsed. Expiry is therefore evaluated at read time as well as being
     * written at grant time.</p>
     */
    boolean isCurrentlyValid(Entitlement entitlement) {
        if (entitlement == null || !"ACTIVE".equals(entitlement.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (entitlement.getStartsAt() != null && entitlement.getStartsAt().isAfter(now)) {
            return false;
        }
        return entitlement.getExpiresAt() == null || entitlement.getExpiresAt().isAfter(now);
    }

    @Transactional
    public Payment cancelPayment(UUID paymentId, UUID userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException("Payment not found"));

        if ("COMPLETED".equals(payment.getStatus())) {
            throw new IllegalStateException("Cannot cancel a completed payment");
        }
        if (!"PENDING".equals(payment.getStatus())) {
            throw new IllegalStateException("Only pending payments can be cancelled");
        }
        Parent callerParent = parentRepository.findByUserIdAndIsDeletedFalse(userId).orElse(null);
        if (callerParent == null
                || (!callerParent.getId().equals(payment.getParentId())
                        && !userId.equals(payment.getParentId()))) {
            throw new tz.elmkusoma.exception.ForbiddenException("Not authorized to cancel this payment");
        }

        payment.setStatus("CANCELLED");
        payment = paymentRepository.save(payment);

        AuditLog auditLog = new AuditLog();
        auditLog.setInstitutionId(payment.getInstitutionId());
        auditLog.setUserId(userId);
        auditLog.setEntityType("Payment");
        auditLog.setEntityId(paymentId);
        auditLog.setEntityName("Payment Cancellation");
        auditLog.setAction(AuditLog.AuditAction.UPDATE);
        auditLog.setOldValues(Map.of("status", "PENDING"));
        auditLog.setNewValues(Map.of("status", "CANCELLED"));
        auditLogRepository.save(auditLog);

        log.info("Payment cancelled: {} by {}", paymentId, userId);
        return payment;
    }

    /**
     * Full refund of a completed payment, as exposed by the legacy admin endpoint.
     *
     * <p>This used to be a second, independent money-moving path: it set the status to REFUNDED
     * and revoked the entitlement, but never recorded the amount in {@code refundedAmount} and
     * never wrote a credit note. Because the refundable balance is computed as
     * {@code amount - refundedAmount}, a payment refunded this way still looked fully refundable,
     * so the governed lifecycle would happily refund it a second time - the same money returned
     * twice, with the receipt showing no reversal at all.</p>
     *
     * <p>It is now a thin wrapper over the governed lifecycle rather than a parallel
     * implementation: the request, the decision and the completion all run through
     * {@link RefundService}, which owns the single financial adjustment, the entitlement decision
     * and the credit note. The signature, the "only completed" rule, the full-refund amount, the
     * always-revoke behaviour and the legacy audit entry are all unchanged, so the existing API
     * contract holds - only the accounting behind it is now correct, and the shared row lock plus
     * the {@code uniq_refund_open_per_payment} index make a duplicate impossible even under
     * concurrency.</p>
     */
    @Transactional
    public Payment refundPayment(UUID paymentId, UUID refundedBy, String reason) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException("Payment not found"));

        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new IllegalStateException("Only completed payments can be refunded");
        }

        BigDecimal refundable = payment.refundableAmount();
        if (refundable.signum() <= 0) {
            // The balance is already exhausted - typically by a refund taken through the
            // governed lifecycle. Refused rather than applied, so nothing can be refunded twice.
            throw new IllegalStateException(
                    "Payment " + paymentId + " has no refundable balance remaining");
        }

        String effectiveReason = reason == null || reason.isBlank() ? "Refunded by admin" : reason;

        // Request, decision and completion run through the governed lifecycle so there is one
        // implementation of the financial adjustment. Entitlement is always revoked here, which
        // is the legacy endpoint's documented behaviour.
        RefundRequest refund = refundService.requestRefund(
                paymentId, refundedBy, refundable, effectiveReason, null);
        refundService.approve(refund.getId(), refundedBy, effectiveReason, true);
        refundService.completeRefund(refund.getId(), refundedBy, null);

        // Legacy audit entry, kept so anything already reading entityName "Payment Refund"
        // keeps working. The governed lifecycle writes its own richer trail alongside it.
        AuditLog auditLog = new AuditLog();
        auditLog.setInstitutionId(payment.getInstitutionId());
        auditLog.setUserId(refundedBy);
        auditLog.setEntityType("Payment");
        auditLog.setEntityId(paymentId);
        auditLog.setEntityName("Payment Refund");
        auditLog.setAction(AuditLog.AuditAction.UPDATE);
        auditLog.setOldValues(Map.of("status", "COMPLETED", "amount", payment.getAmount().toString()));
        auditLog.setNewValues(Map.of("status", "REFUNDED", "reason", effectiveReason));
        auditLogRepository.save(auditLog);

        Payment refreshed = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException("Payment not found"));
        log.info("Payment refunded: {} by {} reason: {} (amount TZS {})",
                paymentId, refundedBy, effectiveReason, refundable);
        return refreshed;
    }

    /** Machine names for the timeline steps. These are codes the UI maps, not sentences. */
    static final String TIMELINE_CREATED = "CREATED";
    static final String TIMELINE_STATUS = "STATUS";
    static final String TIMELINE_SETTLED = "SETTLED";
    static final String TIMELINE_SOURCE_PAYMENT = "PAYMENT";
    static final String TIMELINE_SOURCE_AUDIT = "AUDIT";
    static final String TIMELINE_ENTITY_TYPE = "Payment";
    static final String TIMELINE_STATUS_KEY = "status";
    static final String STATUS_COMPLETED = "COMPLETED";

    /**
     * The status timeline of one payment, for Payment.md section 29's "status timeline".
     *
     * <p>Assembled only from facts that were written down at the time they happened: the
     * payment row's own timestamps, and the audit rows recorded against this payment. Nothing
     * is interpolated, back-dated or inferred from the current status, so a payment whose
     * history was never recorded returns its creation moment and stops - an honest short
     * timeline rather than a plausible-looking long one. The audit rows are read by entity id
     * only after ownership of the payment has been established, so this cannot be used to
     * probe which payment identifiers exist.</p>
     *
     * <p>{@code paidAt} is a stored fact as well, but the verification that set it also wrote
     * an audit row reporting the same moment; the settlement step is therefore only added when
     * no audit row already records a COMPLETED status, so one settlement never reads as two.</p>
     *
     * @return the steps in chronological order; never null
     */
    @Transactional(readOnly = true)
    public List<TimelineEvent> getPaymentTimeline(UUID paymentId, UUID callerUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException(
                        "Payment not found"));
        requireTimelineAccess(payment, callerUserId, "Payment not found");

        List<TimelineEvent> events = new ArrayList<>();

        if (payment.getCreatedAt() != null) {
            events.add(TimelineEvent.builder()
                    .at(payment.getCreatedAt())
                    .kind(TIMELINE_CREATED)
                    .source(TIMELINE_SOURCE_PAYMENT)
                    .build());
        }

        boolean auditRecordsSettlement = false;
        // The unpaged lookup on purpose: a timeline that stopped at a page boundary would show
        // a payment a partial history, and the paged counterpart of this query is explicitly
        // there for list screens, not for reading one entity's whole record. Soft-deleted rows
        // are dropped here rather than in the query so the read still cannot return them.
        for (AuditLog entry : auditLogRepository
                .findByEntityTypeAndEntityId(TIMELINE_ENTITY_TYPE, paymentId)) {
            if (Boolean.TRUE.equals(entry.getIsDeleted())) {
                continue;
            }
            String fromStatus = recordedStatus(entry.getOldValues());
            String toStatus = recordedStatus(entry.getNewValues());
            if (fromStatus == null && toStatus == null) {
                // The row says nothing about this payment's status, so it is not a step of it.
                continue;
            }
            if (toStatus == null) {
                // A row that only records the status in force at that moment is a snapshot,
                // not a transition from an unknown state.
                toStatus = fromStatus;
                fromStatus = null;
            }
            if (entry.getCreatedAt() == null) {
                continue;
            }
            if (STATUS_COMPLETED.equals(toStatus)) {
                auditRecordsSettlement = true;
            }
            events.add(TimelineEvent.builder()
                    .at(entry.getCreatedAt())
                    .kind(TIMELINE_STATUS)
                    .fromStatus(fromStatus)
                    .toStatus(toStatus)
                    .source(TIMELINE_SOURCE_AUDIT)
                    .build());
        }

        if (payment.getPaidAt() != null && !auditRecordsSettlement) {
            events.add(TimelineEvent.builder()
                    .at(payment.getPaidAt())
                    .kind(TIMELINE_SETTLED)
                    .toStatus(STATUS_COMPLETED)
                    .source(TIMELINE_SOURCE_PAYMENT)
                    .build());
        }

        events.sort(Comparator.comparing(TimelineEvent::getAt));
        return events;
    }

    /** Reads the {@code status} entry an audit row carried, if it carried one. */
    private static String recordedStatus(Map<String, Object> values) {
        if (values == null) {
            return null;
        }
        Object raw = values.get(TIMELINE_STATUS_KEY);
        if (raw == null) {
            return null;
        }
        String value = raw.toString().trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Ownership rule for a payment read, mirroring {@code ReceiptController.requireOwnsOrAdmin}
     * on the same column. Both identifier spaces are compared, because a dashboard payment
     * records the parent's user id while an event ticket records the parent record's own id,
     * and comparing only one of them refuses a payer their own history.
     *
     * <p>Unlike that helper there is no administrator bypass: this serves {@code /v1/my/**},
     * which is fenced to {@code hasRole('PARENT')}, so an administrator never reaches this
     * method and a dead branch would only widen it.</p>
     *
     * <p>A foreign id and an id that was never issued raise the same not-found, so the answer
     * cannot be used to discover which payment ids exist (Payment.md section 45).</p>
     */
    private void requireTimelineAccess(Payment payment, UUID callerUserId, String notFoundMessage) {
        if (callerUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "An authenticated user is required");
        }
        if (callerUserId.equals(payment.getParentId())) {
            return;
        }
        boolean ownsParentRecord = parentRepository.findByUserIdAndIsDeletedFalse(callerUserId)
                .map(parent -> parent.getId().equals(payment.getParentId()))
                .orElse(false);
        if (ownsParentRecord) {
            return;
        }
        log.warn("IDOR blocked: user {} attempted to read the timeline of payment {} owned by {}",
                callerUserId, payment.getId(), payment.getParentId());
        throw new tz.elmkusoma.exception.ResourceNotFoundException(notFoundMessage);
    }

    private PaymentItem toPaymentItem(Payment p) {
        return PaymentItem.builder()
                .id(p.getId().toString())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .description(p.getDescription())
                .serviceType(p.getServiceType())
                .status(p.getStatus())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .providerReference(maskProviderReference(p.getProviderReference()))
                .build();
    }

    /**
     * Gateway reference as it may be handed to a payer (Payment.md §28: "Gateway reference,
     * appropriately masked").
     *
     * <p>A payer has no operational use for the full gateway token. Reconciliation, refund
     * handling and support lookup are the only things that need it, and those are Platform
     * Admin surfaces - so the raw value is never placed on a payer-facing payload in the first
     * place, rather than being hidden after the fact. Only the last four characters survive,
     * which is enough for a support conversation to match a value without disclosing it.</p>
     *
     * <p>Null and blank stay null: a payment that never received a reference has nothing to
     * hide, and turning that into a row of asterisks would read as a real value.</p>
     *
     * <p>Idempotent, so a value that arrives already masked is unchanged rather than being
     * masked down to {@code ****} plus four asterisks. The frontend applies the same rule, so
     * either layer alone is enough.</p>
     */
    static String maskProviderReference(String reference) {
        if (reference == null) {
            return null;
        }
        String value = reference.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() <= 4) {
            return "*".repeat(value.length());
        }
        return "****" + value.substring(value.length() - 4);
    }
}
