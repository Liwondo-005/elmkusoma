package tz.elmkusoma.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.service.ParentPaymentService;
import tz.elmkusoma.subscription.domain.Subscription;
import tz.elmkusoma.subscription.repository.SubscriptionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Creates and maintains the commercial record of a recurring purchase.
 *
 * <p>Two rules shape everything here. First, this service never grants access:
 * that stays with {@link Entitlement}, and it is called only from the verified
 * payment path, never from the request that asked for the payment. Second, it
 * invents no commercial policy. Where a value would be a business decision -
 * above all the billing period - the service either takes the already-approved
 * value snapshotted on the payment, or refuses rather than guessing.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ParentRepository parentRepository;

    /**
     * Opens the subscription for a payment that has already been verified.
     *
     * <p>Idempotent on {@code paymentId}: a repeated confirmation returns the
     * existing record instead of creating a second one, which is what makes a
     * retried webhook safe.</p>
     *
     * @throws IllegalStateException when the payment carries no approved access
     *         duration. That is a deliberate refusal - inventing a default
     *         period here would silently sell something nobody priced.
     */
    @Transactional
    public Subscription activateFromVerifiedPayment(Payment payment, Entitlement entitlement) {
        if (payment == null || !"COMPLETED".equals(payment.getStatus())) {
            throw new IllegalStateException(
                    "A subscription is only opened by a verified, completed payment");
        }
        return subscriptionRepository.findByPaymentIdAndIsDeletedFalse(payment.getId())
                .map(existing -> {
                    log.debug("Subscription already exists for payment {}", payment.getId());
                    return existing;
                })
                .orElseGet(() -> create(payment, entitlement));
    }

    private Subscription create(Payment payment, Entitlement entitlement) {
        Integer accessDays = payment.getAccessDays();
        if (accessDays == null || accessDays <= 0) {
            throw new IllegalStateException(
                    "Payment " + payment.getId() + " carries no approved access duration, so the "
                            + "subscription period is unknown; refusing rather than assuming one");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = resolvePeriodStart(payment, entitlement, now);
        LocalDateTime end = start.plus(Duration.ofDays(accessDays));

        Subscription subscription = Subscription.builder()
                .institutionId(payment.getInstitutionId())
                .studentId(payment.getStudentId())
                .payerId(payment.getParentId())
                .serviceType(payment.getServiceType())
                .serviceId(payment.getServiceId())
                .paymentId(payment.getId())
                .entitlementId(entitlement == null ? null : entitlement.getId())
                .status(Subscription.STATUS_ACTIVE)
                .periodStart(start)
                .periodEnd(end)
                .build();
        Subscription saved = subscriptionRepository.save(subscription);

        audit(saved, "Subscription Activated",
                java.util.Map.of("status", "NONE"),
                java.util.Map.of("status", saved.getStatus(),
                        "periodStart", saved.getPeriodStart().toString(),
                        "periodEnd", saved.getPeriodEnd().toString(),
                        "paymentId", payment.getId().toString()));
        log.info("Subscription activated: id={} payment={} period {} -> {}",
                saved.getId(), payment.getId(), saved.getPeriodStart(), saved.getPeriodEnd());
        return saved;
    }

/**
 * Works out when a newly paid period begins.
 *
 * <p>Approved renewal rule: buying again while a period is still live does not
 * discard the earlier purchase, and does not restart the clock from today. The new
 * period begins where the paid access currently ends, so the two sit back to back
 * and the payer ends up with exactly the time they paid for. Nothing already paid
 * for is shortened.</p>
 *
 * <p>Three cases, none of which guesses:</p>
 * <ul>
 *   <li><b>No live period</b> - a first purchase, or one after a lapse. The period
 *       starts now. A lapse is not back-filled: access genuinely ended, and
 *       inventing the missing days would sell a period nobody paid for.</li>
 *   <li><b>A live period</b> - chain from its {@code periodEnd}. This is the
 *       renewal case.</li>
 *   <li><b>A period that has run out but still reads ACTIVE</b> - start now. The
 *       sweep may not have run; the elapsed test, not the status label, decides
 *       whether access is still live.</li>
 * </ul>
 *
 * <p>Prior rows are never modified. A renewal adds a record; it does not rewrite a
 * record someone already paid for.</p>
 */
private LocalDateTime resolvePeriodStart(Payment payment, Entitlement entitlement, LocalDateTime now) {
        // Lock the payer FIRST, before reading any subscription. A payer's first purchase has no
        // subscription row to lock, so without this two concurrent first purchases both read
        // "no current period" and both insert. Taking the stable parent row first makes the loser
        // wait, re-read, and chain onto the winner's period. Parent is always locked before
        // subscription, so the lock order is consistent and cannot deadlock.
        if (payment.getParentId() != null) {
            parentRepository.findByIdForUpdate(payment.getParentId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Payment " + payment.getId() + " names payer " + payment.getParentId()
                                    + ", which does not exist; refusing to open a subscription for an "
                                    + "unknown payer"));
        }

        List<Subscription> active = subscriptionRepository.findActivePeriodsForPayerAndServiceForUpdate(
                payment.getParentId(), payment.getServiceType(),
                payment.getServiceId(), new UUID(0L, 0L));

        if (active.isEmpty()) {
            return entitlement != null && entitlement.getStartsAt() != null
                    ? entitlement.getStartsAt()
                    : now;
        }

        Subscription current = active.get(0);
        if (current.isPeriodElapsed(now)) {
            log.debug("Latest period for payer {} ended at {}; new period starts now",
                    payment.getParentId(), current.getPeriodEnd());
            return now;
        }

        log.info("Renewal for payer {} chains after the live period ending {}",
                payment.getParentId(), current.getPeriodEnd());
        return current.getPeriodEnd();
    }

    /**
     * Cancels a subscription under the approved policy BR-01.
     *
     * <p>Cancellation prevents the next renewal. It does NOT revoke access immediately and does
     * NOT trigger a refund: access is decided from the paid period's boundary rather than from
     * this status, so a cancelled subscription whose period has not ended still grants access
     * until it does, which is exactly what the payer paid for. A refund is a separate, explicitly
     * requested operation.
     *
     * <p>The effective date is therefore the end of the already-paid period. That is a decision,
     * not a default: "immediately" would cut off access that was paid for, and a fixed offset
     * would invent a commercial term nobody approved.
     *
     * <p>Idempotent. A repeated request returns the same subscription with the same effective
     * date rather than failing - refusing the second attempt would tell the payer their
     * cancellation had failed when it had already succeeded.
     *
     * <p>Only this period is affected. A later chained period is a separate row and is untouched.
     */
    @Transactional
    public Subscription cancelAtPeriodEnd(UUID subscriptionId, String reason) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException(
                        "Subscription", "id", subscriptionId));

        String previousStatus = subscription.getStatus();
        LocalDateTime now = LocalDateTime.now();

        if (subscription.isPeriodElapsed(now)) {
            // Nothing left to cancel: the paid period has already run out, so recording a
            // cancellation now would assert an event that never affected access or money.
            throw new IllegalStateException(
                    "This subscription's paid period already ended at " + subscription.getPeriodEnd()
                            + ", so there is nothing left to cancel");
        }

        subscription.requestCancellation(subscription.getPeriodEnd(), reason);
        Subscription saved = subscriptionRepository.save(subscription);

        if (!Subscription.STATUS_CANCELLED.equals(previousStatus)) {
            audit(saved, "Subscription Cancelled",
                    java.util.Map.of("status", previousStatus),
                    java.util.Map.of("status", saved.getStatus(),
                            "cancelledAt", String.valueOf(saved.getCancelledAt()),
                            "periodEnd", String.valueOf(saved.getPeriodEnd()),
                            "reason", reason == null ? "" : reason));
            log.info("Subscription {} cancelled effective {} (its period end); access continues "
                    + "until then and no refund was issued", saved.getId(), saved.getCancelledAt());
        }
        return saved;
    }

    /**
     * Cancels a subscription.
     *
     * <p>{@code effectiveAt} must come from the approved cancellation policy.
     * This method deliberately has no default: choosing "immediately" would cut
     * off access the payer has already paid for, and choosing "at period end"
     * would keep charging someone who asked to stop.</p>
     */
    @Transactional
    public Subscription cancel(UUID subscriptionId, LocalDateTime effectiveAt, String reason) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException(
                        "Subscription", "id", subscriptionId));
        subscription.requestCancellation(effectiveAt, reason);
        Subscription saved = subscriptionRepository.save(subscription);
        audit(saved, "Subscription Cancelled",
                java.util.Map.of("status", Subscription.STATUS_ACTIVE),
                java.util.Map.of("status", saved.getStatus(),
                        "cancelledAt", String.valueOf(saved.getCancelledAt())));
        return saved;
    }

    /**
     * Marks elapsed ACTIVE subscriptions as EXPIRED.
     *
     * <p>Reporting bookkeeping only. Whether access has lapsed is decided when
     * access is read, so a missed or delayed sweep can never leave a lapsed
     * subscription looking current - it can only leave the status field stale for
     * a report, which is exactly the kind of lag that is safe.</p>
     */
    @Transactional
    public int expireElapsed(LocalDateTime now) {
        List<Subscription> elapsed = subscriptionRepository
                .findAllByStatusAndPeriodEndLessThanEqualAndIsDeletedFalse(
                        Subscription.STATUS_ACTIVE, now);
        for (Subscription subscription : elapsed) {
            if (!subscription.canTransitionTo(Subscription.STATUS_EXPIRED)) {
                continue;
            }
            subscription.setStatus(Subscription.STATUS_EXPIRED);
            subscriptionRepository.save(subscription);
            audit(subscription, "Subscription Expired",
                    java.util.Map.of("status", Subscription.STATUS_ACTIVE),
                    java.util.Map.of("status", Subscription.STATUS_EXPIRED,
                            "periodEnd", String.valueOf(subscription.getPeriodEnd())));
        }
        if (!elapsed.isEmpty()) {
            log.info("Marked {} elapsed subscription(s) as EXPIRED", elapsed.size());
        }
        return elapsed.size();
    }

    private void audit(Subscription subscription, String entityName,
                       java.util.Map<String, Object> oldValues,
                       java.util.Map<String, Object> newValues) {
        AuditLog auditLog = new AuditLog();
        auditLog.setInstitutionId(subscription.getInstitutionId());
        auditLog.setUserId(null);
        auditLog.setUserEmail(ParentPaymentService.WEBHOOK_SYSTEM_ACTOR);
        auditLog.setUserRole("SYSTEM");
        auditLog.setEntityType("Subscription");
        auditLog.setEntityId(subscription.getId());
        auditLog.setEntityName(entityName);
        auditLog.setAction(AuditLog.AuditAction.UPDATE);
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);
        auditLogRepository.save(auditLog);
    }
}