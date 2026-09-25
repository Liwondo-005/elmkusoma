package tz.elmkusoma.certificate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.certificate.domain.*;
import tz.elmkusoma.certificate.dto.*;
import tz.elmkusoma.certificate.mapper.CertificateMapper;
import tz.elmkusoma.certificate.repository.*;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.repository.InstitutionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Platform Admin certificate governance: signatories (profile + authorisation),
 * template management/versioning, template-signatory links, certificate detail and
 * overview aggregations.
 *
 * <p>All endpoints sit behind {@code /v1/platform-admin/**} which requires the ADMIN
 * role at the controller — backend authorization is authoritative. Institution/provider
 * ownership is preserved: templates are always created for an explicit institution and
 * signatory scope decides which institution's certificates they may sign.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CertificateGovernanceService {

    /** Seeded "ELMKUSOMA National HQ" institution — platform-wide audit fallback (same value as PlatformAdminService.writeAudit). */
    private static final UUID PLATFORM_INSTITUTION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    /** ~225 KB decoded — keeps signature data URLs modest and safe to render. */
    private static final int MAX_SIGNATURE_IMAGE_CHARS = 300_000;

    private final CertificateSignatoryRepository signatoryRepository;
    private final CertificateTemplateSignatoryRepository templateSignatoryRepository;
    private final CertificateTemplateVersionRepository templateVersionRepository;
    private final CertificateTemplateRepository templateRepository;
    private final CertificateRepository certificateRepository;
    private final InstitutionRepository institutionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final CertificateMapper certificateMapper;

    // ───────────────────────────────────────────────────────────────────────
    // Signatories
    // ───────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<SignatoryResponse> listSignatories(String search, String status, UUID institutionId,
                                                           int page, int size) {
        CertificateSignatory.SignatoryStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = CertificateSignatory.SignatoryStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                statusEnum = null;
            }
        }
        String searchTrim = (search == null || search.isBlank()) ? null : search.trim();

        Page<CertificateSignatory> result = signatoryRepository.search(
                searchTrim, statusEnum, institutionId, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fullName")));

        Map<UUID, String> institutionNames = resolveInstitutionNames(
                result.getContent().stream().map(CertificateSignatory::getInstitutionId).toList());

        return new PageResponse<>(
                result.getContent().stream().map(s -> toSignatoryResponse(s, institutionNames)).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast());
    }

    public SignatoryResponse createSignatory(SignatoryRequest req, UUID actorId, String actorEmail, String actorRole) {
        if (req == null || req.getFullName() == null || req.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Signatory full name is required");
        }
        validateSignatureImage(req.getSignatureImage());
        String types = normalizeTypes(req.getCertificateTypes());
        validateDates(req.getValidFrom(), req.getValidUntil());
        UUID institutionId = validateInstitutionScope(req.getInstitutionId());
        CertificateSignatory.SignatoryStatus status = parseStatus(req.getStatus());

        CertificateSignatory signatory = CertificateSignatory.builder()
                .fullName(req.getFullName().trim())
                .positionTitle(trimToNull(req.getPositionTitle()))
                .organization(trimToNull(req.getOrganization()))
                .signatureImage(trimToNull(req.getSignatureImage()))
                .certificateTypes(types)
                .status(status)
                .validFrom(req.getValidFrom())
                .validUntil(req.getValidUntil())
                .build();
        signatory.setInstitutionId(institutionId);
        signatory = signatoryRepository.save(signatory);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("fullName", signatory.getFullName());
        newValues.put("status", signatory.getStatus().name());
        newValues.put("certificateTypes", types != null ? types : "ALL");
        newValues.put("institutionScope", institutionId != null ? institutionId.toString() : "PLATFORM");
        writeAudit(institutionId, actorId, actorEmail, actorRole,
                "CertificateSignatory", signatory.getId(), signatory.getFullName(),
                AuditLog.AuditAction.CREATE, null, newValues);

        log.info("Created certificate signatory {} (scope: {})", signatory.getId(),
                institutionId != null ? institutionId : "platform");
        return toSignatoryResponse(signatory, Collections.emptyMap());
    }

    public SignatoryResponse updateSignatory(UUID signatoryId, SignatoryRequest req,
                                             UUID actorId, String actorEmail, String actorRole) {
        CertificateSignatory signatory = signatoryRepository.findByIdAndIsDeletedFalse(signatoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CertificateSignatory", "id", signatoryId));
        if (req == null || req.getFullName() == null || req.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Signatory full name is required");
        }
        validateSignatureImage(req.getSignatureImage());
        String types = normalizeTypes(req.getCertificateTypes());
        validateDates(req.getValidFrom(), req.getValidUntil());
        UUID institutionId = validateInstitutionScope(req.getInstitutionId());

        Map<String, Object> oldValues = authorizationSnapshot(signatory);
        signatory.setFullName(req.getFullName().trim());
        signatory.setPositionTitle(trimToNull(req.getPositionTitle()));
        signatory.setOrganization(trimToNull(req.getOrganization()));
        signatory.setSignatureImage(trimToNull(req.getSignatureImage()));
        signatory.setCertificateTypes(types);
        signatory.setValidFrom(req.getValidFrom());
        signatory.setValidUntil(req.getValidUntil());
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            signatory.setStatus(parseStatus(req.getStatus()));
        }
        signatory.setInstitutionId(institutionId);
        signatory = signatoryRepository.save(signatory);

        Map<String, Object> newValues = authorizationSnapshot(signatory);
        writeAudit(institutionId, actorId, actorEmail, actorRole,
                "CertificateSignatory", signatory.getId(), signatory.getFullName(),
                AuditLog.AuditAction.UPDATE, oldValues, newValues);

        log.info("Updated certificate signatory {}", signatoryId);
        return toSignatoryResponse(signatory, Collections.emptyMap());
    }

    public Map<String, Object> deleteSignatory(UUID signatoryId, UUID actorId, String actorEmail, String actorRole) {
        CertificateSignatory signatory = signatoryRepository.findByIdAndIsDeletedFalse(signatoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CertificateSignatory", "id", signatoryId));

        // Soft delete (REVOKE-style: no hard delete of audit-relevant history)
        int removedLinks = 0;
        for (CertificateTemplateSignatory link : templateSignatoryRepository.findBySignatoryId(signatoryId)) {
            link.setIsDeleted(true);
            templateSignatoryRepository.save(link);
            removedLinks++;
        }
        signatory.setIsDeleted(true);
        signatoryRepository.save(signatory);

        writeAudit(signatory.getInstitutionId(), actorId, actorEmail, actorRole,
                "CertificateSignatory", signatoryId, signatory.getFullName(),
                AuditLog.AuditAction.DELETE, authorizationSnapshot(signatory), null);

        Map<String, Object> out = new HashMap<>();
        out.put("id", signatoryId);
        out.put("deleted", true);
        out.put("linksRemoved", removedLinks);
        log.info("Soft-deleted certificate signatory {} ({} template links removed)", signatoryId, removedLinks);
        return out;
    }

    // ───────────────────────────────────────────────────────────────────────
    // Templates
    // ───────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<PlatformTemplateResponse> listTemplates(String search, String type, UUID institutionId,
                                                                int page, int size) {
        CertificateTemplate.TemplateType typeEnum = null;
        if (type != null && !type.isBlank()) {
            try {
                typeEnum = CertificateTemplate.TemplateType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                typeEnum = null;
            }
        }
        String searchTrim = (search == null || search.isBlank()) ? null : search.trim();

        Page<CertificateTemplate> result = templateRepository.searchTemplates(
                searchTrim, typeEnum, institutionId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<CertificateTemplate> templates = result.getContent();
        Map<UUID, String> institutionNames = resolveInstitutionNames(
                templates.stream().map(CertificateTemplate::getInstitutionId).toList());

        // Linked signatories for the whole page (one query for links + one for signatories)
        List<CertificateTemplateSignatory> links =
                templateSignatoryRepository.findByTemplateIdIn(templates.stream().map(CertificateTemplate::getId).toList());
        Set<UUID> signatoryIds = new HashSet<>();
        links.forEach(l -> signatoryIds.add(l.getSignatoryId()));
        Map<UUID, CertificateSignatory> signatoryMap = new HashMap<>();
        if (!signatoryIds.isEmpty()) {
            signatoryRepository.findAllById(signatoryIds).forEach(s -> signatoryMap.put(s.getId(), s));
        }

        Map<UUID, List<CertificateTemplateSignatory>> linksByTemplate = new HashMap<>();
        links.forEach(l -> linksByTemplate.computeIfAbsent(l.getTemplateId(), k -> new ArrayList<>()).add(l));
        linksByTemplate.values().forEach(list -> list.sort(Comparator.comparing(CertificateTemplateSignatory::getDisplayOrder)));

        List<PlatformTemplateResponse> content = templates.stream().map(t -> {
            List<SignatoryResponse> templateSignatories = linksByTemplate.getOrDefault(t.getId(), List.of()).stream()
                    .map(l -> signatoryMap.get(l.getSignatoryId()))
                    .filter(Objects::nonNull)
                    .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                    .map(s -> toSignatoryResponse(s, institutionNames))
                    .toList();
            return PlatformTemplateResponse.builder()
                    .id(t.getId())
                    .institutionId(t.getInstitutionId())
                    .institutionName(institutionNames.get(t.getInstitutionId()))
                    .name(t.getName())
                    .description(t.getDescription())
                    .templateType(t.getTemplateType() != null ? t.getTemplateType().name() : null)
                    .htmlContent(t.getHtmlContent())
                    .cssContent(t.getCssContent())
                    .logoUrl(t.getLogoUrl())
                    .signatureLine1(t.getSignatureLine1())
                    .signatureLine2(t.getSignatureLine2())
                    .signatureLine3(t.getSignatureLine3())
                    .isActive(t.getIsActive())
                    .version(t.getVersion())
                    .usageCount(certificateRepository.countByTemplateIdAndIsDeletedFalse(t.getId()))
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt())
                    .signatories(templateSignatories)
                    .build();
        }).toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }

    public PlatformTemplateResponse createTemplate(CertificateTemplateRequest req,
                                                   UUID actorId, String actorEmail, String actorRole) {
        if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Template name is required");
        }
        CertificateTemplate.TemplateType type = parseTemplateType(req.getTemplateType());
        if (req.getInstitutionId() == null) {
            throw new IllegalArgumentException("Owning institution is required");
        }
        Institution institution = institutionRepository.findById(req.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", req.getInstitutionId()));
        if (templateRepository.existsByNameAndInstitutionIdAndIsDeletedFalse(req.getName().trim(), institution.getId())) {
            throw new IllegalArgumentException("Template with name '" + req.getName().trim() + "' already exists for this institution");
        }

        CertificateTemplate template = CertificateTemplate.builder()
                .name(req.getName().trim())
                .description(trimToNull(req.getDescription()))
                .templateType(type)
                .htmlContent(req.getHtmlContent())
                .cssContent(req.getCssContent())
                .logoUrl(trimToNull(req.getLogoUrl()))
                .signatureLine1(trimToNull(req.getSignatureLine1()))
                .signatureLine2(trimToNull(req.getSignatureLine2()))
                .signatureLine3(trimToNull(req.getSignatureLine3()))
                .isActive(true)
                .version(1)
                .build();
        template.setInstitutionId(institution.getId());
        template = templateRepository.save(template);

        writeAudit(institution.getId(), actorId, actorEmail, actorRole,
                "CertificateTemplate", template.getId(), template.getName(),
                AuditLog.AuditAction.CREATE, null,
                Map.of("name", template.getName(), "type", type.name(), "version", 1, "isActive", true));

        log.info("Created certificate template {} for institution {}", template.getId(), institution.getId());
        return toPlatformTemplateResponse(template, Map.of(), List.of());
    }

    public PlatformTemplateResponse updateTemplate(UUID templateId, CertificateTemplateRequest req,
                                                   UUID actorId, String actorEmail, String actorRole) {
        CertificateTemplate template = templateRepository.findByIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("CertificateTemplate", "id", templateId));
        if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Template name is required");
        }

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("name", template.getName());
        oldValues.put("type", template.getTemplateType() != null ? template.getTemplateType().name() : null);
        oldValues.put("version", template.getVersion());

        // Archive the current content so previous versions keep coexisting
        archiveCurrentVersion(template, actorEmail);

        template.setName(req.getName().trim());
        template.setDescription(trimToNull(req.getDescription()));
        if (req.getTemplateType() != null && !req.getTemplateType().isBlank()) {
            template.setTemplateType(parseTemplateType(req.getTemplateType()));
        }
        if (req.getHtmlContent() != null) template.setHtmlContent(req.getHtmlContent());
        if (req.getCssContent() != null) template.setCssContent(req.getCssContent());
        if (req.getLogoUrl() != null) template.setLogoUrl(trimToNull(req.getLogoUrl()));
        if (req.getSignatureLine1() != null) template.setSignatureLine1(trimToNull(req.getSignatureLine1()));
        if (req.getSignatureLine2() != null) template.setSignatureLine2(trimToNull(req.getSignatureLine2()));
        if (req.getSignatureLine3() != null) template.setSignatureLine3(trimToNull(req.getSignatureLine3()));
        template = templateRepository.save(template);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("name", template.getName());
        newValues.put("type", template.getTemplateType() != null ? template.getTemplateType().name() : null);
        newValues.put("version", template.getVersion());
        writeAudit(template.getInstitutionId(), actorId, actorEmail, actorRole,
                "CertificateTemplate", template.getId(), template.getName(),
                AuditLog.AuditAction.UPDATE, oldValues, newValues);

        log.info("Updated certificate template {} → version {}", templateId, template.getVersion());
        return toPlatformTemplateResponse(template, Map.of(), List.of());
    }

    public PlatformTemplateResponse setTemplateStatus(UUID templateId, Boolean isActive,
                                                      UUID actorId, String actorEmail, String actorRole) {
        if (isActive == null) {
            throw new IllegalArgumentException("isActive is required");
        }
        CertificateTemplate template = templateRepository.findByIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("CertificateTemplate", "id", templateId));
        boolean old = Boolean.TRUE.equals(template.getIsActive());
        template.setIsActive(isActive);
        template = templateRepository.save(template);

        writeAudit(template.getInstitutionId(), actorId, actorEmail, actorRole,
                "CertificateTemplate", template.getId(), template.getName(),
                AuditLog.AuditAction.UPDATE,
                Map.of("isActive", old), Map.of("isActive", isActive));

        log.info("Template {} isActive: {} → {}", templateId, old, isActive);
        return toPlatformTemplateResponse(template, Map.of(), List.of());
    }

    @Transactional(readOnly = true)
    public List<SignatoryResponse> getTemplateSignatories(UUID templateId) {
        CertificateTemplate template = templateRepository.findByIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("CertificateTemplate", "id", templateId));
        Map<UUID, String> institutionNames = resolveInstitutionNames(List.of(template.getInstitutionId()));
        List<CertificateTemplateSignatory> links = templateSignatoryRepository.findByTemplateId(templateId);
        Map<UUID, CertificateSignatory> signatoryMap = loadSignatories(links);
        return links.stream()
                .map(l -> signatoryMap.get(l.getSignatoryId()))
                .filter(Objects::nonNull)
                .map(s -> toSignatoryResponse(s, institutionNames))
                .toList();
    }

    public List<SignatoryResponse> replaceTemplateSignatories(UUID templateId, TemplateSignatoriesRequest req,
                                                              UUID actorId, String actorEmail, String actorRole) {
        CertificateTemplate template = templateRepository.findByIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("CertificateTemplate", "id", templateId));

        List<UUID> ids = req == null || req.getSignatoryIds() == null ? List.of()
                : req.getSignatoryIds().stream().filter(Objects::nonNull).distinct().toList();

        List<CertificateSignatory> signatories = ids.isEmpty() ? List.of() : signatoryRepository.findAllById(ids);
        if (signatories.size() != ids.size()) {
            throw new ResourceNotFoundException("CertificateSignatory", "id", "one or more signatories not found");
        }
        LocalDate today = LocalDate.now();
        for (CertificateSignatory signatory : signatories) {
            assertAuthorizedForTemplate(signatory, template, today);
        }

        // Audit old set before replacing
        List<UUID> oldIds = templateSignatoryRepository.findByTemplateId(templateId).stream()
                .map(CertificateTemplateSignatory::getSignatoryId).toList();

        // Deactivate every currently active link; kept pairs are reactivated below.
        for (CertificateTemplateSignatory link : templateSignatoryRepository.findByTemplateId(templateId)) {
            link.setIsDeleted(true);
            templateSignatoryRepository.save(link);
        }
        // Re-linking must reuse the existing row for a (template, signatory) pair even when that
        // row was soft-deleted earlier: UNIQUE (template_id, signatory_id) applies to every row
        // regardless of is_deleted, and a fresh INSERT would violate it (Hibernate also flushes
        // inserts before updates, so soft-deleting first would not help).
        Map<UUID, CertificateTemplateSignatory> existingBySignatory = new HashMap<>();
        templateSignatoryRepository.findAllByTemplateIdIncludingDeleted(templateId)
                .forEach(l -> existingBySignatory.put(l.getSignatoryId(), l));
        int order = 0;
        for (UUID id : ids) {
            CertificateTemplateSignatory link = existingBySignatory.get(id);
            if (link != null) {
                link.setIsDeleted(false);
                link.setDisplayOrder(order++);
                link.setInstitutionId(template.getInstitutionId());
                templateSignatoryRepository.save(link);
            } else {
                link = CertificateTemplateSignatory.builder()
                        .templateId(templateId)
                        .signatoryId(id)
                        .displayOrder(order++)
                        .build();
                link.setInstitutionId(template.getInstitutionId());
                templateSignatoryRepository.save(link);
            }
        }

        writeAudit(template.getInstitutionId(), actorId, actorEmail, actorRole,
                "CertificateTemplate", templateId, template.getName(),
                AuditLog.AuditAction.UPDATE,
                Map.of("signatoryIds", oldIds.stream().map(UUID::toString).toList()),
                Map.of("signatoryIds", ids.stream().map(UUID::toString).toList()));

        log.info("Template {} signatories set to {} (was {})", templateId, ids.size(), oldIds.size());
        Map<UUID, String> institutionNames = resolveInstitutionNames(List.of(template.getInstitutionId()));
        Map<UUID, CertificateSignatory> byId = new HashMap<>();
        signatories.forEach(s -> byId.put(s.getId(), s));
        return ids.stream().map(byId::get).filter(Objects::nonNull)
                .map(s -> toSignatoryResponse(s, institutionNames)).toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateVersionResponse> getTemplateVersions(UUID templateId) {
        // Archived versions (v1, v2, ...) so previous template content stays visible.
        templateRepository.findByIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("CertificateTemplate", "id", templateId));
        return templateVersionRepository.findByTemplateIdAndIsDeletedFalseOrderByVersionDesc(templateId).stream()
                .map(v -> TemplateVersionResponse.builder()
                        .id(v.getId())
                        .templateId(v.getTemplateId())
                        .version(v.getVersion())
                        .name(v.getName())
                        .templateType(v.getTemplateType())
                        .description(v.getDescription())
                        .htmlContent(v.getHtmlContent())
                        .cssContent(v.getCssContent())
                        .logoUrl(v.getLogoUrl())
                        .archivedAt(v.getArchivedAt())
                        .archivedBy(v.getArchivedBy())
                        .build())
                .toList();
    }

    // ───────────────────────────────────────────────────────────────────────
    // Certificate detail + overview
    // ───────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CertificateDetailResponse getCertificateDetail(UUID certificateId) {
        Certificate certificate = certificateRepository.findByIdAndIsDeletedFalse(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));

        String institutionName = certificate.getInstitutionId() == null ? null
                : institutionRepository.findById(certificate.getInstitutionId())
                        .map(Institution::getName).orElse(null);

        CertificateTemplate template = certificate.getTemplateId() == null ? null
                : templateRepository.findByIdAndIsDeletedFalse(certificate.getTemplateId()).orElse(null);

        List<SignatoryResponse> signatories = List.of();
        if (template != null) {
            Map<UUID, String> names = resolveInstitutionNames(List.of(template.getInstitutionId()));
            Map<UUID, CertificateSignatory> map = loadSignatories(
                    templateSignatoryRepository.findByTemplateId(template.getId()));
            signatories = templateSignatoryRepository.findByTemplateId(template.getId()).stream()
                    .map(l -> map.get(l.getSignatoryId()))
                    .filter(Objects::nonNull)
                    .map(s -> toSignatoryResponse(s, names))
                    .toList();
        }

        return CertificateDetailResponse.builder()
                .certificate(certificateMapper.toCertificateResponse(certificate))
                .institutionName(institutionName)
                .templateId(template != null ? template.getId() : certificate.getTemplateId())
                .templateName(template != null ? template.getName() : null)
                .templateVersion(template != null ? template.getVersion() : null)
                .templateIsActive(template != null ? template.getIsActive() : null)
                .signatories(signatories)
                .build();
    }

    @Transactional(readOnly = true)
    public CertificateOverviewResponse overview() {
        long total = certificateRepository.countByIsDeletedFalse();
        long issued = certificateRepository.countByStatusAndIsDeletedFalse(Certificate.CertificateStatus.ISSUED);
        long revoked = certificateRepository.countByStatusAndIsDeletedFalse(Certificate.CertificateStatus.REVOKED);
        long draft = certificateRepository.countByStatusAndIsDeletedFalse(Certificate.CertificateStatus.DRAFT);

        Map<String, Long> byType = new LinkedHashMap<>();
        for (Certificate.CertificateType type : Certificate.CertificateType.values()) {
            byType.put(type.name(), certificateRepository.countByCertificateTypeAndIsDeletedFalse(type));
        }

        long verifications = auditLogRepository.countByEntityTypeAndActionSince(
                "Certificate", AuditLog.AuditAction.VIEW, LocalDateTime.now().minusDays(30));

        return CertificateOverviewResponse.builder()
                .total(total)
                .issued(issued)
                .revoked(revoked)
                .draft(draft)
                .byType(byType)
                .verificationActivity30Days(verifications)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ───────────────────────────────────────────────────────────────────────
    // Validation helpers
    // ───────────────────────────────────────────────────────────────────────

    private void validateSignatureImage(String image) {
        if (image == null || image.isBlank()) return;
        String img = image.trim();
        boolean dataUrl = img.regionMatches(true, 0, "data:image/", 0, 11) && img.contains(";base64,");
        if (dataUrl) {
            if (img.length() > MAX_SIGNATURE_IMAGE_CHARS) {
                throw new IllegalArgumentException("Signature image is too large (max ~200 KB)");
            }
        } else if (img.startsWith("https://")) {
            if (img.length() > 500) {
                throw new IllegalArgumentException("Signature image URL is too long (max 500 characters)");
            }
        } else {
            throw new IllegalArgumentException("Signature must be an image data URL (data:image/...;base64,...) or an https:// URL");
        }
    }

    private String normalizeTypes(List<String> types) {
        if (types == null || types.isEmpty()) return null;
        List<String> out = new ArrayList<>();
        for (String t : types) {
            if (t == null || t.isBlank()) continue;
            String normalized = t.trim().toUpperCase();
            try {
                Certificate.CertificateType.valueOf(normalized);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown certificate type: " + t);
            }
            if (!out.contains(normalized)) out.add(normalized);
        }
        return out.isEmpty() ? null : String.join(",", out);
    }

    private void validateDates(LocalDate from, LocalDate until) {
        if (from != null && until != null && from.isAfter(until)) {
            throw new IllegalArgumentException("Valid-from date must not be after valid-until date");
        }
    }

    private UUID validateInstitutionScope(UUID institutionId) {
        if (institutionId == null) return null;
        institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", institutionId));
        return institutionId;
    }

    private CertificateSignatory.SignatoryStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return CertificateSignatory.SignatoryStatus.ACTIVE;
        try {
            return CertificateSignatory.SignatoryStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown signatory status: " + status);
        }
    }

    private CertificateTemplate.TemplateType parseTemplateType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Template type is required");
        }
        try {
            return CertificateTemplate.TemplateType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown template type: " + type);
        }
    }

    /** Signatory must be authorised to sign this template's certificates (authorisation rules). */
    private void assertAuthorizedForTemplate(CertificateSignatory signatory, CertificateTemplate template, LocalDate today) {
        if (signatory.getStatus() != CertificateSignatory.SignatoryStatus.ACTIVE) {
            throw new IllegalArgumentException("Signatory '" + signatory.getFullName() + "' is INACTIVE and cannot sign certificates");
        }
        if (signatory.getInstitutionId() != null && !signatory.getInstitutionId().equals(template.getInstitutionId())) {
            throw new IllegalArgumentException("Signatory '" + signatory.getFullName() + "' is scoped to another institution");
        }
        if (signatory.getCertificateTypes() != null && template.getTemplateType() != null
                && !signatory.getCertificateTypes().contains(template.getTemplateType().name())) {
            throw new IllegalArgumentException("Signatory '" + signatory.getFullName() + "' is not authorised for "
                    + template.getTemplateType().name() + " certificates");
        }
        if (signatory.getValidFrom() != null && today.isBefore(signatory.getValidFrom())) {
            throw new IllegalArgumentException("Signatory '" + signatory.getFullName() + "' is not yet valid (from "
                    + signatory.getValidFrom() + ")");
        }
        if (signatory.getValidUntil() != null && today.isAfter(signatory.getValidUntil())) {
            throw new IllegalArgumentException("Signatory '" + signatory.getFullName() + "' validity ended on "
                    + signatory.getValidUntil());
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ───────────────────────────────────────────────────────────────────────

    private void archiveCurrentVersion(CertificateTemplate template, String actorEmail) {
        int currentVersion = template.getVersion() != null ? template.getVersion() : 1;
        try {
            CertificateTemplateVersion archive = CertificateTemplateVersion.builder()
                    .templateId(template.getId())
                    .version(currentVersion)
                    .name(template.getName())
                    .templateType(template.getTemplateType() != null ? template.getTemplateType().name() : "CUSTOM")
                    .description(template.getDescription())
                    .htmlContent(template.getHtmlContent())
                    .cssContent(template.getCssContent())
                    .logoUrl(template.getLogoUrl())
                    .archivedAt(LocalDateTime.now())
                    .archivedBy(actorEmail)
                    .build();
            archive.setInstitutionId(template.getInstitutionId());
            templateVersionRepository.save(archive);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // The version was archived before — keep moving forward instead of failing the edit.
            log.warn("Template {} version {} already archived, continuing", template.getId(), currentVersion);
        }
        template.setVersion(currentVersion + 1);
    }

    private Map<String, Object> authorizationSnapshot(CertificateSignatory s) {
        Map<String, Object> snap = new HashMap<>();
        snap.put("fullName", s.getFullName());
        snap.put("status", s.getStatus() != null ? s.getStatus().name() : null);
        snap.put("certificateTypes", s.getCertificateTypes() != null ? s.getCertificateTypes() : "ALL");
        snap.put("institutionScope", s.getInstitutionId() != null ? s.getInstitutionId().toString() : "PLATFORM");
        snap.put("validFrom", s.getValidFrom() != null ? s.getValidFrom().toString() : null);
        snap.put("validUntil", s.getValidUntil() != null ? s.getValidUntil().toString() : null);
        return snap;
    }

    private void writeAudit(UUID institutionId, UUID actorId, String actorEmail, String actorRole,
                            String entityType, UUID entityId, String entityName, AuditLog.AuditAction action,
                            Map<String, Object> oldValues, Map<String, Object> newValues) {
        try {
            auditService.recordAuditLog(
                    institutionId != null ? institutionId : PLATFORM_INSTITUTION_ID,
                    actorId, actorEmail, actorRole,
                    entityType, entityId, entityName, action, oldValues, newValues);
        } catch (Exception e) {
            log.warn("Failed to write {} audit for {}: {}", action, entityType, e.getMessage());
        }
    }

    private Map<UUID, String> resolveInstitutionNames(List<UUID> institutionIds) {
        Map<UUID, String> names = new HashMap<>();
        institutionIds.stream().filter(Objects::nonNull).distinct()
                .forEach(id -> institutionRepository.findById(id)
                        .ifPresent(inst -> names.put(id, inst.getName())));
        return names;
    }

    private Map<UUID, CertificateSignatory> loadSignatories(List<CertificateTemplateSignatory> links) {
        Set<UUID> ids = new HashSet<>();
        links.forEach(l -> ids.add(l.getSignatoryId()));
        Map<UUID, CertificateSignatory> map = new HashMap<>();
        if (!ids.isEmpty()) {
            signatoryRepository.findAllById(ids).forEach(s -> map.put(s.getId(), s));
        }
        return map;
    }

    private SignatoryResponse toSignatoryResponse(CertificateSignatory s, Map<UUID, String> institutionNames) {
        List<String> types = null;
        if (s.getCertificateTypes() != null && !s.getCertificateTypes().isBlank()) {
            types = Arrays.asList(s.getCertificateTypes().split(","));
        }
        return SignatoryResponse.builder()
                .id(s.getId())
                .institutionId(s.getInstitutionId())
                .institutionName(institutionNames.get(s.getInstitutionId()))
                .fullName(s.getFullName())
                .positionTitle(s.getPositionTitle())
                .organization(s.getOrganization())
                .signatureImage(s.getSignatureImage())
                .certificateTypes(types)
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .validFrom(s.getValidFrom())
                .validUntil(s.getValidUntil())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private PlatformTemplateResponse toPlatformTemplateResponse(CertificateTemplate t,
                                                                Map<UUID, String> institutionNames,
                                                                List<SignatoryResponse> signatories) {
        return PlatformTemplateResponse.builder()
                .id(t.getId())
                .institutionId(t.getInstitutionId())
                .institutionName(institutionNames.get(t.getInstitutionId()))
                .name(t.getName())
                .description(t.getDescription())
                .templateType(t.getTemplateType() != null ? t.getTemplateType().name() : null)
                .htmlContent(t.getHtmlContent())
                .cssContent(t.getCssContent())
                .logoUrl(t.getLogoUrl())
                .signatureLine1(t.getSignatureLine1())
                .signatureLine2(t.getSignatureLine2())
                .signatureLine3(t.getSignatureLine3())
                .isActive(t.getIsActive())
                .version(t.getVersion())
                .usageCount(certificateRepository.countByTemplateIdAndIsDeletedFalse(t.getId()))
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .signatories(signatories)
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
