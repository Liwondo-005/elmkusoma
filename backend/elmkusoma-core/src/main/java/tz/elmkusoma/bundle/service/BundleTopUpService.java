package tz.elmkusoma.bundle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.bundle.domain.Bundle;
import tz.elmkusoma.bundle.domain.BundleItem;
import tz.elmkusoma.bundle.repository.BundleItemRepository;
import tz.elmkusoma.bundle.repository.BundleRepository;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.pricing.service.PricingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BR-03: what a learner actually pays when they buy a bundle they partly already hold.
 *
 * <p>The rule is narrow on purpose: a service covered by a <b>currently valid</b> entitlement is
 * not charged again. That is the whole of "do not double-charge", and everything else follows
 * from taking "currently valid" seriously rather than treating any matching row as a match.
 *
 * <p>Each remaining item is priced individually through
 * {@link PricingService#resolvePrice}, which reads the ACTIVE approved policy. No bundle-level
 * price is applied and no discount is spread across items: allocating a bundle discount between
 * components is a commercial decision that has not been approved, and inventing one would produce
 * a total nobody agreed to.
 *
 * <p>What this deliberately does not decide, and documents instead:
 * <ul>
 *   <li><b>Expired entitlements.</b> An expired row is not coverage. The service is chargeable
 *       again, which is the straightforward reading, but the business may intend a grace period;
 *       that would need an approved rule.</li>
 *   <li><b>Incompatible access levels.</b> Two different service types are never treated as
 *       covering each other, even if they grant similar access. Service identity is
 *       (serviceType, serviceId) and nothing else.</li>
 * </ul>
 *
 * <p>Quote and purchase are separated. A quote is a snapshot for display; the charge is decided at
 * purchase time, and the caller must be told if the price moved rather than being silently
 * charged a different figure.
 */
@Service
@RequiredArgsConstructor
public class BundleTopUpService {

    private final BundleRepository bundleRepository;
    private final BundleItemRepository bundleItemRepository;
    private final EntitlementRepository entitlementRepository;
    private final PricingService pricingService;

    /**
     * One item in the breakdown.
     *
     * @param covered  true when a valid entitlement already grants this service
     * @param price    the approved price, or null when the item is already covered
     */
    public record TopUpLine(BundleItem item,
                            boolean covered,
                            BigDecimal price,
                            String currency,
                            String policyKey,
                            Integer policyVersion) {
    }

    /**
     * The full picture for a buyer.
     *
     * @param total          what they would be charged for the items not already held
     * @param fullyCovered   true when nothing needs charging
     */
    public record TopUpQuote(List<TopUpLine> lines,
                             BigDecimal total,
                             String currency,
                             boolean fullyCovered,
                             int chargeableCount,
                             int coveredCount) {
    }

    /**
     * Prices a bundle for a learner, charging only for what they do not already hold.
     *
     * @throws IllegalStateException when the bundle is not published, or an item has no approved
     *         price - a price is never invented to make a total work out
     */
    @Transactional
    public TopUpQuote quote(UUID bundleId, UUID studentId, LocalDateTime now) {
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException(
                        "Bundle", "id", bundleId));
        if (!bundle.isPurchasable()) {
            throw new IllegalStateException(
                    "This bundle is " + bundle.getStatus() + " and is not available to buy");
        }

        List<BundleItem> items = bundleItemRepository
                .findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(bundleId);
        if (items.isEmpty()) {
            throw new IllegalStateException("This bundle contains no services");
        }

        List<TopUpLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String currency = null;
        int chargeable = 0;
        int covered = 0;

        for (BundleItem item : items) {
            if (holdsValidAccess(studentId, item, now)) {
                // Already covered. Not charged, and not re-priced: there is nothing to buy.
                lines.add(new TopUpLine(item, true, null, null, null, null));
                covered++;
                continue;
            }
            PricingService.ResolvedPrice resolved =
                    pricingService.resolvePrice(item.getServiceType(), null);
            lines.add(new TopUpLine(item, false, resolved.amount(), resolved.currency(),
                    resolved.policyKey(), resolved.policyVersion()));
            total = total.add(resolved.amount());
            currency = currency == null ? resolved.currency() : currency;
            chargeable++;
        }

        return new TopUpQuote(List.copyOf(lines), total, currency, chargeable == 0,
                chargeable, covered);
    }

    /**
     * Whether a valid entitlement already grants this exact service.
     *
     * <p>Identity is (studentId, serviceType, serviceId). A different service type is not
     * coverage however similar the access, and a row that is not ACTIVE, has not started, or has
     * already elapsed is not coverage either - only a currently valid grant suppresses a charge.
     */
    private boolean holdsValidAccess(UUID studentId, BundleItem item, LocalDateTime now) {
        if (studentId == null) {
            return false;
        }
        return entitlementRepository
                .findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                        studentId, item.getServiceType(), item.getServiceId())
                .filter(this::currentlyValid)
                .isPresent();
    }

    private boolean currentlyValid(Entitlement entitlement) {
        LocalDateTime now = LocalDateTime.now();
        if (!"ACTIVE".equals(entitlement.getStatus())) {
            return false;
        }
        if (entitlement.getStartsAt() != null && entitlement.getStartsAt().isAfter(now)) {
            return false;
        }
        // An elapsed entitlement is not coverage. Whether a grace period should apply is an
        // unapproved commercial question, so this takes the straightforward reading and records
        // it rather than inventing a grace window.
        return entitlement.getExpiresAt() == null || entitlement.getExpiresAt().isAfter(now);
    }
}