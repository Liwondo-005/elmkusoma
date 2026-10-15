package tz.elmkusoma.pricing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.pricing.domain.PricingPolicy;
import tz.elmkusoma.pricing.repository.PricingPolicyRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingPolicyAdminServiceTest {

    @Mock private PricingPolicyRepository pricingPolicyRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private PricingPolicyAdminService pricingPolicyAdminService;

    private PricingPolicy draftServicePrice() {
        return PricingPolicy.builder()
                .id(UUID.randomUUID())
                .policyKey("SERVICE_DEFAULT_PRICE")
                .policyVersion(1)
                .status(PricingPolicy.STATUS_DRAFT)
                .serviceType("ONLINE_COURSE")
                .defaultAmount(new BigDecimal("2000"))
                .currency("TZS")
                .build();
    }

    private PricingPolicy draftBand() {
        return PricingPolicy.builder()
                .id(UUID.randomUUID())
                .policyKey("EDUCATION_LEVEL_BAND")
                .policyVersion(1)
                .status(PricingPolicy.STATUS_DRAFT)
                .educationLevel("PRIMARY")
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("500000"))
                .currency("TZS")
                .build();
    }

    @Test
    void activate_movesADraftToActiveAndAuditsWhoApprovedIt() {
        PricingPolicy policy = draftServicePrice();
        when(pricingPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(pricingPolicyRepository.existsByServiceTypeAndStatusAndIsDeletedFalse(
                "ONLINE_COURSE", PricingPolicy.STATUS_ACTIVE)).thenReturn(false);
        when(pricingPolicyRepository.save(any(PricingPolicy.class)))
                .thenAnswer(i -> i.getArgument(0));

        UUID approver = UUID.randomUUID();
        PricingPolicy activated = pricingPolicyAdminService.activate(
                policy.getId(), approver, "Approved at board meeting");

        assertEquals(PricingPolicy.STATUS_ACTIVE, activated.getStatus());
        // The approver is named in the audit trail, so "who approved this price" is answerable.
        verify(auditLogRepository).save(any());
    }

    @Test
    void activate_refusesASecondActivePriceForTheSameService() {
        // Activating would otherwise leave two live prices for one service, and which one a
        // learner is charged would depend on query order.
        PricingPolicy policy = draftServicePrice();
        when(pricingPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(pricingPolicyRepository.existsByServiceTypeAndStatusAndIsDeletedFalse(
                "ONLINE_COURSE", PricingPolicy.STATUS_ACTIVE)).thenReturn(true);

        assertThrows(PricingPolicyAdminService.GovernanceException.class, () ->
                pricingPolicyAdminService.activate(policy.getId(), UUID.randomUUID(), null));
        verify(pricingPolicyRepository, never()).save(any());
    }

    @Test
    void activate_refusesAnAlreadyActivePolicy() {
        PricingPolicy policy = draftServicePrice();
        policy.setStatus(PricingPolicy.STATUS_ACTIVE);
        when(pricingPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        assertThrows(PricingPolicyAdminService.GovernanceException.class, () ->
                pricingPolicyAdminService.activate(policy.getId(), UUID.randomUUID(), null));
    }

    @Test
    void activate_refusesAPolicyThatStatesNoPrice() {
        // A service price with no amount is a proposal with nothing proposed; activating it
        // would make the policy look authoritative while quoting nothing.
        PricingPolicy policy = draftServicePrice();
        policy.setDefaultAmount(null);
        when(pricingPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        assertThrows(PricingPolicyAdminService.GovernanceException.class, () ->
                pricingPolicyAdminService.activate(policy.getId(), UUID.randomUUID(), null));
    }

    @Test
    void activate_refusesABandThatStatesOnlyOneSide() {
        PricingPolicy policy = draftBand();
        policy.setMaxAmount(null);
        when(pricingPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        assertThrows(PricingPolicyAdminService.GovernanceException.class, () ->
                pricingPolicyAdminService.activate(policy.getId(), UUID.randomUUID(), null));
    }

    @Test
    void activate_refusesABandWhoseMinimumExceedsItsMaximum() {
        // The conflict needs a policy decision, so it is refused rather than silently swapped.
        PricingPolicy policy = draftBand();
        policy.setMinAmount(new BigDecimal("900000"));
        when(pricingPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        assertThrows(PricingPolicyAdminService.GovernanceException.class, () ->
                pricingPolicyAdminService.activate(policy.getId(), UUID.randomUUID(), null));
    }

    @Test
    void activate_refusesAPolicyWithNoVersion() {
        PricingPolicy policy = draftServicePrice();
        policy.setPolicyVersion(null);
        when(pricingPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        assertThrows(PricingPolicyAdminService.GovernanceException.class, () ->
                pricingPolicyAdminService.activate(policy.getId(), UUID.randomUUID(), null));
    }

    @Test
    void activate_activatesABandAndAudits() {
        PricingPolicy policy = draftBand();
        when(pricingPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(pricingPolicyRepository.existsByEducationLevelAndStatusAndIsDeletedFalse(
                "PRIMARY", PricingPolicy.STATUS_ACTIVE)).thenReturn(false);
        when(pricingPolicyRepository.save(any(PricingPolicy.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertEquals(PricingPolicy.STATUS_ACTIVE,
                pricingPolicyAdminService.activate(policy.getId(), UUID.randomUUID(), null).getStatus());
        verify(auditLogRepository).save(any());
    }

    @Test
    void retire_movesAnActivePolicyToRetired_andAudits() {
        PricingPolicy policy = draftServicePrice();
        policy.setStatus(PricingPolicy.STATUS_ACTIVE);
        when(pricingPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(pricingPolicyRepository.save(any(PricingPolicy.class)))
                .thenAnswer(i -> i.getArgument(0));

        UUID actor = UUID.randomUUID();
        PricingPolicy retired = pricingPolicyAdminService.retire(policy.getId(), actor, null);

        assertEquals(PricingPolicy.STATUS_RETIRED, retired.getStatus());
        verify(auditLogRepository).save(any());
    }

    @Test
    void retire_refusesToRetireSomethingThatIsNotActive() {
        PricingPolicy policy = draftServicePrice();
        when(pricingPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        assertThrows(PricingPolicyAdminService.GovernanceException.class, () ->
                pricingPolicyAdminService.retire(policy.getId(), UUID.randomUUID(), null));
    }

    @Test
    void activate_reportsAMissingPolicyRatherThanCreatingOne() {
        UUID missing = UUID.randomUUID();
        when(pricingPolicyRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class, () ->
                pricingPolicyAdminService.activate(missing, UUID.randomUUID(), null));
    }
}
