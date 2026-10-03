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
import tz.elmkusoma.config.security.JwtTokenProvider;
import tz.elmkusoma.config.security.RateLimitService;
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
