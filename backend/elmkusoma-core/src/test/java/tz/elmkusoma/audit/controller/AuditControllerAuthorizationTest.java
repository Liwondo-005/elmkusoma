package tz.elmkusoma.audit.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import tz.elmkusoma.audit.dto.AuditLogResponse;
import tz.elmkusoma.audit.mapper.AuditMapper;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Nationaladmin.md §22 (audit row 1.7): NATIONAL_ADMIN may read compliance,
 * logs and feeds; write stays ADMIN/INSTITUTION_ADMIN. Institution-scoped
 * callers never widen their scope — a missing institution is refused.
 */
class AuditControllerAuthorizationTest {

    private AuditService auditService;
    private AuditController controller;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        controller = new AuditController(auditService, mock(AuditMapper.class));
    }

    private String classGate() {
        PreAuthorize gate = AuditController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(gate);
        return gate.value();
    }

    private String methodGate(String name, Class<?>... params) throws NoSuchMethodException {
        Method method = AuditController.class.getDeclaredMethod(name, params);
        PreAuthorize gate = method.getAnnotation(PreAuthorize.class);
        assertNotNull(gate, name + " must carry its own @PreAuthorize");
        return gate.value();
    }

    @Test
    void classGate_allowsNationalAdminReads() {
        String gate = classGate();
        assertTrue(gate.contains("'ADMIN'"));
        assertTrue(gate.contains("'INSTITUTION_ADMIN'"));
        assertTrue(gate.contains("'NATIONAL_ADMIN'"));
    }

    @Test
    void resolveSecurityEvent_staysAdminOnly() throws Exception {
        String gate = methodGate("resolveSecurityEvent", UUID.class, UUID.class, UUID.class);
        assertTrue(gate.contains("'ADMIN'"));
        assertTrue(gate.contains("'INSTITUTION_ADMIN'"));
        assertFalse(gate.contains("NATIONAL_ADMIN"));
    }

    @Test
    void nationalAdmin_readsPlatformWide() {
        UUID membershipInstitution = UUID.randomUUID();
        when(auditService.getAuditLogs(isNull(), eq(0), eq(20))).thenReturn(Page.empty());

        ResponseEntity<ApiResponse<List<AuditLogResponse>>> response =
                controller.getAuditLogs(membershipInstitution, "NATIONAL_ADMIN", null, null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        verify(auditService).getAuditLogs(null, 0, 20);
    }

    @Test
    void institutionAdmin_readsOwnInstitution() {
        UUID institutionId = UUID.randomUUID();
        when(auditService.getAuditLogs(eq(institutionId), eq(0), eq(20))).thenReturn(Page.empty());

        ResponseEntity<ApiResponse<List<AuditLogResponse>>> response =
                controller.getAuditLogs(institutionId, "INSTITUTION_ADMIN", null, null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        verify(auditService).getAuditLogs(institutionId, 0, 20);
        verify(auditService, never()).getAuditLogs(isNull(), anyInt(), anyInt());
    }

    @Test
    void institutionScopedCaller_withoutInstitution_rejected() {
        assertThrows(ForbiddenException.class,
                () -> controller.getAuditLogs(null, "INSTITUTION_ADMIN", null, null, 0, 20));

        verifyNoInteractions(auditService);
    }

    @Test
    void compliance_nationalAdmin_readsPlatformWide() {
        controller.getComplianceReport(UUID.randomUUID(), "NATIONAL_ADMIN");

        verify(auditService).getComplianceReport(null);
    }

    @Test
    void compliance_institutionAdmin_readsOwnInstitution() {
        UUID institutionId = UUID.randomUUID();

        controller.getComplianceReport(institutionId, "INSTITUTION_ADMIN");

        verify(auditService).getComplianceReport(institutionId);
    }
}
