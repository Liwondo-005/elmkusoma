package tz.elmkusoma.institution.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.institution.dto.request.CreateInstitutionRequest;
import tz.elmkusoma.institution.dto.request.UpdateInstitutionRequest;
import tz.elmkusoma.institution.dto.response.InstitutionResponse;
import tz.elmkusoma.institution.service.InstitutionService;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class InstitutionServiceImpl implements InstitutionService {

    private static final Logger log = LoggerFactory.getLogger(InstitutionServiceImpl.class);

    private final InstitutionRepository institutionRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final tz.elmkusoma.audit.repository.AuditLogRepository auditLogRepository;
    private final tz.elmkusoma.nfe.provider.repository.EducationProviderRepository educationProviderRepository;

    public InstitutionServiceImpl(InstitutionRepository institutionRepository,
                                  InstitutionMembershipRepository membershipRepository,
                                  UserRepository userRepository,
                                  tz.elmkusoma.audit.repository.AuditLogRepository auditLogRepository,
                                  tz.elmkusoma.nfe.provider.repository.EducationProviderRepository educationProviderRepository) {
        this.institutionRepository = institutionRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.educationProviderRepository = educationProviderRepository;
    }

    /**
     * Creates the matching {@code nfe_education_providers} row for a provider-type institution.
     * Reuses the existing provider entity/repository -- no second provider model is introduced.
     */
private void provisionProviderIfProviderType(Institution institution, UUID actorId) {
        String type = institution.getType() != null ? institution.getType().name() : null;
        if (type == null) {
            return;
        }
        tz.elmkusoma.nfe.provider.domain.EducationProvider.ProviderType providerType;
        switch (type) {
            case "TRAINING_PROVIDER" -> providerType =
                    tz.elmkusoma.nfe.provider.domain.EducationProvider.ProviderType.TRAINING;
            case "COMPANY" -> providerType =
                    tz.elmkusoma.nfe.provider.domain.EducationProvider.ProviderType.COMPANY;
            case "GOVERNMENT" -> providerType =
                    tz.elmkusoma.nfe.provider.domain.EducationProvider.ProviderType.GOVERNMENT;
            case "TRAINING", "NGO", "PROFESSIONAL_BODY", "CONTENT_PROVIDER", "EVENT_PROVIDER" ->
                    providerType = switch (type) {
                        case "TRAINING" -> tz.elmkusoma.nfe.provider.domain.EducationProvider.ProviderType.TRAINING;
                        case "NGO" -> tz.elmkusoma.nfe.provider.domain.EducationProvider.ProviderType.ORGANIZATION;
                        default -> tz.elmkusoma.nfe.provider.domain.EducationProvider.ProviderType.ORGANIZATION;
                    };
            default -> {
                return; // not a provider type
            }
        }

        tz.elmkusoma.nfe.provider.domain.EducationProvider provider =
                tz.elmkusoma.nfe.provider.domain.EducationProvider.builder()
                        .name(institution.getName())
                        .providerType(providerType)
                        .description(institution.getDescription())
                        .logoUrl(institution.getLogoUrl())
                        .website(institution.getWebsite())
                        .email(institution.getEmail())
                        .phone(institution.getPhone())
                        .address(institution.getAddress())
                        .city(institution.getCity())
                        .country(institution.getCountry())
                        .isActive(true)
                        .isVerified(false)
                        .build();
        provider.setInstitutionId(institution.getId());
        provider.setCreatedBy(actorId != null ? actorId.toString() : "platform-admin");
        try {
            educationProviderRepository.save(provider);
            log.info("Provisioned NFE provider for institution {}", institution.getId());
        } catch (Exception e) {
            // Never fail institution provisioning over the provider mirror; /v1/nfe/providers/me
            // self-heals it on first provider access.
            log.warn("Could not provision NFE provider for institution {}: {}", institution.getId(), e.getMessage());
        }
    }

    /**
     * Audit trail for institution mutations (audit B-07). create/update/delete previously wrote
     * only a log line, so provisioning an organisation was invisible in the platform audit view.
     * Reuses the existing audit_logs entity -- no new audit system.
     */
    private void audit(UUID institutionId, UUID actorId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        try {
            User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
            tz.elmkusoma.audit.domain.AuditLog entry = new tz.elmkusoma.audit.domain.AuditLog();
            entry.setInstitutionId(institutionId);
            entry.setUserId(actorId);
            entry.setUserEmail(actor != null ? actor.getEmail() : "system");
            entry.setUserRole(actor != null && actor.getRole() != null ? actor.getRole().name() : "ADMIN");
            entry.setEntityType("INSTITUTION");
            entry.setEntityId(institutionId);
            entry.setEntityName(institutionId != null ? institutionId.toString() : "institution");
            entry.setAction(tz.elmkusoma.audit.domain.AuditLog.AuditAction.valueOf(action));
            entry.setOldValues(oldValues);
            entry.setNewValues(newValues);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // An audit failure must never abort the business transaction, but it must be visible.
            log.error("Failed to write institution audit (action={}, institution={})", action, institutionId, e);
        }
    }

    @Override
    public InstitutionResponse createInstitution(CreateInstitutionRequest request, UUID ownerUserId) {
        Institution.InstitutionType type;
        try {
            type = Institution.InstitutionType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid institution type: " + request.getType());
        }

        String code = generateCode(request.getName());

        Institution institution = Institution.builder()
                .name(request.getName())
                .code(code)
                .type(type)
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .website(request.getWebsite())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry() : "Tanzania")
                .regionId(request.getRegionId())
                .districtId(request.getDistrictId())
                .isActive(true)
                .isDeleted(false)
                .build();

        institution = institutionRepository.save(institution);
        log.info("Institution created: {} (ID: {})", institution.getName(), institution.getId());

        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerUserId));

        InstitutionMembership membership = InstitutionMembership.builder()
                .userId(ownerUserId)
                .institutionId(institution.getId())
                .role(InstitutionMembership.Role.OWNER)
                .isActive(true)
                .isDeleted(false)
                .build();
        membershipRepository.save(membership);

        // Ecosystem link (audit X-1 / B-01): a provider-type institution and its NFE provider record
        // used to be two unsynchronised tables for one concept, so a freshly created provider had no
        // provider row and its entire workspace was inert. Provision the provider here, in the same
        // transaction, from the institution the platform admin just approved.
        provisionProviderIfProviderType(institution, ownerUserId);

        audit(institution.getId(), ownerUserId, "CREATE", Map.of(),
                Map.of("name", String.valueOf(institution.getName()),
                        "type", String.valueOf(institution.getType()),
                        "code", String.valueOf(institution.getCode())));

        return mapToResponse(institution);
    }

    @Override
    @Transactional(readOnly = true)
    public InstitutionResponse getInstitution(UUID id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", id));
        return mapToResponse(institution);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InstitutionResponse> listInstitutions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Institution> institutionPage = institutionRepository.findByIsActiveTrueAndIsDeletedFalse(pageable);

        return new PageResponse<>(
                institutionPage.getContent().stream().map(this::mapToResponse).toList(),
                institutionPage.getNumber(),
                institutionPage.getSize(),
                institutionPage.getTotalElements(),
                institutionPage.getTotalPages(),
                institutionPage.isFirst(),
                institutionPage.isLast()
        );
    }

    @Override
    public InstitutionResponse updateInstitution(UUID id, UpdateInstitutionRequest request, UUID actorId) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", id));

        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("name", institution.getName());
        oldValues.put("email", institution.getEmail());
        oldValues.put("phone", institution.getPhone());
        oldValues.put("city", institution.getCity());
        oldValues.put("regionId", String.valueOf(institution.getRegionId()));
        oldValues.put("districtId", String.valueOf(institution.getDistrictId()));

        if (request.getName() != null) institution.setName(request.getName());
        if (request.getDescription() != null) institution.setDescription(request.getDescription());
        if (request.getLogoUrl() != null) institution.setLogoUrl(request.getLogoUrl());
        if (request.getWebsite() != null) institution.setWebsite(request.getWebsite());
        if (request.getEmail() != null) institution.setEmail(request.getEmail());
        if (request.getPhone() != null) institution.setPhone(request.getPhone());
        if (request.getAddress() != null) institution.setAddress(request.getAddress());
        if (request.getCity() != null) institution.setCity(request.getCity());
        if (request.getCountry() != null) institution.setCountry(request.getCountry());
        if (request.getRegionId() != null) institution.setRegionId(request.getRegionId());
        if (request.getDistrictId() != null) institution.setDistrictId(request.getDistrictId());

        institution = institutionRepository.save(institution);
        log.info("Institution updated: {} (ID: {})", institution.getName(), institution.getId());

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("name", institution.getName());
        newValues.put("email", institution.getEmail());
        newValues.put("phone", institution.getPhone());
        newValues.put("city", institution.getCity());
        newValues.put("regionId", String.valueOf(institution.getRegionId()));
        newValues.put("districtId", String.valueOf(institution.getDistrictId()));
        audit(institution.getId(), actorId, "UPDATE", oldValues, newValues);

        return mapToResponse(institution);
    }

    @Override
    public void deleteInstitution(UUID id, UUID actorId) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", id));
        Map<String, Object> oldValues = Map.of("name", String.valueOf(institution.getName()),
                "isActive", String.valueOf(institution.getIsActive()));
        institution.setIsDeleted(true);
        institutionRepository.save(institution);
        log.info("Institution soft-deleted: {} (ID: {})", institution.getName(), institution.getId());
        audit(institution.getId(), actorId, "DELETE", oldValues, Map.of());
    }

    @Override
    public InstitutionResponse activateInstitution(UUID id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", id));
        institution.setIsActive(true);
        institution = institutionRepository.save(institution);
        log.info("Institution activated: {} (ID: {})", institution.getName(), institution.getId());
        return mapToResponse(institution);
    }

    @Override
    public InstitutionResponse deactivateInstitution(UUID id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", id));
        institution.setIsActive(false);
        institution = institutionRepository.save(institution);
        log.info("Institution deactivated: {} (ID: {})", institution.getName(), institution.getId());
        return mapToResponse(institution);
    }

    private String generateCode(String name) {
        String base = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (base.length() > 10) base = base.substring(0, 10);
        return base + System.currentTimeMillis() % 10000;
    }

    private InstitutionResponse mapToResponse(Institution institution) {
        return InstitutionResponse.builder()
                .id(institution.getId())
                .name(institution.getName())
                .description(institution.getDescription())
                .type(institution.getType() != null ? institution.getType().name() : null)
                .status(institution.getIsActive() ? "ACTIVE" : "INACTIVE")
                .logoUrl(institution.getLogoUrl())
                .website(institution.getWebsite())
                .email(institution.getEmail())
                .phone(institution.getPhone())
                .address(institution.getAddress())
                .city(institution.getCity())
                .country(institution.getCountry())
                .regionId(institution.getRegionId())
                .districtId(institution.getDistrictId())
                .createdAt(institution.getCreatedAt())
                .build();
    }
}
