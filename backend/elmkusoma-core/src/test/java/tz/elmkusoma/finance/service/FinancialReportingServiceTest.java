package tz.elmkusoma.finance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.finance.repository.ReconciliationExceptionRepository;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialReportingServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private ReconciliationExceptionRepository exceptionRepository;
    @InjectMocks private FinancialReportingService financialReportingService;

    private Payment completed(BigDecimal amount, BigDecimal commission, BigDecimal providerShare) {
        return Payment.builder()
                .id(java.util.UUID.randomUUID())
                .status("COMPLETED")
                .amount(amount)
                .grossAmount(amount)
                .discountAmount(BigDecimal.ZERO)
                .netAmount(amount)
                .commissionAmount(commission)
                .providerShareAmount(providerShare)
                .commissionPolicyKey(commission == null ? null : "PLATFORM_COMMISSION")
                .paidAt(LocalDateTime.now())
                .build();
    }

    @Test
    void profitIsAlwaysUnavailable_neverGrossMinusProviderShares() {
        when(paymentRepository.findAll()).thenReturn(List.of(
                completed(new BigDecimal("10000"), new BigDecimal("1500"), new BigDecimal("8500"))));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        var summary = financialReportingService.summaryFor(
                FinancialReportingService.UUIDRange.lastNDays(30));

        // The one figure Payment.md forbids approximating is always refused, with a reason.
        assertEquals(FinancialReportingService.Availability.UNAVAILABLE,
                summary.profit().availability());
        assertNull(summary.profit().value());
        assertTrue(summary.profit().unavailableReason().contains("not profit"));
    }

    @Test
    void platformRevenueIsUnavailable_whenAnyConfirmedTransactionLacksAnAllocation() {
        // Summing only the resolved ones would understate revenue while looking precise.
        Payment resolved = completed(new BigDecimal("10000"), new BigDecimal("1500"),
                new BigDecimal("8500"));
        Payment unresolved = completed(new BigDecimal("5000"), null, null);

        when(paymentRepository.findAll()).thenReturn(List.of(resolved, unresolved));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        var summary = financialReportingService.summaryFor(null);

        assertEquals(FinancialReportingService.Availability.UNAVAILABLE,
                summary.platformRevenue().availability());
        assertEquals(FinancialReportingService.Availability.UNAVAILABLE,
                summary.providerShares().availability());
        assertTrue(summary.platformRevenue().unavailableReason().contains("commission"));
    }

    @Test
    void platformRevenueIsAvailable_whenEveryTransactionHasAResolvedAllocation() {
        when(paymentRepository.findAll()).thenReturn(List.of(
                completed(new BigDecimal("10000"), new BigDecimal("1500"), new BigDecimal("8500")),
                completed(new BigDecimal("2000"), new BigDecimal("300"), new BigDecimal("1700"))));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        var summary = financialReportingService.summaryFor(null);

        assertEquals(FinancialReportingService.Availability.AVAILABLE,
                summary.platformRevenue().availability());
        assertEquals(0, new BigDecimal("1800.00").compareTo(summary.platformRevenue().value()));
        assertEquals(0, new BigDecimal("10200.00").compareTo(summary.providerShares().value()));
    }

    @Test
    void confirmedCollectionsSumCompletedPaymentsOnly() {
        Payment done = completed(new BigDecimal("50000"), new BigDecimal("1"), BigDecimal.ONE);
        done.setCommissionStatus("RESOLVED");
        Payment pending = completed(new BigDecimal("30000"), null, null);
        pending.setStatus("PENDING");

        when(paymentRepository.findAll()).thenReturn(List.of(done, pending));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        var summary = financialReportingService.summaryFor(null);

        assertEquals(0, new BigDecimal("50000").compareTo(summary.confirmedCollections().value()));
        assertEquals(0, new BigDecimal("30000").compareTo(summary.pendingCollections().value()));
        assertEquals(2, summary.transactionCount().value().intValue());
    }

    @Test
    void netCollectionsDeductRefunds() {
        Payment done = completed(new BigDecimal("50000"), BigDecimal.ONE, BigDecimal.ONE);
        done.setCommissionStatus("RESOLVED");
        done.setRefundedAmount(new BigDecimal("15000"));

        when(paymentRepository.findAll()).thenReturn(List.of(done));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        var summary = financialReportingService.summaryFor(null);

        assertEquals(0, new BigDecimal("50000").compareTo(summary.confirmedCollections().value()));
        assertEquals(0, new BigDecimal("15000").compareTo(summary.refundedAmount().value()));
        assertEquals(0, new BigDecimal("35000").compareTo(summary.netCollections().value()));
    }

    @Test
    void deletedPaymentsAreExcludedFromEveryFigure() {
        Payment deleted = completed(new BigDecimal("99999"), BigDecimal.ONE, BigDecimal.ONE);
        deleted.setCommissionStatus("RESOLVED");
        deleted.setIsDeleted(true);

        when(paymentRepository.findAll()).thenReturn(List.of(deleted));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        var summary = financialReportingService.summaryFor(null);

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.confirmedCollections().value()));
        assertEquals(0, summary.transactionCount().value().intValue());
    }

    @Test
    void paymentsOutsideTheRangeAreExcluded() {
        Payment old = completed(new BigDecimal("50000"), BigDecimal.ONE, BigDecimal.ONE);
        old.setCommissionStatus("RESOLVED");
        old.setPaidAt(LocalDateTime.now().minusDays(60));

        when(paymentRepository.findAll()).thenReturn(List.of(old));
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(0L);

        var summary = financialReportingService.summaryFor(
                FinancialReportingService.UUIDRange.lastNDays(30));

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.confirmedCollections().value()));
    }

    @Test
    void openReconciliationExceptionsAreSurfaced() {
        when(paymentRepository.findAll()).thenReturn(List.of());
        when(exceptionRepository.countByStatusAndIsDeletedFalse(any())).thenReturn(7L);

        var summary = financialReportingService.summaryFor(null);

        assertEquals(7, summary.openReconciliationExceptions().value().intValue());
    }
}
