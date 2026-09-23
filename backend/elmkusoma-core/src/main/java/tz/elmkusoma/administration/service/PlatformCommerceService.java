package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.ProviderServiceEntitlement;
import tz.elmkusoma.administration.dto.SponsorGrantResponse;
import tz.elmkusoma.administration.dto.SponsorSeatGrantRequest;
import tz.elmkusoma.administration.repository.ProviderServiceEntitlementRepository;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.repository.EntitlementRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlatformCommerceService {

    private final ProviderServiceEntitlementRepository providerEntitlementRepository;
    private final EntitlementRepository entitlementRepository;
    private final AuditLogRepository auditLogRepository;
    private final PlatformPolicyService platformPolicyService;

    /**
     * Payment Model B — provider sponsors seats (spec §36).
     * Consumes provider package seats and grants SPONSORED entitlements
     * so selected users join free (hybrid Model C shares the same entitlement engine).
     */
    public SponsorGrantResponse grantSponsoredSeats(SponsorSeatGrantRequest req, UUID grantedBy) {
        if (platformPolicyService != null && !platformPolicyService.sponsorEnabled()) {
            throw new IllegalStateException("Platform policy forbids sponsoring users");
        }
        ProviderServiceEntitlement pkg = providerEntitlementRepository
                .findByProviderIdAndServiceIdAndIsDeletedFalse(req.getProviderId(), req.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider entitlement", "providerId+serviceId",
                        req.getProviderId() + "+" + req.getServiceId()));

        if (!"ACTIVE".equals(pkg.getStatus())) {
            throw new IllegalStateException("Provider package is not ACTIVE (status: " + pkg.getStatus() + ")");
        }
        if (pkg.getExpiresAt() != null && pkg.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Provider package has expired");
        }

        int used = pkg.getSeatsUsed() != null ? pkg.getSeatsUsed() : 0;
        int max = pkg.getMaxSeats() != null ? pkg.getMaxSeats() : Integer.MAX_VALUE;

        // One active sponsored/student/service entitlement at a time — extend instead of duplicate
        Entitlement existing = entitlementRepository
                .findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                        req.getStudentId(), "SPONSORED_SERVICE", req.getServiceId())
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .orElse(null);

        List<UUID> grantedIds = new ArrayList<>();
        int newlyConsumed = 0;
        if (existing != null) {
            existing.setExpiresAt(req.getExpiresAt() != null ? req.getExpiresAt() : LocalDateTime.now().plusDays(365));
            entitlementRepository.save(existing);
            grantedIds.add(existing.getId());
        } else {
            if (used + req.getSeats() > max) {
                throw new IllegalStateException("Insufficient seats: " + used + "/" + max
                        + ", requested " + req.getSeats());
            }
            Entitlement ent = Entitlement.builder()
                    .userId(req.getUserId())
                    .studentId(req.getStudentId())
                    .serviceType("SPONSORED_SERVICE")
                    .serviceId(req.getServiceId())
                    .status("ACTIVE")
                    .startsAt(LocalDateTime.now())
                    .expiresAt(req.getExpiresAt() != null ? req.getExpiresAt() : LocalDateTime.now().plusDays(365))
                    .metadata(buildSponsorMetadata(req, grantedBy))
                    .build();
            ent.setInstitutionId(pkg.getInstitutionId() != null ? pkg.getInstitutionId()
                    : PlatformAdminService.PLATFORM_INSTITUTION_ID);
            entitlementRepository.save(ent);
            grantedIds.add(ent.getId());
            newlyConsumed = req.getSeats();
        }

        if (newlyConsumed > 0) {
            pkg.setSeatsUsed(used + newlyConsumed);
            providerEntitlementRepository.save(pkg);
        }

        AuditLog al = new AuditLog();
        al.setInstitutionId(pkg.getInstitutionId() != null ? pkg.getInstitutionId()
                : PlatformAdminService.PLATFORM_INSTITUTION_ID);
        al.setUserId(grantedBy);
        al.setEntityType("ENTITLEMENT");
        al.setEntityId(grantedIds.get(0));
        al.setEntityName("Sponsored Seat Grant");
        al.setAction(AuditLog.AuditAction.CREATE);
        al.setNewValues(Map.of("providerId", req.getProviderId().toString(),
                "serviceId", req.getServiceId().toString(),
                "seats", String.valueOf(req.getSeats()),
                "seatsUsed", String.valueOf(pkg.getSeatsUsed() != null ? pkg.getSeatsUsed() : 0)));
        auditLogRepository.save(al);

        log.info("Sponsored seats granted: provider={} service={} seats={} by {}",
                req.getProviderId(), req.getServiceId(), req.getSeats(), grantedBy);

        return SponsorGrantResponse.builder()
                .seatsGranted(newlyConsumed)
                .seatsUsed(pkg.getSeatsUsed() != null ? pkg.getSeatsUsed() : 0)
                .maxSeats(pkg.getMaxSeats())
                .entitlementIds(grantedIds)
                .build();
    }

    @Transactional
    public void revokeSponsoredEntitlement(UUID entitlementId, UUID revokedBy) {
        Entitlement ent = entitlementRepository.findById(entitlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Entitlement", "id", entitlementId));
        if (!"ACTIVE".equals(ent.getStatus())) {
            return;
        }
        String metadata = ent.getMetadata();
        ent.setStatus("REVOKED");
        entitlementRepository.save(ent);

        // Free the provider seat if this was a sponsored grant
        if (metadata != null && metadata.contains("providerEntitlementId")) {
            UUID pkgId = extractJsonUuid(metadata, "providerEntitlementId");
            if (pkgId != null) {
                providerEntitlementRepository.findById(pkgId).ifPresent(pkg -> {
                    int used = pkg.getSeatsUsed() != null ? pkg.getSeatsUsed() : 0;
                    if (used > 0) {
                        pkg.setSeatsUsed(used - 1);
                        providerEntitlementRepository.save(pkg);
                    }
                });
            }
        }

        AuditLog al = new AuditLog();
        al.setInstitutionId(ent.getInstitutionId() != null ? ent.getInstitutionId()
                : PlatformAdminService.PLATFORM_INSTITUTION_ID);
        al.setUserId(revokedBy);
        al.setEntityType("ENTITLEMENT");
        al.setEntityId(entitlementId);
        al.setEntityName("Sponsored Entitlement Revoke");
        al.setAction(AuditLog.AuditAction.UPDATE);
        al.setOldValues(Map.of("status", "ACTIVE"));
        al.setNewValues(Map.of("status", "REVOKED"));
        auditLogRepository.save(al);

        log.info("Sponsored entitlement {} revoked by {}", entitlementId, revokedBy);
    }

    private String buildSponsorMetadata(SponsorSeatGrantRequest req, UUID grantedBy) {
        ProviderServiceEntitlement pkg = providerEntitlementRepository
                .findByProviderIdAndServiceIdAndIsDeletedFalse(req.getProviderId(), req.getServiceId())
                .orElse(null);
        return "{\"origin\":\"SPONSORED\",\"providerId\":\"" + req.getProviderId()
                + "\",\"serviceId\":\"" + req.getServiceId()
                + "\",\"providerEntitlementId\":\"" + (pkg != null ? pkg.getId() : "")
                + "\",\"grantedBy\":\"" + grantedBy
                + "\",\"note\":" + (req.getNote() != null ? "\"" + req.getNote().replace("\"", "'") + "\"" : "null") + "}";
    }

    private UUID extractJsonUuid(String json, String key) {
        try {
            String marker = "\"" + key + "\":\"";
            int start = json.indexOf(marker);
            if (start < 0) return null;
            start += marker.length();
            int end = json.indexOf('"', start);
            if (end <= start) return null;
            return UUID.fromString(json.substring(start, end));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
