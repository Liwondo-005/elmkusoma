package tz.elmkusoma.pricing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.pricing.domain.DiscountRule;
import tz.elmkusoma.pricing.repository.DiscountRuleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Resolves an approved discount, or none.
 *
 * <p>Only an {@code ACTIVE} rule that is inside its validity window and still has uses
 * remaining is ever applied. A {@code DRAFT} rule is not a discount - it is a proposal - so
 * nothing here can be reached by creating a rule and hoping. When no rule applies, the discount
 * is zero, which is the correct and honest default.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountService {

    private final DiscountRuleRepository discountRuleRepository;

    public record ResolvedDiscount(BigDecimal amount, String policyKey, Integer policyVersion,
                                   String ruleName, boolean applied) {
        public static ResolvedDiscount none() {
            return new ResolvedDiscount(BigDecimal.ZERO, null, null, null, false);
        }
    }

    private static final int MONEY_SCALE = 2;

    /**
     * The discount that applies to a service, or zero.
     *
     * <p>The discount is derived from the catalogue price, never from a caller-supplied figure,
     * so a client cannot name its own discount any more than it can name its own price.</p>
     */
    @Transactional(readOnly = true)
    public ResolvedDiscount resolve(String serviceType, BigDecimal grossAmount) {
        if (serviceType == null || serviceType.isBlank() || grossAmount == null) {
            return ResolvedDiscount.none();
        }
        String normalised = serviceType.trim().toUpperCase(java.util.Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();

        DiscountRule rule = discountRuleRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        normalised, DiscountRule.STATUS_ACTIVE)
                .filter(DiscountRule::isActive)
                .filter(r -> r.isCurrentlyInWindow(now))
                .filter(DiscountRule::hasRemainingUses)
                .orElse(null);

        if (rule == null) {
            return ResolvedDiscount.none();
        }

        BigDecimal discount = compute(rule, grossAmount);
        if (discount == null || discount.signum() <= 0) {
            return ResolvedDiscount.none();
        }
        // A discount can never exceed what is being charged.
        if (discount.compareTo(grossAmount) > 0) {
            discount = grossAmount;
        }
        return new ResolvedDiscount(discount.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                rule.getPolicyKey(), rule.getPolicyVersion(), rule.getName(), true);
    }

    private BigDecimal compute(DiscountRule rule, BigDecimal grossAmount) {
        if (rule.getFixedAmountOff() != null) {
            return rule.getFixedAmountOff();
        }
        BigDecimal percent = rule.getPercentOff();
        if (percent == null) {
            return null;
        }
        if (percent.signum() < 0 || percent.compareTo(new BigDecimal("100")) > 0) {
            throw new PricingService.PricingConflictException(
                    "Discount percent must be between 0 and 100 but was " + percent);
        }
        return grossAmount.multiply(percent)
                .divide(new BigDecimal("100"), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Records one redemption. Called only after a payment is actually confirmed. */
    @Transactional
    public void recordUsage(ResolvedDiscount discount) {
        if (discount == null || !discount.applied()) {
            return;
        }
        // Re-read for the row lock so two concurrent confirmations cannot both take the last use.
        discountRuleRepository.findAll().stream()
                .filter(r -> r.getPolicyKey().equals(discount.policyKey())
                        && r.getPolicyVersion().equals(discount.policyVersion()))
                .findFirst()
                .ifPresent(rule -> {
                    rule.setUsedCount(rule.getUsedCount() == null ? 1 : rule.getUsedCount() + 1);
                    discountRuleRepository.save(rule);
                    log.info("Discount {} v{} used {} time(s)",
                            rule.getPolicyKey(), rule.getPolicyVersion(), rule.getUsedCount());
                });
    }
}
