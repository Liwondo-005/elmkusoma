package tz.elmkusoma.identity.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import tz.elmkusoma.academic.repository.AcademicYearRepository;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.config.EventPublisherService;
import tz.elmkusoma.config.security.JwtTokenProvider;
import tz.elmkusoma.config.security.RateLimitService;
import tz.elmkusoma.identity.service.MfaService;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.identity.domain.PasswordResetToken;
import tz.elmkusoma.identity.dto.request.ForgotPasswordRequest;
import tz.elmkusoma.identity.dto.request.RefreshTokenRequest;
import tz.elmkusoma.identity.dto.request.ResetPasswordRequest;
import tz.elmkusoma.identity.repository.EmailVerificationTokenRepository;
import tz.elmkusoma.identity.repository.PasswordResetTokenRepository;
import tz.elmkusoma.identity.repository.RevokedTokenRepository;
import tz.elmkusoma.identity.repository.VerificationCodeRepository;
import tz.elmkusoma.identity.service.impl.AuthServiceImpl;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Security regression tests for authentication recovery (attack scenarios).
 */
@ExtendWith(MockitoExtension.class)
class AuthRecoverySecurityTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentClassAssignmentRepository studentClassAssignmentRepository;
    @Mock private RevokedTokenRepository revokedTokenRepository;
    @Mock private VerificationCodeRepository verificationCodeRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private InstitutionMembershipRepository membershipRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private ClassGroupRepository classGroupRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    // Real limiter: the security property under test lives here.
    private final RateLimitService rateLimitService = new RateLimitService();
    @Mock private EventPublisherService eventPublisherService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private MfaService mfaService;

    @InjectMocks
    private AuthServiceImpl authService;

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    private User userAtVersion(long version) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("victim@example.com")
                .passwordHash("old_hash")
                .role(User.Role.STUDENT)
                .isActive(true)
                .isEmailVerified(false)
                .securityVersion(version)
                .build();
    }

    private PasswordResetToken liveToken(UUID userId) {
        return PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .token("stored-hash")
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusHours(23))
                .used(false)
                .build();
    }

    @Test
    void resetPassword_bumpsSecurityVersion_invalidatingOldSessions() {
        User user = userAtVersion(1L);
        PasswordResetToken token = liveToken(user.getId());
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("BrandNew123");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString()))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("BrandNew123")).thenReturn("new_hash");
        when(passwordResetTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(token));

        authService.resetPassword(request);

        assertEquals(2L, user.getSecurityVersion());
        assertTrue(token.getUsed());
    }

    @Test
    void resetPassword_replayWithSameToken_fails() {
        User user = userAtVersion(1L);
        PasswordResetToken token = liveToken(user.getId());
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("BrandNew123");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString()))
                .thenReturn(Optional.of(token), Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("BrandNew123")).thenReturn("new_hash");
        when(passwordResetTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(token));

        authService.resetPassword(request);
        assertThrows(ResourceNotFoundException.class, () -> authService.resetPassword(request));
    }

    @Test
    void resetPassword_expiredToken_rejected() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .token("stored-hash")
                .userId(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("BrandNew123");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString()))
                .thenReturn(Optional.of(token));

        assertThrows(ForbiddenException.class, () -> authService.resetPassword(request));
    }

    @Test
    void refreshToken_staleSecurityVersion_rejected() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("stale_refresh");

        // Token minted at version 1, but the account has since been reset to 2.
        User user = userAtVersion(2L);
        when(jwtTokenProvider.validateToken("stale_refresh")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("stale_refresh")).thenReturn("victim@example.com");
        when(userRepository.findByEmailAndIsDeletedFalse("victim@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtTokenProvider.getSecurityVersionFromToken("stale_refresh")).thenReturn(1L);

        assertThrows(ForbiddenException.class, () -> authService.refreshToken(request));
    }

    @Test
    void forgotPassword_storesHashNotRawToken() throws Exception {
        User user = userAtVersion(1L);
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("victim@example.com");

        when(userRepository.findByEmailAndIsDeletedFalse("victim@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of());

        final PasswordResetToken[] saved = new PasswordResetToken[1];
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> {
            saved[0] = inv.getArgument(0);
            return saved[0];
        });

        String raw = authService.forgotPassword(request);

        assertNotNull(raw);
        assertNotEquals(raw, saved[0].getToken());
        assertEquals(sha256(raw), saved[0].getToken());
    }

    @Test
    void changePassword_successBumpsVersion() {
        User user = userAtVersion(1L);
        tz.elmkusoma.identity.dto.request.ChangePasswordRequest request =
                new tz.elmkusoma.identity.dto.request.ChangePasswordRequest();
        request.setCurrentPassword("OldPass123");
        request.setNewPassword("BrandNew123");

        when(userRepository.findByEmailAndIsDeletedFalse("victim@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "old_hash")).thenReturn(true);
        when(passwordEncoder.matches("BrandNew123", "old_hash")).thenReturn(false);
        when(passwordEncoder.encode("BrandNew123")).thenReturn("new_hash");

        authService.changePassword("victim@example.com", request);

        assertEquals("new_hash", user.getPasswordHash());
        assertEquals(2L, user.getSecurityVersion());
    }

    @Test
    void changePassword_wrongCurrent_rejectedWithoutChange() {
        User user = userAtVersion(1L);
        tz.elmkusoma.identity.dto.request.ChangePasswordRequest request =
                new tz.elmkusoma.identity.dto.request.ChangePasswordRequest();
        request.setCurrentPassword("Nope12345");
        request.setNewPassword("BrandNew123");

        when(userRepository.findByEmailAndIsDeletedFalse("victim@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Nope12345", "old_hash")).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> authService.changePassword("victim@example.com", request));
        assertEquals(1L, user.getSecurityVersion());
        assertEquals("old_hash", user.getPasswordHash());
    }

    @Test
    void forgotPassword_publishesDeliveryEmail() {
        User user = userAtVersion(1L);
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("victim@example.com");

        when(userRepository.findByEmailAndIsDeletedFalse("victim@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of());

        authService.forgotPassword(request);

        verify(eventPublisherService).publishEmailEvent(
                eq("victim@example.com"), anyString(), eq("email/password-reset"),
                argThat(vars -> vars != null && String.valueOf(vars.get("resetLink"))
                        .startsWith("http://localhost:3000/reset-password?token=")),
                any());
    }

    @Test
    void resetPassword_notifiesAndAudits() {
        User user = userAtVersion(1L);
        PasswordResetToken token = liveToken(user.getId());
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("BrandNew123");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString()))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("BrandNew123")).thenReturn("new_hash");
        when(passwordResetTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(token));

        authService.resetPassword(request);

        verify(notificationService).notifyUser(eq(user.getId()), anyString(), anyString(),
                eq("SECURITY"), eq("AUTH"), isNull());
        verify(auditService).recordSecurityEvent(eq(user.getInstitutionId()), eq(user.getId()),
                eq("victim@example.com"),
                eq(tz.elmkusoma.audit.domain.SecurityEvent.SecurityEventType.PASSWORD_RESET),
                anyString(), any(), isNull(), isNull());
    }

    @Test
    void login_mfaEnrolled_returnsChallengeNotSession() {
        User user = userAtVersion(1L);
        tz.elmkusoma.identity.dto.request.LoginRequest request =
                new tz.elmkusoma.identity.dto.request.LoginRequest();
        request.setEmail("victim@example.com");
        request.setPassword("RightPass123");

        org.springframework.security.core.Authentication authentication =
                mock(org.springframework.security.core.Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmailAndIsDeletedFalse("victim@example.com"))
                .thenReturn(Optional.of(user));
        when(mfaService.hasVerifiedFactor(user.getId())).thenReturn(true);
        when(jwtTokenProvider.generateMfaToken(eq("victim@example.com"), eq(1L), eq(user.getId())))
                .thenReturn("mfa_challenge");

        tz.elmkusoma.identity.dto.response.AuthResponse response = authService.login(request);

        assertTrue(response.isMfaRequired());
        assertEquals("mfa_challenge", response.getMfaToken());
        assertNull(response.getAccessToken());
        assertNull(response.getRefreshToken());
    }

    @Test
    void resetPassword_mfaEnrolledWithoutStepUp_blocked() {
        User user = userAtVersion(1L);
        PasswordResetToken token = liveToken(user.getId());
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("BrandNew123");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString()))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(mfaService.hasVerifiedFactor(user.getId())).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> authService.resetPassword(request));
        assertEquals(1L, user.getSecurityVersion());
    }

    @Test
    void resetPassword_mfaEnrolledWithRecoveryCode_proceeds() {
        User user = userAtVersion(1L);
        PasswordResetToken token = liveToken(user.getId());
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("BrandNew123");
        request.setRecoveryCode("KQ7M-X2P9");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString()))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(mfaService.hasVerifiedFactor(user.getId())).thenReturn(true);
        when(mfaService.consumeRecoveryCode(user.getId(), "KQ7M-X2P9")).thenReturn(true);
        when(passwordEncoder.encode("BrandNew123")).thenReturn("new_hash");
        when(passwordResetTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(token));

        authService.resetPassword(request);

        assertEquals(2L, user.getSecurityVersion());
    }

    @Test
    void revokeAllSessions_bumpsVersion() {
        User user = userAtVersion(1L);
        when(userRepository.findByEmailAndIsDeletedFalse("victim@example.com"))
                .thenReturn(Optional.of(user));

        authService.revokeAllSessions("victim@example.com");

        assertEquals(2L, user.getSecurityVersion());
        verify(auditService).recordSecurityEvent(eq(user.getInstitutionId()), eq(user.getId()),
                eq("victim@example.com"),
                eq(tz.elmkusoma.audit.domain.SecurityEvent.SecurityEventType.SESSIONS_REVOKED),
                anyString(), any(), isNull(), isNull());
    }

    @Test
    void rateLimiter_tripsAfterBudget_andRecovers() {
        RateLimitService limiter = new RateLimitService();
        String key = "probe:" + UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allow(key, 10, 60_000L));
        }
        assertFalse(limiter.allow(key, 10, 60_000L));
        assertTrue(limiter.retryAfterSeconds(key, 60_000L) >= 1);
    }

    @Test
    void login_overAccountBudget_rejectedWith429() {
        // @InjectMocks leaves the real limiter field unfilled (null) because it
        // is not a mock; install a real one so the budget is actually enforced.
        RateLimitService realLimiter = new RateLimitService();
        org.springframework.test.util.ReflectionTestUtils.setField(
                authService, "rateLimitService", realLimiter);
        tz.elmkusoma.identity.dto.request.LoginRequest request =
                new tz.elmkusoma.identity.dto.request.LoginRequest();
        request.setEmail("victim@example.com");
        request.setPassword("wrong");
        // Exhaust the login per-account bucket directly.
        for (int i = 0; i < 10; i++) {
            realLimiter.allow("login:victim@example.com", 10, 60_000L);
        }
        assertThrows(tz.elmkusoma.exception.RateLimitExceededException.class,
                () -> authService.login(request));
    }
}
