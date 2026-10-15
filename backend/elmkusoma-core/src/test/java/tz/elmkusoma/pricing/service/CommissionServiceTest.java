package tz.elmkusoma.pricing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.pricing.domain.CommissionPolicy;
import tz.elmkusoma.pricing.repository.CommissionPolicyRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock private CommissionPolicyRepository commissionPolicyRepository;
    @InjectMocks private CommissionService commissionService;

    private CommissionPolicy policy(String status, BigDecimal rate, BigDecimal fixed) {
        return CommissionPolicy.builder()
                .policyKey("PLATFORM_COMMISSION")
                .policyVersion(1)
                .status(status)
                .serviceType("ONLINE_COURSE")
                .commissionRate(rate)
                .fixedAmount(fixed)
                .currency("TZS")
                .build();
    }

    @Test
    void allocate_isUnresolved_whenNoActivePolicyExists() {
        // The seeded catalogue is DRAFT with no rate. Reporting zero here would invent a
        // commission the business never approved.
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.empty());

        CommissionService.Allocation allocation = commissionService.allocate(
                "ONLINE_COURSE", null, new BigDecimal("2000"), BigDecimal.ZERO);

        assertFalse(allocation.resolved());
        assertEquals(CommissionService.AllocationStatus.NO_ACTIVE_POLICY, allocation.status());
        assertNull(allocation.commissionAmount());
        assertNull(allocation.providerShare());
        assertTrue(allocation.reason().contains("unresolved"));
    }

    @Test
    void allocate_isUnresolved_whenTheActivePolicyStatesNoRate() {
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(policy(CommissionPolicy.STATUS_ACTIVE, null, null)));

        CommissionService.Allocation allocation = commissionService.allocate(
                "ONLINE_COURSE", null, new BigDecimal("2000"), BigDecimal.ZERO);

        assertFalse(allocation.resolved());
        assertEquals(CommissionService.AllocationStatus.RATE_NOT_APPROVED, allocation.status());
        assertNull(allocation.commissionAmount());
        assertTrue(allocation.reason().contains("states no rate"));
    }

    @Test
    void allocate_computesFromAnApprovedRate() {
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(policy(CommissionPolicy.STATUS_ACTIVE,
                        new BigDecimal("0.150000"), null)));

        CommissionService.Allocation allocation = commissionService.allocate(
                "ONLINE_COURSE", null, new BigDecimal("2000"), BigDecimal.ZERO);

        assertTrue(allocation.resolved());
        assertEquals(0, new BigDecimal("300.00").compareTo(allocation.commissionAmount()));
        assertEquals(0, new BigDecimal("1700.00").compareTo(allocation.providerShare()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(allocation.netAmount()));
        assertEquals("PLATFORM_COMMISSION", allocation.policyKey());
        assertEquals(1, allocation.policyVersion());
    }

    @Test
    void allocate_allocatesOnTheNetNotTheGross() {
        // A 15% commission on a 2000 price with a 200 discount is 15% of 1800, not of 2000.
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(policy(CommissionPolicy.STATUS_ACTIVE,
                        new BigDecimal("0.150000"), null)));

        CommissionService.Allocation allocation = commissionService.allocate(
                "ONLINE_COURSE", null, new BigDecimal("2000"), new BigDecimal("200"));

        assertEquals(0, new BigDecimal("1800.00").compareTo(allocation.netAmount()));
        assertEquals(0, new BigDecimal("270.00").compareTo(allocation.commissionAmount()));
        assertEquals(0, new BigDecimal("1530.00").compareTo(allocation.providerShare()));
    }

    @Test
    void allocate_supportsAFlatFeePolicy() {
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(policy(CommissionPolicy.STATUS_ACTIVE,
                        null, new BigDecimal("500"))));

        CommissionService.Allocation allocation = commissionService.allocate(
                "ONLINE_COURSE", null, new BigDecimal("2000"), BigDecimal.ZERO);

        assertEquals(0, new BigDecimal("500.00").compareTo(allocation.commissionAmount()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(allocation.providerShare()));
    }

    @Test
    void allocate_refusesARateAboveOneHundredPercent() {
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(policy(CommissionPolicy.STATUS_ACTIVE,
                        new BigDecimal("1.500000"), null)));

        assertThrows(PricingService.PricingConflictException.class, () ->
                commissionService.allocate("ONLINE_COURSE", null, new BigDecimal("2000"), BigDecimal.ZERO));
    }

    @Test
    void allocate_refusesANegativeRate() {
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(policy(CommissionPolicy.STATUS_ACTIVE,
                        new BigDecimal("-0.10"), null)));

        assertThrows(PricingService.PricingConflictException.class, () ->
                commissionService.allocate("ONLINE_COURSE", null, new BigDecimal("2000"), BigDecimal.ZERO));
    }

    @Test
    void allocate_refusesADiscountLargerThanTheGrossAmount() {
        assertThrows(PricingService.PricingConflictException.class, () ->
                commissionService.allocate("ONLINE_COURSE", null,
                        new BigDecimal("2000"), new BigDecimal("5000")));
    }

    @Test
    void allocate_refusesANegativeDiscount() {
        assertThrows(PricingService.PricingConflictException.class, () ->
                commissionService.allocate("ONLINE_COURSE", null,
                        new BigDecimal("2000"), new BigDecimal("-100")));
    }

    @Test
    void allocate_flagsWhenTheCommissionExceedsTheNetAmount() {
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(policy(CommissionPolicy.STATUS_ACTIVE,
                        null, new BigDecimal("99999"))));

        CommissionService.Allocation allocation = commissionService.allocate(
                "ONLINE_COURSE", null, new BigDecimal("2000"), BigDecimal.ZERO);

        assertEquals(CommissionService.AllocationStatus.RATE_EXCEEDS_AMOUNT, allocation.status());
        assertFalse(allocation.resolved());
    }

    @Test
    void allocate_normalisesTheServiceTypeSoCasingCannotBypassThePolicy() {
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(policy(CommissionPolicy.STATUS_ACTIVE,
                        new BigDecimal("0.10"), null)));

        assertTrue(commissionService.allocate(" online_course ", null,
                new BigDecimal("2000"), BigDecimal.ZERO).resolved());
    }

    @Test
    void feeBearerIsCarriedThroughButNeverInvented() {
        CommissionPolicy undecided = policy(CommissionPolicy.STATUS_ACTIVE,
                new BigDecimal("0.10"), null);
        undecided.setFeeBearer(null);
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(undecided));

        // The rate may be approved while who bears the gateway fee is still open. The allocation
        // carries that through as null rather than assuming either party.
        assertNull(commissionService.allocate("ONLINE_COURSE", null,
                new BigDecimal("2000"), BigDecimal.ZERO).feeBearer());
    }

    @Test
    void allocate_fallsBackToAProviderScopedPolicy() {
        UUID providerId = UUID.randomUUID();
        when(commissionPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "SOMETHING_ELSE", CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.empty());
        CommissionPolicy providerPolicy = policy(CommissionPolicy.STATUS_ACTIVE,
                new BigDecimal("0.20"), null);
        providerPolicy.setServiceType(null);
        providerPolicy.setProviderId(providerId);
        when(commissionPolicyRepository
                .findFirstByProviderIdAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        providerId, CommissionPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(providerPolicy));

        CommissionService.Allocation allocation = commissionService.allocate(
                "SOMETHING_ELSE", providerId, new BigDecimal("1000"), BigDecimal.ZERO);

        assertTrue(allocation.resolved());
        assertEquals(0, new BigDecimal("200.00").compareTo(allocation.commissionAmount()));
    }
}
