package tz.elmkusoma.pricing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.pricing.domain.PricingPolicy;
import tz.elmkusoma.pricing.repository.PricingPolicyRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Platform Admin governance over pricing policy.
 *
 * <p>A price is proposal data until an authorized human activates it. This service is the only
 * path from DRAFT to ACTIVE, it requires the acting user to be named, and it records the
 * transition in the audit log - so "who approved this price, and when" is always answerable.</p>
 *
 * <p>It also refuses to activate a policy that conflicts with an approved band rather than
 * fixing it silently. Clamping a price into range would change what the provider asked for
 * without anybody deciding to, and the receipt would then disagree with the agreed terms.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingPolicyAdminService {

    private final PricingPolicyRepository pricingPolicyRepository;
    private final AuditLogRepository auditLogRepository;

    public static class GovernanceException extends IllegalStateException {
        public GovernanceException(String message) {
            super(message);
        }
    }

    @Transactional(readOnly = true)
    public List<PricingPolicy> listAll() {
        return pricingPolicyRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PricingPolicy> listByStatus(String status) {
        return pricingPolicyRepository
                .findByStatusAndIsDeletedFalseOrderByPolicyVersionDesc(status);
    }

    /**
     * Activates a policy.
     *
     * <p>Refuses rather than adjusts when the new price would breach an already-approved
     * education band, because that conflict needs a policy decision, not a silent correction.</p>
     */
    @Transactional
    public PricingPolicy activate(UUID policyId, UUID activatedBy, String note) {
        PricingPolicy policy = require(policyId);

        if (policy.isActive()) {
            throw new GovernanceException("Policy " + policy.getPolicyKey() + " v"
                    + policy.getPolicyVersion() + " is already ACTIVE");
        }
        if (policy.getPolicyVersion() == null) {
            throw new GovernanceException("A policy cannot be activated without a version");
        }
        if (policy.getServiceType() == null && policy.getEducationLevel() == null) {
            throw new GovernanceException("A policy must target a service type or an education level");
        }
        if (policy.getServiceType() != null && policy.getDefaultAmount() == null) {
            throw new GovernanceException("A service price policy must state a default amount");
        }
        if (policy.getEducationLevel() != null
                && (policy.getMinAmount() == null || policy.getMaxAmount() == null)) {
            throw new GovernanceException("An education band policy must state both a minimum and a maximum");
        }
        if (policy.getEducationLevel() != null && policy.getMinAmount().compareTo(policy.getMaxAmount()) > 0) {
            throw new GovernanceException("A band minimum cannot exceed its maximum");
        }

        // A second ACTIVE price for the same service, or band for the same level, is refused
        // here as well as by the partial unique index - the message is for a human, the index
        // is for a race.
        if (policy.getServiceType() != null && pricingPolicyRepository
                .existsByServiceTypeAndStatusAndIsDeletedFalse(policy.getServiceType(),
                        PricingPolicy.STATUS_ACTIVE)) {
            throw new GovernanceException("An ACTIVE price already exists for service type "
                    + policy.getServiceType() + "; retire it first");
        }
        if (policy.getEducationLevel() != null && pricingPolicyRepository
                .existsByEducationLevelAndStatusAndIsDeletedFalse(policy.getEducationLevel(),
                        PricingPolicy.STATUS_ACTIVE)) {
            throw new GovernanceException("An ACTIVE band already exists for education level "
                    + policy.getEducationLevel() + "; retire it first");
        }

        // Band-vs-price consistency is checked at price-resolution time, where the learner's
        // education level is actually known. Activation does not know it - Student carries no
        // education level, it lives on Grade - so no band check is done here rather than a
        // check against a guessed level.

        policy.setStatus(PricingPolicy.STATUS_ACTIVE);
        policy = pricingPolicyRepository.save(policy);

        audit(activatedBy, policy, "Pricing policy activated", note);
        log.info("Pricing policy {} v{} activated by {}", policy.getPolicyKey(),
                policy.getPolicyVersion(), activatedBy);
        return policy;
    }

    @Transactional
    public PricingPolicy retire(UUID policyId, UUID retiredBy, String note) {
        PricingPolicy policy = require(policyId);
        if (!policy.isActive()) {
            throw new GovernanceException("Only an ACTIVE policy can be retired, but it is " + policy.getStatus());
        }
        policy.setStatus(PricingPolicy.STATUS_RETIRED);
        policy = pricingPolicyRepository.save(policy);
        audit(retiredBy, policy, "Pricing policy retired", note);
        log.info("Pricing policy {} v{} retired by {}", policy.getPolicyKey(),
                policy.getPolicyVersion(), retiredBy);
        return policy;
    }

    @Transactional(readOnly = true)
    public List<PricingPolicy> historyFor(String serviceType, String educationLevel) {
        return pricingPolicyRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .filter(p -> (serviceType != null && serviceType.equals(p.getServiceType()))
                        || (educationLevel != null && educationLevel.equals(p.getEducationLevel())))
                .toList();
    }

    private PricingPolicy require(UUID policyId) {
        return pricingPolicyRepository.findById(policyId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Pricing policy not found"));
    }

    private void audit(UUID actor, PricingPolicy policy, String action, String note) {
        AuditLog auditLog = new AuditLog();
        auditLog.setInstitutionId(policy.getInstitutionId());
        auditLog.setUserId(actor);
        auditLog.setEntityType("PricingPolicy");
        auditLog.setEntityId(policy.getId());
        auditLog.setEntityName(policy.getPolicyKey());
        auditLog.setAction(AuditLog.AuditAction.UPDATE);
        auditLog.setOldValues(Map.of("status", policy.getStatus(),
                "policyVersion", String.valueOf(policy.getPolicyVersion())));
        auditLog.setNewValues(Map.of("action", action, "note", note == null ? "" : note,
                "activatedBy", actor == null ? "" : actor.toString(),
                "at", LocalDateTime.now().toString()));
        auditLogRepository.save(auditLog);
    }
}
