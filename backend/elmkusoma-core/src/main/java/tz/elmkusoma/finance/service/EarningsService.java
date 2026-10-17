package tz.elmkusoma.finance.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Earnings and revenue-split reporting for an institution's own payments.
 *
 * <p>Scoped to a single institution. A provider or institution administrator sees the money
 * attached to their own organisation's transactions and nothing else; the institution filter is
 * applied in the query itself, so there is no window in which another school's payments are
 * loaded and then discarded.</p>
 *
 * <p>What this reports, and what it refuses to report:</p>
 * <ul>
 *   <li><b>Gross collected</b> - the sum of completed payments, after refunds. Real, because it
 *       is just the stored amount.</li>
 *   <li><b>Platform commission</b> - the sum of {@code commissionAmount} across the payments where
 *       an approved rate actually existed.</li>
 *   <li><b>Provider share</b> - the sum of {@code providerShareAmount}, same population.</li>
 *   <li><b>Unallocated</b> - the count and value of completed payments carrying no resolved
 *       allocation.</li>
 * </ul>
 *
 * <p>No profit figure is produced. Payment.md is explicit that profit must not be derived from
 * gross sales minus provider shares, and the data to do it honestly - processing fees, taxes,
 * operating expenses, a settlement date - does not exist in this system. Producing the number
 * anyway would be the exact fabrication the requirements forbid.</p>
 *
 * <p>Per-teacher and per-host earnings are deliberately absent. A payment records the institution
 * that took the money, not which teacher or host inside it earned the share, so there is nothing
 * truthful to group by. That would need a recorded attribution on the payment, which is a schema
 * change and therefore a separate, approved piece of work - not a guess made here.</p>
 */
@Service
@RequiredArgsConstructor
public class EarningsService {

    private final PaymentRepository paymentRepository;

    /** One figure with its provenance, so a reader can see what is real and what is not. */
    public record Amount(BigDecimal value, String currency, boolean resolved, String note) {

        static Amount resolved(BigDecimal value, String currency) {
            return new Amount(value, currency, true, null);
        }

        static Amount unresolved(BigDecimal value, String currency, String note) {
            return new Amount(value, currency, false, note);
        }
    }

    /**
     * Earnings summary for one institution over a period.
     *
     * @param unallocatedCount completed payments in scope with no resolved allocation
     */
    public record EarningsSummary(
            UUID institutionId,
            LocalDateTime from,
            LocalDateTime to,
            long completedPaymentCount,
            Amount grossCollected,
            Amount refunded,
            Amount platformCommission,
            Amount providerShare,
            Amount unallocatedValue,
            long unallocatedCount,
            String profitabilityNote) {
    }

    @Transactional(readOnly = true)
    public EarningsSummary summaryFor(UUID institutionId, LocalDateTime from, LocalDateTime to) {

        // Everything is filtered in the query, including the institution. Selecting broadly and
        // filtering in Java would still be correct, but it would load another school's financial
        // rows into this process to discard them, which is exactly the habit that turns into an
        // IDOR the moment someone later reuses the method.
        Specification<Payment> scoped = (root, query, cb) -> {
            var where = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            where.add(cb.isFalse(root.get("isDeleted")));
            where.add(cb.equal(root.get("institutionId"), institutionId));
            where.add(cb.equal(root.get("status"), "COMPLETED"));
            where.add(cb.isNotNull(root.get("paidAt")));
            if (from != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("paidAt"), from));
            }
            if (to != null) {
                where.add(cb.lessThanOrEqualTo(root.get("paidAt"), to));
            }
            return cb.and(where.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        var page = paymentRepository.findAll(scoped,
                org.springframework.data.domain.Pageable.unpaged());

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal providerShare = BigDecimal.ZERO;
        BigDecimal unallocated = BigDecimal.ZERO;
        long unallocatedCount = 0;

        for (Payment p : page) {
            gross = gross.add(p.getAmount() == null ? BigDecimal.ZERO : p.getAmount());
            refunded = refunded.add(p.getRefundedAmount() == null
                    ? BigDecimal.ZERO : p.getRefundedAmount());

            // hasResolvedAllocation() is the backend's own test: a commission amount is only
            // counted when an approved, rate-bearing policy produced it. Everything else lands
            // in the unallocated bucket rather than being quietly treated as zero commission,
            // because "no approved rate existed" and "the rate was zero" are different facts.
            if (p.hasResolvedAllocation()) {
                commission = commission.add(p.getCommissionAmount());
                providerShare = providerShare.add(
                        p.getProviderShareAmount() == null
                                ? BigDecimal.ZERO : p.getProviderShareAmount());
            } else {
                unallocated = unallocated.add(p.getAmount() == null
                        ? BigDecimal.ZERO : p.getAmount());
                unallocatedCount++;
            }
        }

        String currency = page.getContent().stream()
                .map(Payment::getCurrency)
                .filter(c -> c != null && !c.isBlank())
                .findFirst()
                .orElse("TZS");

        String note = unallocatedCount > 0
                ? "No approved commission policy existed for " + unallocatedCount
                        + " completed payment(s). Those amounts are counted as unallocated and "
                        + "are deliberately not folded into platform revenue or provider share."
                : null;

        return new EarningsSummary(
                institutionId, from, to, page.getTotalElements(),
                Amount.resolved(gross, currency),
                Amount.resolved(refunded, currency),
                Amount.resolved(commission, currency),
                Amount.resolved(providerShare, currency),
                unallocatedCount > 0
                        ? Amount.unresolved(unallocated, currency, note)
                        : Amount.resolved(unallocated, currency),
                unallocatedCount,
                "Profit is not available. It requires processing fees, taxes, operating expenses "
                        + "and a settlement date, none of which this system records. Gross sales "
                        + "minus provider shares is not an accounting result.");
    }

    /**
     * Earnings for one recorded beneficiary - a teacher, trainer, provider or event organiser.
     *
     * <p>Only possible because V179 records a beneficiary on the payment. Before that, no payment
     * named the person who earned the provider share, so the only statement available was an
     * institution total and any per-persona figure would have had to be inferred from a current
     * profile - fabricating earnings for money that was never attributed to anyone.
     *
     * <p>The beneficiary filter is applied in the query, for the same reason the institution filter
     * is: loading another person's financial rows into this process in order to discard them is the
     * habit that turns into an IDOR the moment someone reuses the method.
     *
     * <p>Only payments with a recorded beneficiary are visible. Historical rows have none and stay
     * unattributed rather than being assigned to whoever holds the role today.
     *
     * <p>The caller must prove the authenticated user owns {@code beneficiaryId}. This method
     * scopes the data; it does not authorise the caller.
     */
    public EarningsSummary summaryForBeneficiary(UUID beneficiaryId,
                                                 LocalDateTime from,
                                                 LocalDateTime to) {
        if (beneficiaryId == null) {
            throw new IllegalArgumentException("A beneficiary id is required");
        }

        Specification<Payment> scoped = (root, query, cb) -> {
            var where = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            where.add(cb.isFalse(root.get("isDeleted")));
            where.add(cb.equal(root.get("beneficiaryId"), beneficiaryId));
            where.add(cb.isNotNull(root.get("beneficiaryId")));
            where.add(cb.equal(root.get("status"), "COMPLETED"));
            where.add(cb.isNotNull(root.get("paidAt")));
            if (from != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("paidAt"), from));
            }
            if (to != null) {
                where.add(cb.lessThanOrEqualTo(root.get("paidAt"), to));
            }
            return cb.and(where.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        var page = paymentRepository.findAll(scoped,
                org.springframework.data.domain.Pageable.unpaged());

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal providerShare = BigDecimal.ZERO;
        BigDecimal unallocated = BigDecimal.ZERO;
        long unallocatedCount = 0;

        for (Payment p : page) {
            gross = gross.add(p.getAmount() == null ? BigDecimal.ZERO : p.getAmount());
            refunded = refunded.add(p.getRefundedAmount() == null
                    ? BigDecimal.ZERO : p.getRefundedAmount());
            if (p.hasResolvedAllocation()) {
                commission = commission.add(p.getCommissionAmount());
                providerShare = providerShare.add(p.getProviderShareAmount() == null
                        ? BigDecimal.ZERO : p.getProviderShareAmount());
            } else {
                unallocated = unallocated.add(p.getAmount() == null
                        ? BigDecimal.ZERO : p.getAmount());
                unallocatedCount++;
            }
        }

        String currency = page.getContent().stream()
                .map(Payment::getCurrency)
                .filter(c -> c != null && !c.isBlank())
                .findFirst()
                .orElse("TZS");

        String note = unallocatedCount > 0
                ? "No approved commission policy existed for " + unallocatedCount
                + " completed payment(s). Those amounts are counted as unallocated and are "
                + "deliberately not folded into platform revenue or provider share."
                : null;

        return new EarningsSummary(
                null, from, to, page.getTotalElements(),
                Amount.resolved(gross, currency),
                Amount.resolved(refunded, currency),
                Amount.resolved(commission, currency),
                Amount.resolved(providerShare, currency),
                unallocatedCount > 0
                        ? Amount.unresolved(unallocated, currency, note)
                        : Amount.resolved(unallocated, currency),
                unallocatedCount,
                "Gross collected, platform commission and the provider share are stated separately. "
                + "Profit is not available: it requires processing fees, taxes, operating expenses "
                + "and a settlement date, none of which this system records.");
    }
}
