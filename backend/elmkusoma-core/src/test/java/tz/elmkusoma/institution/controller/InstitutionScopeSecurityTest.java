package tz.elmkusoma.institution.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.institution.dto.response.InstitutionResponse;
import tz.elmkusoma.institution.service.InstitutionService;
import tz.elmkusoma.shared.repository.UserRepository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code InstitutionController.assertWithinScope}: platform admins may touch any
 * institution, an INSTITUTION_ADMIN only the institution resolved server-side.
 * Uses standalone MockMvc (no Spring context) with the real GlobalExceptionHandler.
 */
class InstitutionScopeSecurityTest {

    private static final String SCOPE_ERROR = "You are not authorized to access this institution";

    private InstitutionService institutionService;
    private InstitutionController controller;
    private MockMvc mockMvc;

    private UUID targetId;
    private UUID ownInstitutionId;

    @BeforeEach
    void setUp() {
        institutionService = mock(InstitutionService.class);
        UserRepository userRepository = mock(UserRepository.class);
        controller = new InstitutionController(institutionService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        targetId = UUID.randomUUID();
        ownInstitutionId = UUID.randomUUID();
    }

    private InstitutionResponse sampleInstitution() {
        return InstitutionResponse.builder()
                .id(targetId)
                .name("Test Institution")
                .build();
    }

    // ── HTTP scope enforcement ──

    @Test
    void institutionAdminMayReadOwnInstitution() throws Exception {
        when(institutionService.getInstitution(targetId)).thenReturn(sampleInstitution());

        mockMvc.perform(get("/v1/institutions/{id}", targetId)
                        .requestAttr("institutionId", targetId)
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(institutionService).getInstitution(targetId);
    }

    @Test
    void institutionAdminMayNotReadAnotherInstitution() throws Exception {
        mockMvc.perform(get("/v1/institutions/{id}", targetId)
                        .requestAttr("institutionId", ownInstitutionId)
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(SCOPE_ERROR));

        verify(institutionService, never()).getInstitution(any());
    }

    @Test
    void platformAdminMayReadAnyInstitution() throws Exception {
        when(institutionService.getInstitution(targetId)).thenReturn(sampleInstitution());

        mockMvc.perform(get("/v1/institutions/{id}", targetId)
                        .requestAttr("institutionId", ownInstitutionId)
                        .requestAttr("userRole", "ADMIN"))
                .andExpect(status().isOk());

        verify(institutionService).getInstitution(targetId);
    }

    @Test
    void nationalAdminMayReadAnyInstitution() throws Exception {
        when(institutionService.getInstitution(targetId)).thenReturn(sampleInstitution());

        mockMvc.perform(get("/v1/institutions/{id}", targetId)
                        .requestAttr("institutionId", ownInstitutionId)
                        .requestAttr("userRole", "NATIONAL_ADMIN"))
                .andExpect(status().isOk());

        verify(institutionService).getInstitution(targetId);
    }

    @Test
    void institutionAdminMayNotUpdateAnotherInstitution() throws Exception {
        mockMvc.perform(put("/v1/institutions/{id}", targetId)
                        .requestAttr("institutionId", ownInstitutionId)
                        .requestAttr("userRole", "INSTITUTION_ADMIN")
                        .contentType("application/json")
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(SCOPE_ERROR));

        verify(institutionService, never()).updateInstitution(any(), any());
    }

    @Test
    void institutionAdminMayUpdateOwnInstitution() throws Exception {
        when(institutionService.updateInstitution(eq(targetId), any()))
                .thenReturn(sampleInstitution());

        mockMvc.perform(put("/v1/institutions/{id}", targetId)
                        .requestAttr("institutionId", targetId)
                        .requestAttr("userRole", "INSTITUTION_ADMIN")
                        .contentType("application/json")
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk());

        verify(institutionService).updateInstitution(eq(targetId), any());
    }

    @Test
    void securityExceptionFromServiceIsMappedTo403() throws Exception {
        when(institutionService.getInstitution(targetId))
                .thenThrow(new SecurityException(SCOPE_ERROR));

        mockMvc.perform(get("/v1/institutions/{id}", targetId)
                        .requestAttr("institutionId", ownInstitutionId)
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(SCOPE_ERROR));
    }

    @Test
    void resourceNotFoundFromServiceIsMappedTo404() throws Exception {
        when(institutionService.getInstitution(targetId))
                .thenThrow(new ResourceNotFoundException("Institution", "id", targetId));

        mockMvc.perform(get("/v1/institutions/{id}", targetId)
                        .requestAttr("institutionId", ownInstitutionId)
                        .requestAttr("userRole", "ADMIN"))
                .andExpect(status().isNotFound());
    }

    // ── direct coverage of assertWithinScope ──

    private Throwable invokeAssertWithinScope(UUID target, UUID ctxInstitutionId, String userRole) {
        try {
            Method method = InstitutionController.class.getDeclaredMethod(
                    "assertWithinScope", UUID.class, UUID.class, String.class);
            method.setAccessible(true);
            method.invoke(controller, target, ctxInstitutionId, userRole);
            return null;
        } catch (InvocationTargetException ex) {
            return ex.getCause();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void scopeCheck_rejectsInstitutionAdminWithNoContextInstitution() {
        Throwable cause = invokeAssertWithinScope(targetId, null, "INSTITUTION_ADMIN");

        assertInstanceOf(SecurityException.class, cause);
    }

    @Test
    void scopeCheck_rejectsInstitutionAdminOutsideOwnInstitution() {
        Throwable cause = invokeAssertWithinScope(targetId, ownInstitutionId, "INSTITUTION_ADMIN");

        assertInstanceOf(SecurityException.class, cause);
    }

    @Test
    void scopeCheck_allowsInstitutionAdminInsideOwnInstitution() {
        assertNull(invokeAssertWithinScope(ownInstitutionId, ownInstitutionId, "INSTITUTION_ADMIN"));
    }

    @Test
    void scopeCheck_platformAdminBypassesEvenWithoutContextInstitution() {
        assertNull(invokeAssertWithinScope(targetId, null, "ADMIN"));
        assertNull(invokeAssertWithinScope(targetId, null, "NATIONAL_ADMIN"));
        assertNull(invokeAssertWithinScope(targetId, ownInstitutionId, "NATIONAL_ADMIN"));
    }

    @Test
    void scopeCheck_rejectsNonPlatformRoleOutsideItsInstitution() {
        assertInstanceOf(SecurityException.class, invokeAssertWithinScope(targetId, ownInstitutionId, "TEACHER"));
        assertInstanceOf(SecurityException.class, invokeAssertWithinScope(targetId, null, "STUDENT"));
    }

    @Test
    void scopeCheck_isInstitutionBasedNotRoleBased() {
        // Role gating for this controller is done by @PreAuthorize; assertWithinScope
        // itself only compares institutions.
        assertNull(invokeAssertWithinScope(ownInstitutionId, ownInstitutionId, "TEACHER"));
    }
}
