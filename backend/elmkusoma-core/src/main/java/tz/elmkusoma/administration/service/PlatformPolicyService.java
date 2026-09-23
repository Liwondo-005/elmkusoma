package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.PlatformConfigEntry;
import tz.elmkusoma.administration.dto.PolicyFlagResponse;
import tz.elmkusoma.administration.repository.PlatformConfigRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central policy & governance engine (spec §62).
 * Platform rules live in platform_config (POLICY category) instead of being
 * scattered across frontend components. Defaults are permissive ("true") so a
 * missing config row never changes existing behaviour.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlatformPolicyService {

    static final List<String> POLICY_KEYS = List.of(
            "policy.registration.enabled",
            "policy.provider.create.enabled",
            "policy.content.publish.enabled",
            "policy.live.create.enabled",
            "policy.certificate.issue.enabled",
            "policy.charge.enabled",
            "policy.sponsor.enabled",
            "policy.invite.enabled",
            "policy.paid_access.enabled",
            "policy.verification.required",
            "policy.approval.required"
    );

    private final PlatformConfigRepository configRepository;

    public boolean isEnabled(String key) {
        try {
            return configRepository.findByConfigKeyAndIsDeletedFalse(key)
                    .map(PlatformConfigEntry::getConfigValue)
                    .map(v -> !("false".equalsIgnoreCase(v.trim())))
                    .orElse(true);
        } catch (Exception e) {
            log.warn("Policy read failed for {} — defaulting to enabled: {}", key, e.getMessage());
            return true;
        }
    }

    public boolean registrationEnabled() { return isEnabled("policy.registration.enabled"); }
    public boolean providerCreateEnabled() { return isEnabled("policy.provider.create.enabled"); }
    public boolean contentPublishEnabled() { return isEnabled("policy.content.publish.enabled"); }
    public boolean liveCreateEnabled() { return isEnabled("policy.live.create.enabled"); }
    public boolean certificateIssueEnabled() { return isEnabled("policy.certificate.issue.enabled"); }
    public boolean chargeEnabled() { return isEnabled("policy.charge.enabled"); }
    public boolean sponsorEnabled() { return isEnabled("policy.sponsor.enabled"); }
    public boolean inviteEnabled() { return isEnabled("policy.invite.enabled"); }
    public boolean paidAccessEnabled() { return isEnabled("policy.paid_access.enabled"); }
    public boolean verificationRequired() { return isEnabled("policy.verification.required"); }
    public boolean approvalRequired() { return isEnabled("policy.approval.required"); }

    public void require(String key, String operation) {
        if (!isEnabled(key)) {
            throw new IllegalStateException("Platform policy '" + key + "' forbids: " + operation);
        }
    }

    public List<PolicyFlagResponse> listPolicies() {
        Map<String, PolicyFlagResponse> out = new LinkedHashMap<>();
        for (String key : POLICY_KEYS) {
            PlatformConfigEntry entry = configRepository.findByConfigKeyAndIsDeletedFalse(key).orElse(null);
            out.put(key, PolicyFlagResponse.builder()
                    .key(key)
                    .value(entry != null && entry.getConfigValue() != null ? entry.getConfigValue() : "true")
                    .description(entry != null ? entry.getDescription() : "Default policy flag (missing config row)")
                    .category("POLICY")
                    .build());
        }
        return List.copyOf(out.values());
    }
}
