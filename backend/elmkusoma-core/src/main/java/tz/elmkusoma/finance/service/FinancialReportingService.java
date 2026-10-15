package tz.elmkusoma.finance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.finance.domain.ReconciliationException;
import tz.elmkusoma.finance.repository.ReconciliationExceptionRepository;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Backend-authoritative financial summaries over genuine transaction records.
 *
 * <p>Every figure here is derived from stored payments. Nothing is estimated, and where the
 * accounting data to compute a figure genuinely does not exist, the figure is reported as
 * <b>unavailable</b> rather than approximated. Profit is the clearest case: platform revenue
 * minus provider shares is not profit, because it ignores processing fees, taxes, refunds,
 * expenses and settlement timing. Publishing that subtraction as a profit figure would be an
 * authoritative-looking number built on an unapproved methodology.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialReportingService {

    private final PaymentRepository paymentRepository;
    private final ReconciliationExceptionRepository exceptionRepository;

    public enum Availability { AVAILABLE, UNAVAILABLE }

    public record Metric(String name, BigDecimal value, String currency, Availability availability,
                         String unavailableReason) {

        public static Metric of(String name, BigDecimal value, String currency) {
            return new Metric(name, value, currency, Availability.AVAILABLE, null);
        }

        public static Metric unavailable(String name, String reason) {
            return new Metric(name, null, null, Availability.UNAVAILABLE, reason);
        }
    }

    public record FinancialSummary(
            Metric confirmedCollections,
            Metric pendingCollections,
            Metric failedCollections,
            Metric refundedAmount,
            Metric netCollections,
            Metric grossSales,
            Metric transactionCount,
            Metric successfulCount,
            Metric failedCount,
            Metric openReconciliationExceptions,
            Metric platformRevenue,
            Metric providerShares,
            Metric profit) {
    }

    private static final String TZS = "TZS";

    @Transactional(readOnly = true)
    public FinancialSummary summaryFor(UUIDRange range) {
        LocalDateTime from = range == null ? null : range.from();
        LocalDateTime to = range == null ? null : range.to();

        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .filter(p -> inRange(p.getPaidAt() != null ? p.getPaidAt() : p.getCreatedAt(), from, to))
                .toList();

        BigDecimal confirmed = sumByStatus(payments, "COMPLETED");
        BigDecimal pending = sumByStatus(payments, "PENDING")
                .add(sumByStatus(payments, "PROCESSING"));
        BigDecimal failed = sumByStatus(payments, "FAILED");
        BigDecimal cancelled = sumByStatus(payments, "CANCELLED");
        BigDecimal refunded = payments.stream()
                .map(p -> p.getRefundedAmount() == null ? BigDecimal.ZERO : p.getRefundedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gross = payments.stream()
                .map(p -> p.getGrossAmount() != null ? p.getGrossAmount()
                        : (p.getAmount() == null ? BigDecimal.ZERO : p.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long successfulCount = countByStatus(payments, "COMPLETED");
        long failedCount = countByStatus(payments, "FAILED") + countByStatus(payments, "CANCELLED");

        long openExceptions = exceptionRepository.countByStatusAndIsDeletedFalse(
                ReconciliationException.STATUS_OPEN);

        return new FinancialSummary(
                Metric.of("confirmedCollections", confirmed, TZS),
                Metric.of("pendingCollections", pending, TZS),
                Metric.of("failedAndCancelledCollections", failed.add(cancelled), TZS),
                Metric.of("refundedAmount", refunded, TZS),
                Metric.of("netCollections", confirmed.subtract(refunded), TZS),
                Metric.of("grossSales", gross, TZS),
                Metric.of("transactionCount", BigDecimal.valueOf(payments.size()), null),
                Metric.of("successfulCount", BigDecimal.valueOf(successfulCount), null),
                Metric.of("failedCount", BigDecimal.valueOf(failedCount), null),
                Metric.of("openReconciliationExceptions", BigDecimal.valueOf(openExceptions), null),
                // Revenue and provider share require an approved, rate-bearing commission policy.
                // Where none exists they are not zero - they are unknown, and reported as such.
                platformRevenue(payments),
                providerShares(payments),
                // Never gross minus provider shares. That subtraction is not profit.
                Metric.unavailable("profit",
                        "Profit requires processing fees, taxes, refunds, expenses and settlement "
                                + "timing, none of which has an approved methodology. Platform revenue "
                                + "minus provider share is an operational estimate, not profit, and is "
                                + "deliberately not published as a figure."));
    }

    private Metric platformRevenue(List<Payment> payments) {
        List<Payment> completed = payments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()) || "REFUNDED".equals(p.getStatus())
                        || "PARTIALLY_REFUNDED".equals(p.getStatus()))
                .toList();
        if (completed.isEmpty()) {
            return Metric.of("platformRevenue", BigDecimal.ZERO, TZS);
        }
        // A single transaction without a resolved allocation makes the total unknowable. Summing
        // the resolved ones and publishing that as "platform revenue" would understate it while
        // looking precise, which is worse than saying it cannot be computed yet.
        long unresolved = completed.stream().filter(p -> !p.hasResolvedAllocation()).count();
        if (unresolved > 0) {
            return Metric.unavailable("platformRevenue",
                    unresolved + " of " + completed.size() + " confirmed transactions have no "
                            + "approved commission allocation (no rate-bearing policy was active "
                            + "when they were confirmed), so a total would mix measured and "
                            + "unknown amounts. Activate a commission policy and resolve them.");
        }
        return Metric.of("platformRevenue", completed.stream()
                .map(p -> p.getCommissionAmount() == null ? BigDecimal.ZERO : p.getCommissionAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add), TZS);
    }

    private Metric providerShares(List<Payment> payments) {
        List<Payment> completed = payments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()) || "REFUNDED".equals(p.getStatus())
                        || "PARTIALLY_REFUNDED".equals(p.getStatus()))
                .toList();
        if (completed.isEmpty()) {
            return Metric.of("providerShares", BigDecimal.ZERO, TZS);
        }
        long unresolved = completed.stream().filter(p -> !p.hasResolvedAllocation()).count();
        if (unresolved > 0) {
            return Metric.unavailable("providerShares",
                    unresolved + " of " + completed.size() + " confirmed transactions have no "
                            + "approved commission allocation, so a provider-share total would be "
                            + "an estimate presented as a fact.");
        }
        return Metric.of("providerShares", completed.stream()
                .map(p -> p.getProviderShareAmount() == null ? BigDecimal.ZERO : p.getProviderShareAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add), TZS);
    }

    private static BigDecimal sumByStatus(List<Payment> payments, String status) {
        return payments.stream()
                .filter(p -> status.equals(p.getStatus()))
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static long countByStatus(List<Payment> payments, String status) {
        return payments.stream().filter(p -> status.equals(p.getStatus())).count();
    }

    private static boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) {
            return false;
        }
        if (from != null && value.isBefore(from)) {
            return false;
        }
        return to == null || !value.isAfter(to);
    }

    /** Half-open range over business dates. */
    public record UUIDRange(LocalDateTime from, LocalDateTime to) {
        public static UUIDRange of(LocalDateTime from, LocalDateTime to) {
            return new UUIDRange(from, to);
        }

        public static UUIDRange lastNDays(int days) {
            LocalDateTime to = LocalDateTime.now().plusDays(1);
            return new UUIDRange(to.minusDays(days), to);
        }
    }
}
