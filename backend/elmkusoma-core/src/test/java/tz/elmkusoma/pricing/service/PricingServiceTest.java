package tz.elmkusoma.pricing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.pricing.domain.PricingPolicy;
import tz.elmkusoma.pricing.repository.PricingPolicyRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock private PricingPolicyRepository pricingPolicyRepository;
    @InjectMocks private PricingService pricingService;

    private PricingPolicy activePrice(String serviceType, String amount, Integer accessDays) {
        return PricingPolicy.builder()
                .policyKey("SERVICE_DEFAULT_PRICE")
                .policyVersion(1)
                .status(PricingPolicy.STATUS_ACTIVE)
                .serviceType(serviceType)
                .defaultAmount(new BigDecimal(amount))
                .currency("TZS")
                .accessDays(accessDays)
                .build();
    }

    private PricingPolicy activeBand(String educationLevel, String min, String max) {
        return PricingPolicy.builder()
                .policyKey("EDUCATION_LEVEL_BAND")
                .policyVersion(1)
                .status(PricingPolicy.STATUS_ACTIVE)
                .educationLevel(educationLevel)
                .minAmount(new BigDecimal(min))
                .maxAmount(new BigDecimal(max))
                .currency("TZS")
                .build();
    }

    @Test
    void resolvePrice_returnsTheActivePolicyAmountAndItsVersion() {
        when(pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(activePrice("ONLINE_COURSE", "2000", 30)));

        PricingService.ResolvedPrice price = pricingService.resolvePrice("ONLINE_COURSE", null);

        assertEquals(0, new BigDecimal("2000").compareTo(price.amount()));
        assertEquals("TZS", price.currency());
        assertEquals(1, price.policyVersion());
        assertEquals("SERVICE_DEFAULT_PRICE", price.policyKey());
        assertEquals(30, price.accessDays());
    }

    @Test
    void resolvePrice_normalisesTheServiceTypeSoCasingCannotBypassThePolicy() {
        when(pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(activePrice("ONLINE_COURSE", "2000", 30)));

        assertEquals(0, new BigDecimal("2000")
                .compareTo(pricingService.resolvePrice("  online_course  ", null).amount()));
    }

    @Test
    void resolvePrice_refusesToChargeWhenNoActivePolicyExists() {
        // The catalogue is seeded DRAFT and only an authorized approval makes a price
        // authoritative. While nothing is ACTIVE the correct behaviour is to refuse the charge,
        // not to fall back to whatever the caller sent.
        when(pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(PricingService.PricingNotConfiguredException.class,
                () -> pricingService.resolvePrice("ONLINE_COURSE", null));
    }

    @Test
    void resolvePrice_refusesWhenTheActivePolicyHasNoAmount() {
        PricingPolicy noAmount = activePrice("ONLINE_COURSE", "2000", 30);
        noAmount.setDefaultAmount(null);
        when(pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(noAmount));

        assertThrows(PricingService.PricingNotConfiguredException.class,
                () -> pricingService.resolvePrice("ONLINE_COURSE", null));
    }

    @Test
    void resolvePrice_refusesANegativePolicyAmount() {
        when(pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(activePrice("ONLINE_COURSE", "-1", 30)));

        assertThrows(PricingService.PricingConflictException.class,
                () -> pricingService.resolvePrice("ONLINE_COURSE", null));
    }

    @Test
    void resolvePrice_rejectsACataloguePriceOutsideTheEducationBand() {
        when(pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(activePrice("ONLINE_COURSE", "9000", 30)));
        when(pricingPolicyRepository
                .findFirstByEducationLevelAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "PRIMARY", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(activeBand("PRIMARY", "500", "3000")));

        // Refused, never clamped into 3000: a silent rewrite would leave the receipt
        // disagreeing with what was actually agreed.
        assertThrows(PricingService.PricingConflictException.class,
                () -> pricingService.resolvePrice("ONLINE_COURSE", EducationLevel.PRIMARY));
    }

    @Test
    void resolvePrice_acceptsACataloguePriceInsideTheEducationBand() {
        when(pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(activePrice("ONLINE_COURSE", "2000", 30)));
        when(pricingPolicyRepository
                .findFirstByEducationLevelAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "PRIMARY", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(activeBand("PRIMARY", "500", "3000")));

        assertEquals(0, new BigDecimal("2000")
                .compareTo(pricingService.resolvePrice("ONLINE_COURSE", EducationLevel.PRIMARY).amount()));
    }

    @Test
    void resolvePrice_ignoresBandsForPlatformFixedSubscriptionTiers() {
        // Premium is TZS 7,000 for every learner. Validating it against the Primary band of
        // 500-3,000 would refuse a legitimate subscription as though it were a pricing conflict.
        when(pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "PREMIUM_ACCESS", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(activePrice("PREMIUM_ACCESS", "7000", 30)));

        assertEquals(0, new BigDecimal("7000")
                .compareTo(pricingService.resolvePrice("PREMIUM_ACCESS", EducationLevel.PRIMARY).amount()));
    }

    @Test
    void resolvePrice_skipsBandValidationWhenNoBandIsPublished() {
        when(pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "ONLINE_COURSE", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.of(activePrice("ONLINE_COURSE", "2000", 30)));
        when(pricingPolicyRepository
                .findFirstByEducationLevelAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        "UNIVERSITY", PricingPolicy.STATUS_ACTIVE))
                .thenReturn(Optional.empty());

        assertEquals(0, new BigDecimal("2000")
                .compareTo(pricingService.resolvePrice("ONLINE_COURSE", EducationLevel.UNIVERSITY).amount()));
    }

    @Test
    void requireAmountMatches_acceptsAnEqualOrAbsentCallerAmount() {
        PricingService.ResolvedPrice resolved = new PricingService.ResolvedPrice(
                new BigDecimal("2000"), "TZS", 1, "SERVICE_DEFAULT_PRICE", 30);

        assertDoesNotThrow(() -> pricingService.requireAmountMatches(null, resolved));
        assertDoesNotThrow(() -> pricingService.requireAmountMatches(new BigDecimal("2000"), resolved));
        // 2000.0 must equal 2000: a scale difference is not a price disagreement.
        assertDoesNotThrow(() -> pricingService.requireAmountMatches(new BigDecimal("2000.0"), resolved));
    }

    @Test
    void requireAmountMatches_rejectsADifferentCallerAmount() {
        PricingService.ResolvedPrice resolved = new PricingService.ResolvedPrice(
                new BigDecimal("2000"), "TZS", 1, "SERVICE_DEFAULT_PRICE", 30);

        assertThrows(PricingService.PricingConflictException.class,
                () -> pricingService.requireAmountMatches(new BigDecimal("1"), resolved));
        assertThrows(PricingService.PricingConflictException.class,
                () -> pricingService.requireAmountMatches(new BigDecimal("1999.99"), resolved));
    }

    @Test
    void resolvePrice_requiresAServiceType() {
        assertThrows(PricingService.PricingNotConfiguredException.class,
                () -> pricingService.resolvePrice(null, null));
        assertThrows(PricingService.PricingNotConfiguredException.class,
                () -> pricingService.resolvePrice("   ", null));
    }

    @Test
    void bandGovernedScoping_isDeliberate() {
        assertTrue(PricingService.isBandGoverned("ONLINE_COURSE"));
        assertTrue(PricingService.isBandGoverned("LEARNING_RESOURCE"));
        assertTrue(PricingService.isBandGoverned("LIVE_CLASS"));
        assertFalse(PricingService.isBandGoverned("PREMIUM_ACCESS"));
        assertFalse(PricingService.isBandGoverned("MONTHLY_ACCESS"));
        assertFalse(PricingService.isBandGoverned("CERTIFICATE_REPRINT"));
    }

    @Test
    void resolvePrice_neverReadsADraftPolicy() {
        // Guards the query contract itself: only an ACTIVE status is ever asked for, so a DRAFT
        // row can never be the source of a charge.
        when(pricingPolicyRepository.findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                eq("ONLINE_COURSE"), any())).thenReturn(Optional.empty());

        assertThrows(PricingService.PricingNotConfiguredException.class,
                () -> pricingService.resolvePrice("ONLINE_COURSE", null));
    }
}
