package tz.elmkusoma.bundle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.bundle.domain.Bundle;
import tz.elmkusoma.bundle.domain.BundleItem;
import tz.elmkusoma.bundle.repository.BundleItemRepository;
import tz.elmkusoma.bundle.repository.BundleRepository;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.EntitlementRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Grants the services inside a bundle once its payment is genuinely verified.
 *
 * <p>Called only from {@code ParentPaymentService.verifyPayment}, the single authoritative
 * COMPLETED transition. Routing bundles through the same place subscriptions and event seats use
 * is what makes duplicate-callback idempotency inherited rather than reimplemented: the payment
 * reaches COMPLETED once, so the bundle grants once.
 *
 * <p>The grant reuses the existing {@link Entitlement} table rather than introducing a parallel
 * access system. Entitlements already key on (student, service, serviceId) and are what every
 * access check in the application already reads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BundleGrantService {

    private final BundleRepository bundleRepository;
    private final BundleItemRepository bundleItemRepository;
    private final EntitlementRepository entitlementRepository;

    /**
     * Test seam, invoked in the window between the coverage check and the insert. Package-private
     * on purpose: a public setter would let anything reconfigure grant behaviour at runtime.
     */
    private volatile java.util.function.BiConsumer<UUID, BundleItem> beforeInsert =
            (student, item) -> { };

    void setBeforeInsertHook(java.util.function.BiConsumer<UUID, BundleItem> hook) {
        this.beforeInsert = hook == null ? (student, item) -> { } : hook;
    }

    /**
     * Grants every item in the bundle this payment bought.
     *
     * <p>A payment whose service type is not a bundle is ignored, so this is safe to call for every
     * payment without the caller having to know which ones are bundles.
     *
     * @return how many entitlements were newly created; already-held items are not re-created
     */
    @Transactional
    public int grantFor(Payment payment) {
        if (payment == null || !"COMPLETED".equals(payment.getStatus())) {
            return 0;
        }
        Bundle bundle = bundleRepository
                .findByServiceTypeAndIsDeletedFalse(payment.getServiceType())
                .orElse(null);
        if (bundle == null) {
            return 0;
        }

        // Publication is a sale rule, not a display filter. Hiding a draft or suspended bundle from
        // a listing is presentation; refusing to grant access is the rule. Without this check a
        // payment raised against a suspended bundle would still deliver every item in it.
        if (!bundle.isPurchasable()) {
            log.warn("Bundle {} ({}) is {} and must not grant access", bundle.getId(),
                    bundle.getServiceType(), bundle.getStatus());
            return 0;
        }

        List<BundleItem> items = bundleItemRepository
                .findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(bundle.getId());
        if (items.isEmpty()) {
            // Published but empty. A published bundle always has at least one item, but a
            // suspension or a bad edit could produce one; granting nothing is safer than
            // guessing what the missing content was.
            log.warn("Bundle {} ({}) has no items; a verified payment granted no access",
                    bundle.getId(), bundle.getServiceType());
            return 0;
        }

        // The duration is snapshotted on the payment at initiation, so a later edit to the
        // bundle cannot restate the period this payer already bought. The bundle's own authored
        // duration is real recorded data and is an acceptable second source, but a literal default
        // is not: inventing "30 days" would silently promise access nobody sold and nobody paid
        // for, and it would differ per environment depending on which branch ran first. If neither
        // source exists the payment is refused instead, because granting access of an unknown
        // length is not a smaller mistake than granting none.
        Integer snapshot = payment.getAccessDays();
        int accessDays = snapshot != null && snapshot > 0
                ? snapshot
                : (bundle.getAccessDays() != null ? bundle.getAccessDays() : 0);
        if (accessDays <= 0) {
            log.error("Bundle {} ({}) has no recorded access duration and payment {} has none "
                            + "snapshotted; no access was granted",
                    bundle.getId(), bundle.getServiceType(), payment.getId());
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        int granted = 0;
        for (BundleItem item : items) {
            if (grantOne(payment, item, now, accessDays)) {
                granted++;
            }
        }
        return granted;
    }

    /**
     * Grants one item, or leaves a still-running grant alone.
     *
     * <p>Whether the learner already holds this item is decided by <em>time</em>, not by the stored
     * status string. An entitlement row keeps status {@code ACTIVE} until something flips it, so
     * reading status alone told this method that an entitlement which expired months ago was still
     * held and refused the grant - while the quote side, which does read the timestamp, had already
     * charged the parent for it. The two sides now agree on the same predicate the rest of the
     * platform uses ({@code ParentPaymentService.isCurrentlyValid}).
     *
     * <p>Under the approved expiry rule:
     * <ul>
     *   <li>a period that has not elapsed is still held - a repurchase that lands while it runs is
     *       neither re-created nor extended;</li>
     *   <li>a period that has elapsed is closed out as {@code EXPIRED} so the previous period stays
     *       auditable, then a new period begins now;</li>
     *   <li>there is no grace period: the new period starts at the moment of repurchase, never
     *       backdated to when the old one lapsed;</li>
     *   <li>a row that never expires cannot elapse and therefore still blocks a re-grant.</li>
     * </ul>
     *
     * <p>Only reached for a payment that has already reached COMPLETED, so a pending, failed or
     * unverified payment can never open a period here.
     */
    private boolean grantOne(Payment payment, BundleItem item, LocalDateTime now, int accessDays) {
        if (payment.getStudentId() == null) {
            return false;
        }
        // Taken before the coverage check, and inside the same transaction as the insert below.
        // Ordering matters: a lock acquired after the check would let both transactions read
        // "not held" before either held anything, which is the race this exists to close.
        entitlementRepository.lockGrantKey(
                payment.getStudentId() + ":" + item.getServiceType() + ":" + item.getServiceId());

        List<Entitlement> existing = entitlementRepository
                .findAllByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                        payment.getStudentId(), item.getServiceType(), item.getServiceId());

        boolean stillHeld = existing.stream().anyMatch(e -> isActive(e) && isUnexpired(e, now));
        if (stillHeld) {
            return false;
        }

        // Close out an elapsed period before opening the new one, so the old row is never left
        // claiming ACTIVE while its timestamp says otherwise and the history of what was bought
        // remains readable. Rows already EXPIRED or REVOKED are left exactly as they are.
        boolean closedOut = false;
        for (Entitlement entitlement : existing) {
            if (isActive(entitlement) && !isUnexpired(entitlement, now)) {
                entitlement.setStatus("EXPIRED");
                entitlementRepository.save(entitlement);
                closedOut = true;
            }
        }
        if (closedOut) {
            log.info("Closed out an elapsed period for student {} service {}:{} before granting "
                            + "the repurchased one",
                    payment.getStudentId(), item.getServiceType(), item.getServiceId());
        }

        // Seam for the concurrency test. It sits in the exact window the race lives in - after the
        // coverage check, before the insert - so a test can hold both transactions inside the
        // window and make the interleaving deterministic instead of hoping timing lines up. It is
        // package-private and defaults to doing nothing, and no production path can reach it.
        beforeInsert.accept(payment.getStudentId(), item);
        entitlementRepository.save(Entitlement.builder()
                .userId(payment.getPurchaserId() != null
                        ? payment.getPurchaserId() : payment.getParentId())
                .studentId(payment.getStudentId())
                .institutionId(payment.getInstitutionId())
                .serviceType(item.getServiceType())
                .serviceId(item.getServiceId())
                .paymentId(payment.getId())
                .status("ACTIVE")
                .startsAt(now)
                .expiresAt(now.plus(Duration.ofDays(accessDays)))
                .build());
        return true;
    }

    private static boolean isActive(Entitlement entitlement) {
        return "ACTIVE".equals(entitlement.getStatus());
    }

    /**
     * Whether an entitlement's period has not elapsed. A null expiry means the row records no end,
     * so nothing has passed and the period still holds.
     */
    private static boolean isUnexpired(Entitlement entitlement, LocalDateTime now) {
        return entitlement.getExpiresAt() == null || entitlement.getExpiresAt().isAfter(now);
    }

    /** Whether a service type currently corresponds to a bundle. Used by discovery endpoints. */
    @Transactional(readOnly = true)
    public boolean isBundleServiceType(String serviceType) {
        return bundleRepository.findByServiceTypeAndIsDeletedFalse(serviceType).isPresent();
    }

    /** Items of a bundle, for display. Never grants anything. */
    @Transactional(readOnly = true)
    public List<BundleItem> itemsOf(UUID bundleId) {
        return bundleItemRepository.findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(bundleId);
    }
}