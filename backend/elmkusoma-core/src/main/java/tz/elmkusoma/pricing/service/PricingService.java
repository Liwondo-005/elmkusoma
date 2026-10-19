package tz.elmkusoma.pricing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.pricing.domain.PricingPolicy;
import tz.elmkusoma.pricing.repository.PricingPolicyRepository;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * The single place a chargeable amount is decided.
 *
 * <p>Every price the platform collects is resolved here from an ACTIVE {@link PricingPolicy}
 * row. Nothing else in the payment path may invent, accept or adjust an amount: a client that
 * can name its own price can buy a TZS 50,000 course for TZS 1, and the gateway webhook's
 * amount reconciliation does not help, because it only compares the gateway's figure against the
 * already-tainted stored record.</p>
 *
 * <p>Two deliberate refusals, both of which refuse rather than guess:</p>
 * <ul>
 *   <li><b>No ACTIVE policy</b> - pricing is not configured, so nothing is charged. Policies are
 *       born DRAFT and require authorized activation; trusting the caller while waiting for that
 *       approval would defeat the approval gate entirely.</li>
 *   <li><b>Price outside its education band</b> - reported as a conflict, never silently clamped
 *       into range. Clamping would rewrite the provider's price without anyone deciding to, and
 *       the receipt would then disagree with what was agreed.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final PricingPolicyRepository pricingPolicyRepository;

    /** A price the platform is willing to collect, and the policy version it came from. */
    public record ResolvedPrice(BigDecimal amount, String currency, Integer policyVersion,
                                String policyKey, Integer accessDays) {
    }

    /**
     * Raised when a proposed price conflicts with an approved policy. Never auto-corrected.
     *
     * <p>Extends {@link IllegalStateException} on purpose: {@code GlobalExceptionHandler} already
     * maps that to HTTP 409 CONFLICT, so a caller that sends a disagreeing amount gets a 409 with
     * the policy version in the message rather than falling through to the generic handler's 500,
     * which would tell Developer 02 nothing and look like a server fault.</p>
     */
    public static class PricingConflictException extends IllegalStateException {
        public PricingConflictException(String message) {
            super(message);
        }
    }

    /**
     * Raised when no ACTIVE policy covers the service, so no authoritative price exists.
     *
     * <p>Also a 409 rather than a 500: the platform is refusing to charge, which is deliberate
     * and recoverable by activating a policy, not a crash.</p>
     */
    public static class PricingNotConfiguredException extends IllegalStateException {
        public PricingNotConfiguredException(String message) {
            super(message);
        }
    }

    /**
     * The authoritative amount to charge for a service, or a refusal.
     *
     * @param serviceType   the catalogue service class being bought
     * @param educationLevel the learner's level, used to pick the applicable band; may be null
     *                       for services that bands do not govern (certificate administration,
     *                       event categories priced by their own policy)
     */
    @Transactional(readOnly = true)
    public ResolvedPrice resolvePrice(String serviceType, EducationLevel educationLevel) {
        if (serviceType == null || serviceType.isBlank()) {
            throw new PricingNotConfiguredException("A service type is required to price a purchase");
        }
        String normalised = serviceType.trim().toUpperCase(Locale.ROOT);

        PricingPolicy price = pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        normalised, PricingPolicy.STATUS_ACTIVE)
                .orElseThrow(() -> new PricingNotConfiguredException(
                        "Pricing is not configured for service type " + normalised
                                + "; an authorized ACTIVE pricing policy is required before charging"));

        if (price.getDefaultAmount() == null) {
            throw new PricingNotConfiguredException(
                    "Active pricing policy " + price.getPolicyKey() + " v" + price.getPolicyVersion()
                            + " does not define a default amount for " + normalised);
        }
        if (price.getDefaultAmount().signum() < 0) {
            throw new PricingConflictException(
                    "Active pricing policy defines a negative amount for " + normalised);
        }

        if (educationLevel != null && isBandGoverned(normalised)) {
            validateAgainstBand(price, educationLevel);
        }

        return new ResolvedPrice(price.getDefaultAmount(), price.getCurrency(),
                price.getPolicyVersion(), price.getPolicyKey(), price.getAccessDays());
    }

    /**
     * Service types whose price is constrained by an education-level band.
     *
     * <p>Only the provider catalogue - resources, courses, modules, live classes and replays -
     * is priced per level. Access passes and subscription tiers are platform-wide fixed prices
     * (Premium is TZS 7,000 for every learner), so validating them against the Primary band of
     * TZS 500-3,000 would reject a perfectly legitimate subscription as a conflict.</p>
     */
    private static final java.util.Set<String> BAND_GOVERNED = java.util.Set.of(
            "LEARNING_RESOURCE", "LEARNING_RESOURCES", "RESOURCE",
            "ONLINE_COURSE", "COURSE",
            "VOCATIONAL_MODULE", "MODULE", "ACADEMIC_MODULE",
            "LIVE_CLASS", "LIVE_SESSION", "LIVE_EVENT",
            "RECORDED_LESSON", "REPLAY");

    static boolean isBandGoverned(String normalisedServiceType) {
        return BAND_GOVERNED.contains(normalisedServiceType);
    }

    /**
     * Confirms a resolved price sits inside the band approved for its education level.
     *
     * <p>Only levels that have an ACTIVE band are checked; a level with no band is not evidence
     * of approval either way, so it is left to the service-type policy. A conflict throws rather
     * than rewriting the amount, because the correction has to be a human decision between the
     * provider and the Platform Admin.</p>
     */
    void validateAgainstBand(PricingPolicy price, EducationLevel educationLevel) {
        PricingPolicy band = pricingPolicyRepository
                .findFirstByEducationLevelAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        educationLevel.name(), PricingPolicy.STATUS_ACTIVE)
                .orElse(null);
        if (band == null || band.getMinAmount() == null || band.getMaxAmount() == null) {
            return;
        }
        BigDecimal amount = price.getDefaultAmount();
        if (amount.compareTo(band.getMinAmount()) < 0 || amount.compareTo(band.getMaxAmount()) > 0) {
            throw new PricingConflictException(
                    "Price TZS " + amount + " for " + price.getServiceType()
                            + " falls outside the approved " + educationLevel + " band of TZS "
                            + band.getMinAmount() + " - TZS " + band.getMaxAmount()
                            + " (policy " + band.getPolicyKey() + " v" + band.getPolicyVersion()
                            + "). An authorized policy decision is required.");
        }
    }

    /**
     * Compares a caller-supplied amount against the authoritative one.
     *
     * <p>Kept separate from {@link #resolvePrice} so a controller can reject a mismatch with a
     * precise message instead of a generic pricing failure, while never treating the caller's
     * figure as the price.</p>
     */
    public void requireAmountMatches(BigDecimal clientAmount, ResolvedPrice resolved) {
        if (clientAmount == null) {
            return;
        }
        if (clientAmount.compareTo(resolved.amount()) != 0) {
            throw new PricingConflictException(
                    "Amount TZS " + clientAmount + " does not match the authoritative price of TZS "
                            + resolved.amount() + " from policy " + resolved.policyKey()
                            + " v" + resolved.policyVersion());
        }
    }

    /**
     * Same check, against an amount that was assembled server-side rather than read off one policy.
     *
     * <p>A bundle sold to a learner who already holds part of it is priced from several item
     * policies, so there is no single {@link ResolvedPrice} to compare against. The property that
     * matters is unchanged and is the one this enforces: the caller may not name the figure. What
     * differs is only where the authoritative number came from, and it is named in the message so
     * a mismatch can be traced to a decision rather than guessed at.
     */
    public void requireAmountMatches(BigDecimal clientAmount, BigDecimal authoritativeAmount,
                                     String authoritativeSource) {
        if (clientAmount == null) {
            return;
        }
        if (clientAmount.compareTo(authoritativeAmount) != 0) {
            throw new PricingConflictException(
                    "Amount TZS " + clientAmount + " does not match the authoritative amount of TZS "
                            + authoritativeAmount + " from " + authoritativeSource);
        }
    }
}
