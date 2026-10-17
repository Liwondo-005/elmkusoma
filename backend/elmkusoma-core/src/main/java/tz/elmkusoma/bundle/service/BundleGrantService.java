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
        // bundle cannot restate the period this payer already bought.
        int accessDays = payment.getAccessDays() != null && payment.getAccessDays() > 0
                ? payment.getAccessDays()
                : (bundle.getAccessDays() != null ? bundle.getAccessDays() : 30);

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
     * Grants one item, or leaves an existing grant alone.
     *
     * <p>An entitlement the learner already holds is not re-created and not extended. Whether a
     * re-purchase should top up an existing period is a commercial decision this service is not
     * entitled to make, so it stays a no-op rather than guessing.
     */
    private boolean grantOne(Payment payment, BundleItem item, LocalDateTime now, int accessDays) {
        if (payment.getStudentId() == null) {
            return false;
        }
        boolean alreadyHeld = entitlementRepository
                .existsByStudentIdAndServiceTypeAndServiceIdAndStatusAndIsDeletedFalse(
                        payment.getStudentId(), item.getServiceType(), item.getServiceId(), "ACTIVE");
        if (alreadyHeld) {
            return false;
        }
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