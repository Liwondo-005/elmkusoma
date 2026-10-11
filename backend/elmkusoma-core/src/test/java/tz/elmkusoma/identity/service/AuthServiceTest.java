package tz.elmkusoma.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.config.EventPublisherService;
import tz.elmkusoma.config.security.JwtTokenProvider;
import tz.elmkusoma.config.security.RateLimitService;
import tz.elmkusoma.identity.service.MfaService;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.identity.domain.EmailVerificationToken;
import tz.elmkusoma.identity.domain.PasswordResetToken;
import tz.elmkusoma.identity.domain.RevokedToken;
import tz.elmkusoma.identity.dto.request.*;
import tz.elmkusoma.identity.dto.response.AuthResponse;
import tz.elmkusoma.identity.repository.EmailVerificationTokenRepository;
import tz.elmkusoma.identity.repository.PasswordResetTokenRepository;
import tz.elmkusoma.identity.repository.RevokedTokenRepository;
import tz.elmkusoma.identity.service.PasswordPolicy;
import tz.elmkusoma.identity.service.impl.AuthServiceImpl;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentClassAssignmentRepository studentClassAssignmentRepository;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private ParentRepository parentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private InstitutionMembershipRepository membershipRepository;
    @Mock
    private InstitutionRepository institutionRepository;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private EventPublisherService eventPublisherService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private MfaService mfaService;

    // Concrete collaborator, so @InjectMocks would otherwise inject null.


    @Spy


    private PasswordPolicy passwordPolicy = new PasswordPolicy();



    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 3600000L);
        lenient().when(rateLimitService.allow(anyString(), anyInt(), anyLong())).thenReturn(true);
    }

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setPassword("Str0ng!Passw0rd");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setRole("STUDENT");

        when(userRepository.existsByEmailAndIsDeletedFalse("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng!Passw0rd")).thenReturn("encoded_password");

        User savedUser = User.builder()
                .id(userId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.STUDENT)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        // Audit B-27: registration only assigns the HQ scope when that institution really exists.
        // The old code fabricated the id even when the lookup came back empty, and the stub below
        // encoded that fiction. Mocking a real HQ institution keeps the original intent.
        when(institutionRepository.findByIdAndIsDeletedFalse(
                UUID.fromString("a0000000-0000-0000-0000-000000000001")))
                .thenReturn(Optional.of(Institution.builder()
                        .id(UUID.fromString("a0000000-0000-0000-0000-000000000001"))
                        .name("ELMKUSOMA National HQ")
                        .build()));

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessTokenWithClaims(
                eq("john@example.com"), eq(userId), eq("STUDENT"),
                eq(UUID.fromString("a0000000-0000-0000-0000-000000000001")), any()))
                .thenReturn("access_token_123");
        when(jwtTokenProvider.generateRefreshToken(eq("john@example.com"), any())).thenReturn("refresh_token_123");
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token_123", response.getAccessToken());
        assertEquals("refresh_token_123", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getUser());
        assertEquals("john@example.com", response.getUser().getEmail());
        assertEquals("STUDENT", response.getUser().getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withVetaLearningLevel_persistsVeta() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("veta-student@example.com");
        request.setPassword("Str0ng!Passw0rd");
        request.setFirstName("Neema");
        request.setLastName("Kileo");
        request.setRole("STUDENT");
        request.setLearningLevel("VETA");

        when(userRepository.existsByEmailAndIsDeletedFalse("veta-student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng!Passw0rd")).thenReturn("encoded_password");

        final User[] captured = new User[1];
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(UUID.randomUUID());
            captured[0] = user;
            return user;
        });
        // Audit B-27: the HQ institution must exist for that scope to be assigned.
        when(institutionRepository.findByIdAndIsDeletedFalse(
                UUID.fromString("a0000000-0000-0000-0000-000000000001")))
                .thenReturn(Optional.of(Institution.builder()
                        .id(UUID.fromString("a0000000-0000-0000-0000-000000000001"))
                        .name("ELMKUSOMA National HQ")
                        .build()));
        when(jwtTokenProvider.generateAccessTokenWithClaims(
                eq("veta-student@example.com"), any(UUID.class), eq("STUDENT"),
                eq(UUID.fromString("a0000000-0000-0000-0000-000000000001")), any()))
                .thenReturn("access_token_123");
        when(jwtTokenProvider.generateRefreshToken(eq("veta-student@example.com"), any())).thenReturn("refresh_token_123");
        when(studentRepository.findByUserIdAndIsDeletedFalse(any(UUID.class))).thenReturn(Optional.empty());

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(captured[0]);
        // VETA is part of the six-level model and must survive registration
        // (previously ValueOf failed silently and the level was dropped).
        assertEquals(User.LearningLevel.VETA, captured[0].getLearningLevel());
    }

    @Test
    void register_whenEmailExists_shouldThrow() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("Str0ng!Passw0rd");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmailAndIsDeletedFalse("existing@example.com")).thenReturn(true);

        // Anti-enumeration: identical 201 shape, no tokens, owner notified.
        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNull(response.getAccessToken());
        assertNull(response.getUser());
        verify(eventPublisherService).publishEmailEvent(
                eq("existing@example.com"), anyString(), eq("password-reset"),
                any(), any());
    }

    @Test
    void register_withInvalidRole_shouldThrow() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setPassword("Str0ng!Passw0rd");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setRole("ADMIN");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));
        assertTrue(exception.getMessage().contains("not allowed"));
    }

    @Test
    void login_shouldAuthenticateAndReturnTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("john@example.com", "password123")))
                .thenReturn(authentication);

        User user = User.builder()
                .id(userId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.STUDENT)
                .isActive(true)
                .isEmailVerified(true)
                .passwordHash("encoded_password")
                .build();

        when(userRepository.findByEmailAndIsDeletedFalse("john@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessTokenWithClaims(
                eq("john@example.com"), eq(userId), eq("STUDENT"), isNull(), any()))
                .thenReturn("access_token_123");
        when(jwtTokenProvider.generateRefreshToken(eq("john@example.com"), any())).thenReturn("refresh_token_123");
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token_123", response.getAccessToken());
        assertEquals("refresh_token_123", response.getRefreshToken());
        assertEquals("john@example.com", response.getUser().getEmail());
    }

    @Test
    void login_whenAccountDeactivated_shouldThrow() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("john@example.com", "password123")))
                .thenReturn(authentication);

        User user = User.builder()
                .id(userId)
                .email("john@example.com")
                .isActive(false)
                .role(User.Role.STUDENT)
                .passwordHash("encoded_password")
                .build();

        when(userRepository.findByEmailAndIsDeletedFalse("john@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> authService.login(request));
    }

    @Test
    void login_whenUserNotFound_shouldThrow() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("nonexistent@example.com", "password123")))
                .thenReturn(authentication);

        when(userRepository.findByEmailAndIsDeletedFalse("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request));
    }

    @Test
    void refreshToken_shouldGenerateNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old_refresh_token");

        when(jwtTokenProvider.validateRefreshToken("old_refresh_token")).thenReturn(true);
        when(revokedTokenRepository.existsByTokenHash(anyString())).thenReturn(false);
        when(jwtTokenProvider.getEmailFromRefreshToken("old_refresh_token")).thenReturn("john@example.com");
        when(jwtTokenProvider.getSecurityVersionFromToken("old_refresh_token")).thenReturn(1L);

        User user = User.builder()
                .id(userId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.STUDENT)
                .isActive(true)
                .isEmailVerified(true)
                .build();

        when(userRepository.findByEmailAndIsDeletedFalse("john@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessTokenWithClaims(
                eq("john@example.com"), eq(userId), eq("STUDENT"), isNull(), any()))
                .thenReturn("new_access_token");
        when(jwtTokenProvider.generateRefreshToken(eq("john@example.com"), any())).thenReturn("new_refresh_token");
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
    }

    @Test
    void refreshToken_whenTokenInvalid_shouldThrow() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid_token");

        when(jwtTokenProvider.validateRefreshToken("invalid_token")).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_whenTokenRevoked_shouldThrow() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("revoked_token");

        when(jwtTokenProvider.validateRefreshToken("revoked_token")).thenReturn(true);
        when(revokedTokenRepository.existsByTokenHash(anyString())).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> authService.refreshToken(request));
    }

    @Test
    void forgotPassword_shouldCreateResetToken() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("john@example.com");

        User user = User.builder()
                .id(userId)
                .email("john@example.com")
                .build();

        when(userRepository.findByEmailAndIsDeletedFalse("john@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(null);

        authService.forgotPassword(request);

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void forgotPassword_whenUserNotFound_shouldNotThrow() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nonexistent@example.com");

        when(userRepository.findByEmailAndIsDeletedFalse("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.forgotPassword(request));
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndMarkTokenUsed() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset_token_123");
        request.setNewPassword("newpassword123");

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("reset_token_123")
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString()))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newpassword123")).thenReturn("encoded_new_password");

        User user = User.builder()
                .id(userId)
                .email("john@example.com")
                .passwordHash("old_hash")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(resetToken);

        authService.resetPassword(request);

        assertEquals("encoded_new_password", user.getPasswordHash());
        assertTrue(resetToken.getUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
    }

    @Test
    void resetPassword_whenTokenInvalid_shouldThrow() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid_token");
        request.setNewPassword("newpassword123");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString()))
                .thenReturn(Optional.empty());

        // Unified single-400: unknown tokens are indistinguishable from expired ones.
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    }

    @Test
    void logout_shouldRevokeRefreshToken() {
        authService.logout("refresh_token_123");

        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    void getCurrentUser_shouldReturnUserInfo() {
        User user = User.builder()
                .id(userId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.STUDENT)
                .isActive(true)
                .isEmailVerified(true)
                .build();

        when(userRepository.findByEmailAndIsDeletedFalse("john@example.com"))
                .thenReturn(Optional.of(user));
        when(studentRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        AuthResponse.UserInfo userInfo = authService.getCurrentUser("john@example.com");

        assertNotNull(userInfo);
        assertEquals("john@example.com", userInfo.getEmail());
        assertEquals("John", userInfo.getFirstName());
        assertEquals("Doe", userInfo.getLastName());
        assertEquals("STUDENT", userInfo.getRole());
        assertTrue(userInfo.isEmailVerified());
    }

    @Test
    void getCurrentUser_whenNotFound_shouldThrow() {
        when(userRepository.findByEmailAndIsDeletedFalse("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.getCurrentUser("nonexistent@example.com"));
    }
}
