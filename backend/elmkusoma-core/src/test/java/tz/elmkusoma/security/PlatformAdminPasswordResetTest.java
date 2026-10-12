package tz.elmkusoma.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Platform-admin password reset must actually work.
 * <p>
 * Regression guard: the service wrote an audit action ({@code RESET_PASSWORD}) that did
 * not exist in {@link AuditLog.AuditAction}, so every reset failed with HTTP 400
 * "No enum constant ... AuditAction.RESET_PASSWORD" Ã¢â‚¬â€ an admin could never reset a
 * password. The action column is a plain varchar, so the enum is the only contract.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class PlatformAdminPasswordResetTest {

    /**
     * The seeded "admin" user is an INSTITUTION_ADMIN, which is deliberately denied on
     * /v1/platform-admin/**. These tests exercise the platform surface, so they need a
     * genuine ADMIN-role token.
     */
    private static String platformAdminToken;

    @org.junit.jupiter.api.BeforeEach
    void ensurePlatformAdmin() {
        if (platformAdminToken != null) {
            return;
        }
        User admin = userRepository.save(User.builder()
                .email("platform-admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .passwordHash("hash")
                .firstName("Platform")
                .lastName("Admin")
                .role(User.Role.ADMIN)
                .isActive(true)
                .isEmailVerified(true)
                .securityVersion(1L)
                .isDeleted(false)
                .build());
        platformAdminToken = TestTokens.userToken(admin.getEmail());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void resetPassword_succeeds_andInvalidatesTheStoredHash() throws Exception {
        User user = userRepository.save(User.builder()
                .email("reset-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .passwordHash("old-hash")
                .firstName("Reset")
                .lastName("Target")
                .role(User.Role.STUDENT)
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .isActive(true)
                .isEmailVerified(true)
                .securityVersion(1L)
                .isDeleted(false)
                .build());

        String oldHash = user.getPasswordHash();

        mockMvc.perform(post("/v1/platform-admin/users/" + user.getId() + "/reset-password")
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"Str0ngPass!2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User reloaded = userRepository.findByIdAndIsDeletedFalse(user.getId()).orElseThrow();
        assertNotEquals(oldHash, reloaded.getPasswordHash(), "the stored hash must be replaced");
        assertTrue(reloaded.getPasswordHash().startsWith("$2"), "the new hash must be a BCrypt hash");
        assertEquals(2L, reloaded.getSecurityVersion(),
                "reset must bump securityVersion so existing sessions/tokens are invalidated");
    }

    @Test
    void resetPassword_writesAnAuditableActionThatExistsInTheEnum() throws Exception {
        User user = userRepository.save(User.builder()
                .email("audit-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .passwordHash("old-hash")
                .firstName("Audit")
                .lastName("Target")
                .role(User.Role.STUDENT)
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .isActive(true)
                .isEmailVerified(true)
                .securityVersion(1L)
                .isDeleted(false)
                .build());

        mockMvc.perform(post("/v1/platform-admin/users/" + user.getId() + "/reset-password")
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"Str0ngPass!2026\"}"))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(a -> AuditLog.AuditAction.RESET_PASSWORD.equals(a.getAction()))
                .toList();
        assertTrue(!logs.isEmpty(), "a password reset must leave an audit trail");
        // valueOf must succeed for every action the services write (guards the enum contract).
        for (AuditLog log : logs) {
            assertEquals(AuditLog.AuditAction.RESET_PASSWORD, log.getAction());
        }
    }

    @Test
    void resetPassword_rejectsShortPasswordWithAClearMessage() throws Exception {
        User user = userRepository.save(User.builder()
                .email("short-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .passwordHash("old-hash")
                .firstName("Short")
                .lastName("Password")
                .role(User.Role.STUDENT)
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .isActive(true)
                .isEmailVerified(true)
                .securityVersion(1L)
                .isDeleted(false)
                .build());

        mockMvc.perform(post("/v1/platform-admin/users/" + user.getId() + "/reset-password")
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("A new password of at least 8 characters is required"));
    }

    @Test
    void resetPassword_isNotAvailableToNonAdmins() throws Exception {
        User user = userRepository.save(User.builder()
                .email("nonadm-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .passwordHash("old-hash")
                .firstName("Non")
                .lastName("Admin")
                .role(User.Role.STUDENT)
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .isActive(true)
                .isEmailVerified(true)
                .securityVersion(1L)
                .isDeleted(false)
                .build());

        mockMvc.perform(post("/v1/platform-admin/users/" + user.getId() + "/reset-password")
                        .header("Authorization", "Bearer " + TestTokens.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"Str0ngPass!2026\"}"))
                .andExpect(status().isForbidden());
    }
}
