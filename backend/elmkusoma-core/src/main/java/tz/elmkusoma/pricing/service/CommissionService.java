package tz.elmkusoma.pricing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.pricing.domain.CommissionPolicy;
import tz.elmkusoma.pricing.repository.CommissionPolicyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Authoritative commission allocation.
 *
 * <p>The single rule this service exists to enforce: <b>never invent a commission rate</b>.
 * Payment.md forbids a universal percentage, so an allocation is only computed from an
 * {@code ACTIVE} {@link CommissionPolicy} that actually states a rate. When none does, the
 * allocation comes back {@link Allocation#resolved() == false} with the reason attached.</p>
 *
 * <p>That distinction matters more than the arithmetic. "Zero commission" and "commission not
 * yet decided" both look like TZS 0 on a dashboard, but one is a business decision and the other
 * is an unresolved policy. Reporting the second as the first would quietly assert a fact the
 * business has not approved, which is exactly the concealed financial diversion the rules forbid.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionService {

    private final CommissionPolicyRepository commissionPolicyRepository;

    public enum AllocationStatus {
        /** An ACTIVE policy with a rate produced these figures. */
        RESOLVED,
        /** No ACTIVE policy covers this service/provider. */
        NO_ACTIVE_POLICY,
        /** A policy is ACTIVE but states no rate - the business decision is still open. */
        RATE_NOT_APPROVED,
        /** The policy produces a figure greater than the amount being allocated. */
        RATE_EXCEEDS_AMOUNT
    }

    /**
     * The financial breakdown of one transaction.
     *
     * <p>Every field is derived from an approved policy and stamped with the policy version that
     * produced it, so a later policy change can never silently recalculate history.</p>
     */
    public record Allocation(
            AllocationStatus status,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal netAmount,
            BigDecimal commissionAmount,
            BigDecimal providerShare,
            String policyKey,
            Integer policyVersion,
            String feeBearer,
            String reason) {

        public boolean resolved() {
            return AllocationStatus.RESOLVED.equals(status);
        }
    }

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 6;

    /**
     * Allocates the platform commission and provider share for one confirmed transaction.
     *
     * @param serviceType the catalogue service class sold
     * @param providerId  the earning party, when the policy is provider-scoped; may be null
     * @param grossAmount catalogue price before discount
     * @param discountAmount approved discount deducted; never negative
     */
    @Transactional(readOnly = true)
    public Allocation allocate(String serviceType, java.util.UUID providerId,
                               BigDecimal grossAmount, BigDecimal discountAmount) {
        BigDecimal gross = nullSafe(grossAmount);
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        if (discount.signum() < 0) {
            throw new PricingService.PricingConflictException("A discount cannot be negative");
        }
        if (discount.compareTo(gross) > 0) {
            throw new PricingService.PricingConflictException(
                    "Discount TZS " + discount + " exceeds the gross amount TZS " + gross);
        }
        BigDecimal net = gross.subtract(discount);

        CommissionPolicy policy = findApplicablePolicy(serviceType, providerId);

        if (policy == null) {
            return new Allocation(AllocationStatus.NO_ACTIVE_POLICY, gross, discount, net,
                    null, null, null, null, null,
                    "No ACTIVE commission policy covers " + describe(serviceType, providerId)
                            + "; commission is unresolved, not zero");
        }
        if (!policy.hasRate()) {
            // The honest outcome. A rate-less ACTIVE policy is a policy that exists but has not
            // been given a number, and reporting that as a zero commission would invent a term.
            return new Allocation(AllocationStatus.RATE_NOT_APPROVED, gross, discount, net,
                    null, null, policy.getPolicyKey(), policy.getPolicyVersion(),
                    policy.getFeeBearer(),
                    "Commission policy " + policy.getPolicyKey() + " v" + policy.getPolicyVersion()
                            + " states no rate; an authorized decision is required");
        }

        BigDecimal commission = computeCommission(policy, net);
        if (commission == null) {
            return new Allocation(AllocationStatus.RATE_EXCEEDS_AMOUNT, gross, discount, net,
                    null, null, policy.getPolicyKey(), policy.getPolicyVersion(),
                    policy.getFeeBearer(),
                    "Commission exceeds the net amount TZS " + net + "; the policy is inconsistent");
        }
        if (commission.compareTo(net) > 0) {
            return new Allocation(AllocationStatus.RATE_EXCEEDS_AMOUNT, gross, discount, net,
                    commission, null, policy.getPolicyKey(), policy.getPolicyVersion(),
                    policy.getFeeBearer(),
                    "Commission TZS " + commission + " exceeds net TZS " + net);
        }

        BigDecimal providerShare = net.subtract(commission);
        return new Allocation(AllocationStatus.RESOLVED, gross, discount, net,
                commission, providerShare, policy.getPolicyKey(), policy.getPolicyVersion(),
                policy.getFeeBearer(), null);
    }

    private BigDecimal computeCommission(CommissionPolicy policy, BigDecimal net) {
        if (policy.getFixedAmount() != null) {
            return policy.getFixedAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal rate = policy.getCommissionRate();
        if (rate == null) {
            return null;
        }
        if (rate.signum() < 0) {
            throw new PricingService.PricingConflictException("A commission rate cannot be negative");
        }
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            throw new PricingService.PricingConflictException(
                    "A commission rate cannot exceed 100% but was " + rate);
        }
        return net.multiply(rate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private CommissionPolicy findApplicablePolicy(String serviceType, java.util.UUID providerId) {
        String normalised = normalise(serviceType);
        if (normalised != null) {
            CommissionPolicy byService = commissionPolicyRepository
                    .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                            normalised, CommissionPolicy.STATUS_ACTIVE)
                    .orElse(null);
            if (byService != null) {
                return byService;
            }
        }
        if (providerId != null) {
            return commissionPolicyRepository
                    .findFirstByProviderIdAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                            providerId, CommissionPolicy.STATUS_ACTIVE)
                    .orElse(null);
        }
        return null;
    }

    private static String normalise(String serviceType) {
        return serviceType == null || serviceType.isBlank()
                ? null : serviceType.trim().toUpperCase(Locale.ROOT);
    }

    private static String describe(String serviceType, java.util.UUID providerId) {
        if (serviceType != null && !serviceType.isBlank()) {
            return "service type " + serviceType;
        }
        return providerId != null ? "provider " + providerId : "any transaction";
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
