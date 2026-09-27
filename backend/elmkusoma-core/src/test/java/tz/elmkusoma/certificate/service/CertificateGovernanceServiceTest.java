package tz.elmkusoma.certificate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.certificate.domain.*;
import tz.elmkusoma.certificate.dto.*;
import tz.elmkusoma.certificate.mapper.CertificateMapper;
import tz.elmkusoma.certificate.repository.*;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.repository.InstitutionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateGovernanceServiceTest {

    @Mock private CertificateSignatoryRepository signatoryRepository;
    @Mock private CertificateTemplateSignatoryRepository templateSignatoryRepository;
    @Mock private CertificateTemplateVersionRepository templateVersionRepository;
    @Mock private CertificateTemplateRepository templateRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditService auditService;
    @Mock private CertificateMapper certificateMapper;

    @InjectMocks
    private CertificateGovernanceService service;

    private UUID institutionId;
    private UUID templateId;
    private String actorEmail;
    private String actorRole;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        actorEmail = "admin@elmkusoma.go.tz";
        actorRole = "ADMIN";
    }

    private CertificateSignatory buildSignatory(UUID scopedInstitution, String types,
                                                CertificateSignatory.SignatoryStatus status,
                                                LocalDate validFrom, LocalDate validUntil) {
        CertificateSignatory s = CertificateSignatory.builder()
                .fullName("Dr. John Doe")
                .positionTitle("Programme Director")
                .organization("ELMKUSOMA")
                .certificateTypes(types)
                .status(status)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .build();
        s.setInstitutionId(scopedInstitution);
        return s;
    }

    private CertificateTemplate buildTemplate(CertificateTemplate.TemplateType type) {
        CertificateTemplate t = CertificateTemplate.builder()
                .name("Standard Completion")
                .templateType(type)
                .isActive(true)
                .version(1)
                .build();
        t.setId(templateId);
        t.setInstitutionId(institutionId);
        return t;
    }

    // ── Signatory creation ──

    @Test
    void createSignatory_success_persistsAndAudits() {
        SignatoryRequest req = SignatoryRequest.builder()
                .fullName("Dr. John Doe")
                .positionTitle("Programme Director")
                .organization("ELMKUSOMA")
                .signatureImage("data:image/png;base64,iVBORw0KGgo=")
                .certificateTypes(List.of("COMPLETION", "PARTICIPATION"))
                .status("ACTIVE")
                .build();
        when(signatoryRepository.save(any(CertificateSignatory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SignatoryResponse response = service.createSignatory(req, null, actorEmail, actorRole);

        assertEquals("Dr. John Doe", response.getFullName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(List.of("COMPLETION", "PARTICIPATION"), response.getCertificateTypes());
        assertNull(response.getInstitutionId()); // platform-wide scope
        verify(auditService).recordAuditLog(any(), any(), any(), any(), any(), any(), any(),
                eq(AuditLog.AuditAction.CREATE), isNull(), any());
    }

    @Test
    void createSignatory_blankName_rejected() {
        SignatoryRequest req = SignatoryRequest.builder().fullName("   ").build();
        assertThrows(IllegalArgumentException.class,
                () -> service.createSignatory(req, null, actorEmail, actorRole));
        verify(signatoryRepository, never()).save(any());
    }

    @Test
    void createSignatory_filePathSignature_rejected() {
        SignatoryRequest req = SignatoryRequest.builder()
                .fullName("Dr. Jane Doe")
                .signatureImage("C:\\signatures\\jane.png")
                .build();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createSignatory(req, null, actorEmail, actorRole));
        assertTrue(ex.getMessage().contains("data URL"));
        verify(signatoryRepository, never()).save(any());
    }

    @Test
    void createSignatory_unknownCertificateType_rejected() {
        SignatoryRequest req = SignatoryRequest.builder()
                .fullName("Dr. Jane Doe")
                .certificateTypes(List.of("NOT_A_TYPE"))
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> service.createSignatory(req, null, actorEmail, actorRole));
    }

    @Test
    void createSignatory_invertedValidityDates_rejected() {
        SignatoryRequest req = SignatoryRequest.builder()
                .fullName("Dr. Jane Doe")
                .validFrom(LocalDate.of(2026, 12, 31))
                .validUntil(LocalDate.of(2026, 1, 1))
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> service.createSignatory(req, null, actorEmail, actorRole));
    }

    @Test
    void createSignatory_unknownInstitutionScope_notFound() {
        UUID unknown = UUID.randomUUID();
        when(institutionRepository.findById(unknown)).thenReturn(Optional.empty());
        SignatoryRequest req = SignatoryRequest.builder()
                .fullName("Dr. Jane Doe")
                .institutionId(unknown)
                .build();
        assertThrows(ResourceNotFoundException.class,
                () -> service.createSignatory(req, null, actorEmail, actorRole));
    }

    // ── Signatory deletion ──

    @Test
    void deleteSignatory_softDeletesAndUnlinks() {
        CertificateSignatory signatory = buildSignatory(institutionId, null,
                CertificateSignatory.SignatoryStatus.ACTIVE, null, null);
        signatory.setId(UUID.randomUUID());
        when(signatoryRepository.findByIdAndIsDeletedFalse(signatory.getId()))
                .thenReturn(Optional.of(signatory));
        CertificateTemplateSignatory link = CertificateTemplateSignatory.builder()
                .templateId(templateId).signatoryId(signatory.getId()).displayOrder(0).build();
        when(templateSignatoryRepository.findBySignatoryId(signatory.getId()))
                .thenReturn(List.of(link));

        Map<String, Object> out = service.deleteSignatory(signatory.getId(), null, actorEmail, actorRole);

        assertEquals(true, out.get("deleted"));
        assertEquals(1, out.get("linksRemoved"));
        assertTrue(Boolean.TRUE.equals(signatory.getIsDeleted()));
        assertTrue(Boolean.TRUE.equals(link.getIsDeleted()));
        verify(signatoryRepository).save(signatory);
        verify(templateSignatoryRepository).save(link);
        verify(auditService).recordAuditLog(any(), any(), any(), any(), any(), any(), any(),
                eq(AuditLog.AuditAction.DELETE), any(), isNull());
    }

    // ── Template ↔ signatory authorisation ──

    @Test
    void replaceTemplateSignatories_inactiveSignatory_rejected() {
        CertificateTemplate template = buildTemplate(CertificateTemplate.TemplateType.COMPLETION);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        UUID signatoryId = UUID.randomUUID();
        CertificateSignatory inactive = buildSignatory(institutionId, null,
                CertificateSignatory.SignatoryStatus.INACTIVE, null, null);
        inactive.setId(signatoryId);
        when(signatoryRepository.findAllById(List.of(signatoryId))).thenReturn(List.of(inactive));

        assertThrows(IllegalArgumentException.class, () -> service.replaceTemplateSignatories(
                templateId, TemplateSignatoriesRequest.builder().signatoryIds(List.of(signatoryId)).build(),
                null, actorEmail, actorRole));
        verify(templateSignatoryRepository, never()).save(any());
    }

    @Test
    void replaceTemplateSignatories_institutionScopeMismatch_rejected() {
        CertificateTemplate template = buildTemplate(CertificateTemplate.TemplateType.COMPLETION);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        UUID signatoryId = UUID.randomUUID();
        CertificateSignatory otherInstitution = buildSignatory(UUID.randomUUID(), null,
                CertificateSignatory.SignatoryStatus.ACTIVE, null, null);
        otherInstitution.setId(signatoryId);
        when(signatoryRepository.findAllById(List.of(signatoryId))).thenReturn(List.of(otherInstitution));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.replaceTemplateSignatories(
                templateId, TemplateSignatoriesRequest.builder().signatoryIds(List.of(signatoryId)).build(),
                null, actorEmail, actorRole));
        assertTrue(ex.getMessage().contains("institution"));
    }

    @Test
    void replaceTemplateSignatories_typeScopeMismatch_rejected() {
        CertificateTemplate template = buildTemplate(CertificateTemplate.TemplateType.COMPLETION);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        UUID signatoryId = UUID.randomUUID();
        CertificateSignatory participationOnly = buildSignatory(institutionId, "PARTICIPATION",
                CertificateSignatory.SignatoryStatus.ACTIVE, null, null);
        participationOnly.setId(signatoryId);
        when(signatoryRepository.findAllById(List.of(signatoryId))).thenReturn(List.of(participationOnly));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.replaceTemplateSignatories(
                templateId, TemplateSignatoriesRequest.builder().signatoryIds(List.of(signatoryId)).build(),
                null, actorEmail, actorRole));
        assertTrue(ex.getMessage().contains("not authorised"));
    }

    @Test
    void replaceTemplateSignatories_expiredValidity_rejected() {
        CertificateTemplate template = buildTemplate(CertificateTemplate.TemplateType.COMPLETION);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        UUID signatoryId = UUID.randomUUID();
        CertificateSignatory expired = buildSignatory(institutionId, null,
                CertificateSignatory.SignatoryStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
        expired.setId(signatoryId);
        when(signatoryRepository.findAllById(List.of(signatoryId))).thenReturn(List.of(expired));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.replaceTemplateSignatories(
                templateId, TemplateSignatoriesRequest.builder().signatoryIds(List.of(signatoryId)).build(),
                null, actorEmail, actorRole));
        assertTrue(ex.getMessage().contains("validity ended"));
    }

    @Test
    void replaceTemplateSignatories_success_replacesLinksAndAudits() {
        CertificateTemplate template = buildTemplate(CertificateTemplate.TemplateType.COMPLETION);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        UUID newSignatoryId = UUID.randomUUID();
        UUID oldSignatoryId = UUID.randomUUID();
        CertificateSignatory active = buildSignatory(institutionId, "COMPLETION",
                CertificateSignatory.SignatoryStatus.ACTIVE, null, null);
        active.setId(newSignatoryId);
        when(signatoryRepository.findAllById(List.of(newSignatoryId))).thenReturn(List.of(active));

        CertificateTemplateSignatory oldLink = CertificateTemplateSignatory.builder()
                .templateId(templateId).signatoryId(oldSignatoryId).displayOrder(0).build();
        when(templateSignatoryRepository.findByTemplateId(templateId))
                .thenReturn(List.of(oldLink))
                .thenReturn(List.of(oldLink));
        when(templateSignatoryRepository.save(any(CertificateTemplateSignatory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<SignatoryResponse> out = service.replaceTemplateSignatories(
                templateId, TemplateSignatoriesRequest.builder().signatoryIds(List.of(newSignatoryId)).build(),
                null, actorEmail, actorRole);

        assertEquals(1, out.size());
        assertEquals("Dr. John Doe", out.get(0).getFullName());
        assertTrue(Boolean.TRUE.equals(oldLink.getIsDeleted())); // previous link soft-deleted
        // old link soft-delete + new link insert
        verify(templateSignatoryRepository, times(2)).save(any(CertificateTemplateSignatory.class));
        verify(auditService).recordAuditLog(any(), any(), any(), any(), any(), any(), any(),
                eq(AuditLog.AuditAction.UPDATE), any(), any());
    }

    @Test
    void replaceTemplateSignatories_reactivatesPreviouslyRemovedLink() {
        CertificateTemplate template = buildTemplate(CertificateTemplate.TemplateType.COMPLETION);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        UUID signatoryId = UUID.randomUUID();
        CertificateSignatory active = buildSignatory(institutionId, "COMPLETION",
                CertificateSignatory.SignatoryStatus.ACTIVE, null, null);
        active.setId(signatoryId);
        when(signatoryRepository.findAllById(List.of(signatoryId))).thenReturn(List.of(active));

        // The pair was linked before and soft-deleted later: only one physical row may exist
        // (UNIQUE (template_id, signatory_id)), so the row must be reactivated, not re-inserted.
        CertificateTemplateSignatory oldRow = CertificateTemplateSignatory.builder()
                .templateId(templateId).signatoryId(signatoryId).displayOrder(3).build();
        oldRow.setIsDeleted(true);
        when(templateSignatoryRepository.findByTemplateId(templateId)).thenReturn(List.of());
        when(templateSignatoryRepository.findAllByTemplateIdIncludingDeleted(templateId))
                .thenReturn(List.of(oldRow));
        when(templateSignatoryRepository.save(any(CertificateTemplateSignatory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<SignatoryResponse> out = service.replaceTemplateSignatories(
                templateId, TemplateSignatoriesRequest.builder().signatoryIds(List.of(signatoryId)).build(),
                null, actorEmail, actorRole);

        assertEquals(1, out.size());
        assertTrue(!Boolean.TRUE.equals(oldRow.getIsDeleted())); // link reactivated, not left deleted
        assertEquals(0, oldRow.getDisplayOrder()); // display order reset to the requested one
        verify(templateSignatoryRepository, times(1)).save(any(CertificateTemplateSignatory.class));
    }

    @Test
    void replaceTemplateSignatories_unknownSignatory_notFound() {
        CertificateTemplate template = buildTemplate(CertificateTemplate.TemplateType.COMPLETION);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        UUID unknown = UUID.randomUUID();
        when(signatoryRepository.findAllById(List.of(unknown))).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.replaceTemplateSignatories(
                templateId, TemplateSignatoriesRequest.builder().signatoryIds(List.of(unknown)).build(),
                null, actorEmail, actorRole));
    }

    // ── Template versioning ──

    @Test
    void updateTemplate_archivesPreviousVersionAndIncrements() {
        CertificateTemplate template = buildTemplate(CertificateTemplate.TemplateType.COMPLETION);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        when(templateVersionRepository.save(any(CertificateTemplateVersion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(templateRepository.save(any(CertificateTemplate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PlatformTemplateResponse out = service.updateTemplate(templateId,
                CertificateTemplateRequest.builder().name("Completion v2").templateType("COMPLETION").build(),
                null, actorEmail, actorRole);

        ArgumentCaptor<CertificateTemplateVersion> captor = ArgumentCaptor.forClass(CertificateTemplateVersion.class);
        verify(templateVersionRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getVersion());          // archived version = previous (v1)
        assertEquals("Standard Completion", captor.getValue().getName());
        assertNotNull(captor.getValue().getArchivedAt());
        assertEquals(2, out.getVersion());                        // live template now v2
        verify(auditService).recordAuditLog(any(), any(), any(), any(), any(), any(), any(),
                eq(AuditLog.AuditAction.UPDATE), any(), any());
    }

    @Test
    void setTemplateStatus_togglesAndAudits() {
        CertificateTemplate template = buildTemplate(CertificateTemplate.TemplateType.COMPLETION);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(CertificateTemplate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PlatformTemplateResponse out = service.setTemplateStatus(templateId, false, null, actorEmail, actorRole);

        assertEquals(Boolean.FALSE, out.getIsActive());
        verify(auditService).recordAuditLog(any(), any(), any(), any(), any(), any(), any(),
                eq(AuditLog.AuditAction.UPDATE), any(), any());
    }

    // ── Detail + overview ──

    @Test
    void getCertificateDetail_returnsCertificateWithTemplateSignatories() {
        UUID certificateId = UUID.randomUUID();
        Certificate certificate = Certificate.builder()
                .serialNumber("CERT-CMP-2026-000001")
                .certificateType(Certificate.CertificateType.COMPLETION)
                .title("Course Completion")
                .studentName("John Doe")
                .status(Certificate.CertificateStatus.ISSUED)
                .verificationCode("ABC123")
                .issueDate(LocalDateTime.now())
                .completionDate(LocalDate.of(2026, 1, 1))
                .build();
        certificate.setId(certificateId);
        certificate.setInstitutionId(institutionId);
        certificate.setTemplateId(templateId);

        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId)).thenReturn(Optional.of(certificate));
        when(institutionRepository.findById(institutionId))
                .thenReturn(Optional.of(Institution.builder().name("ELMKUSOMA HQ").build()));
        CertificateTemplate template = buildTemplate(CertificateTemplate.TemplateType.COMPLETION);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));

        UUID signatoryId = UUID.randomUUID();
        CertificateSignatory signatory = buildSignatory(institutionId, "COMPLETION",
                CertificateSignatory.SignatoryStatus.ACTIVE, null, null);
        signatory.setId(signatoryId);
        CertificateTemplateSignatory link = CertificateTemplateSignatory.builder()
                .templateId(templateId).signatoryId(signatoryId).displayOrder(0).build();
        when(templateSignatoryRepository.findByTemplateId(templateId)).thenReturn(List.of(link));
        when(signatoryRepository.findAllById(Set.of(signatoryId))).thenReturn(List.of(signatory));
        when(certificateMapper.toCertificateResponse(certificate))
                .thenReturn(CertificateResponse.builder().id(certificateId).serialNumber("CERT-CMP-2026-000001").build());

        CertificateDetailResponse detail = service.getCertificateDetail(certificateId);

        assertEquals("ELMKUSOMA HQ", detail.getInstitutionName());
        assertEquals("Standard Completion", detail.getTemplateName());
        assertEquals(1, detail.getTemplateVersion());
        assertEquals(1, detail.getSignatories().size());
        assertEquals("Dr. John Doe", detail.getSignatories().get(0).getFullName());
    }

    @Test
    void overview_returnsRealCountsOnly() {
        when(certificateRepository.countByIsDeletedFalse()).thenReturn(10L);
        when(certificateRepository.countByStatusAndIsDeletedFalse(Certificate.CertificateStatus.ISSUED)).thenReturn(8L);
        when(certificateRepository.countByStatusAndIsDeletedFalse(Certificate.CertificateStatus.REVOKED)).thenReturn(1L);
        when(certificateRepository.countByStatusAndIsDeletedFalse(Certificate.CertificateStatus.DRAFT)).thenReturn(1L);
        when(certificateRepository.countByCertificateTypeAndIsDeletedFalse(any(Certificate.CertificateType.class)))
                .thenReturn(2L);
        when(auditLogRepository.countByEntityTypeAndActionSince(eq("Certificate"),
                eq(AuditLog.AuditAction.VIEW), any(LocalDateTime.class))).thenReturn(5L);

        CertificateOverviewResponse overview = service.overview();

        assertEquals(10L, overview.getTotal());
        assertEquals(8L, overview.getIssued());
        assertEquals(1L, overview.getRevoked());
        assertEquals(1L, overview.getDraft());
        assertEquals(4, overview.getByType().size()); // one entry per real enum value
        assertEquals(5L, overview.getVerificationActivity30Days());
        assertNotNull(overview.getGeneratedAt());
    }

    // ── Create template ──

    @Test
    void createTemplate_requiresOwningInstitution() {
        CertificateTemplateRequest req = CertificateTemplateRequest.builder()
                .name("New Template")
                .templateType("COMPLETION")
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> service.createTemplate(req, null, actorEmail, actorRole));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void createTemplate_success_forInstitution() {
        UUID inst = UUID.randomUUID();
        when(institutionRepository.findById(inst))
                .thenReturn(Optional.of(Institution.builder().id(inst).name("DARMS").build()));
        when(templateRepository.existsByNameAndInstitutionIdAndIsDeletedFalse("New Template", inst)).thenReturn(false);
        when(templateRepository.save(any(CertificateTemplate.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(certificateRepository.countByTemplateIdAndIsDeletedFalse(any())).thenReturn(0L);

        CertificateTemplateRequest req = CertificateTemplateRequest.builder()
                .name("New Template")
                .templateType("COMPLETION")
                .institutionId(inst)
                .build();

        PlatformTemplateResponse out = service.createTemplate(req, null, actorEmail, actorRole);

        assertEquals("New Template", out.getName());
        assertEquals(1, out.getVersion());
        assertEquals(Boolean.TRUE, out.getIsActive());
        assertEquals(0L, out.getUsageCount());
        verify(auditService).recordAuditLog(any(), any(), any(), any(), any(), any(), any(),
                eq(AuditLog.AuditAction.CREATE), isNull(), any());
    }
}
