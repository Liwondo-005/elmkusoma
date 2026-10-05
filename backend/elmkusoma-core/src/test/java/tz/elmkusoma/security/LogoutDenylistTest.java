package tz.elmkusoma.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tz.elmkusoma.academic.repository.AcademicYearRepository;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.config.TenantAwareRedisTemplate;
import tz.elmkusoma.config.security.JwtAuthenticationFilter;
import tz.elmkusoma.config.security.JwtTokenProvider;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.identity.repository.EmailVerificationTokenRepository;
import tz.elmkusoma.identity.repository.PasswordResetTokenRepository;
import tz.elmkusoma.identity.repository.RevokedTokenRepository;
import tz.elmkusoma.identity.repository.VerificationCodeRepository;
import tz.elmkusoma.identity.service.impl.AuthServiceImpl;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Mock-only tests for the logout access-token denylist (no Spring context,
 * no Redis server).
 *
 * <p>Why mock-only: {@code application-test.properties} points at
 * {@code localhost:6379} with no guaranteed test Redis, and both the logout
 * write and the filter read fail OPEN when Redis is down. An HTTP-level
 * "logout then access denied" test would therefore pass vacuously without a
 * live Redis (access still allowed) and cannot pin the contract. These tests
 * pin the logic instead:</p>
 * <ul>
 *   <li>logout writes the global (tenant-unscoped) jti key with a TTL bounded
 *       by the remaining access-token lifetime;</li>
 *   <li>pre-hardening tokens without jti write nothing (expire naturally);</li>
 *   <li>the filter denies a denylisted jti and admits a non-denylisted one.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LogoutDenylistTest {

    private static final String JWT_SECRET_B64 = "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLTIwMjQ=";

    private JwtTokenProvider realProvider;
    private AuthServiceImpl authService;

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
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
    @Mock private tz.elmkusoma.config.security.RateLimitService rateLimitService;
    @Mock private tz.elmkusoma.config.EventPublisherService eventPublisherService;
    @Mock private tz.elmkusoma.learner.service.NotificationService notificationService;
    @Mock private tz.elmkusoma.audit.service.AuditService auditService;
    @Mock private tz.elmkusoma.identity.service.MfaService mfaService;
    @Mock private tz.elmkusoma.shared.repository.UserRepository filterUserRepository;

    @Mock private TenantAwareRedisTemplate tenantRedis;
    @Mock private RedisTemplate<String, Object> raw;
    @Mock private ValueOperations<String, Object> valueOps;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        // Session-version gate: test tokens carry sv=1, so the backing user
        // must exist at version 1 or the filter denies before jti logic runs.
        lenient().when(filterUserRepository.findByEmailAndIsDeletedFalse(anyString()))
                .thenAnswer(inv -> {
                    String email = inv.getArgument(0);
                    return Optional.of(tz.elmkusoma.shared.domain.User.builder()
                            .id(UUID.randomUUID())
                            .email(email)
                            .passwordHash("hash")
                            .role(tz.elmkusoma.shared.domain.User.Role.STUDENT)
                            .isActive(true)
                            .securityVersion(1L)
                            .build());
                });
        realProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(realProvider, "jwtSecret", JWT_SECRET_B64);
        ReflectionTestUtils.setField(realProvider, "accessTokenExpirationMs", 3_600_000L);
        ReflectionTestUtils.setField(realProvider, "refreshTokenExpirationMs", 604_800_000L);

        authService = new AuthServiceImpl(authenticationManager, userRepository, passwordEncoder,
                realProvider, passwordResetTokenRepository, emailVerificationTokenRepository,
                studentRepository, studentClassAssignmentRepository, revokedTokenRepository,
                verificationCodeRepository, parentRepository, teacherRepository, membershipRepository,
                institutionRepository, classGroupRepository, academicYearRepository, enrollmentRepository,
                rateLimitService, eventPublisherService, notificationService, auditService, mfaService);
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 3_600_000L);
        ReflectionTestUtils.setField(authService, "tenantAwareRedisTemplate", tenantRedis);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    private void bindBearerToken(String accessToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void logout_writesGlobalJtiDenylistKeyWithBoundedTtl() {
        String email = "logout-" + UUID.randomUUID() + "@test.com";
        String accessToken = realProvider.generateAccessTokenWithClaims(
                email, UUID.randomUUID(), "STUDENT", UUID.randomUUID(), 1L);
        String jti = realProvider.getJtiFromToken(accessToken);
        assertNotNull(jti);
        bindBearerToken(accessToken);

        when(revokedTokenRepository.existsByTokenHash(anyString())).thenReturn(false);
        when(revokedTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRedis.getRawTemplate()).thenReturn(raw);
        when(raw.opsForValue()).thenReturn(valueOps);

        authService.logout("refresh-token-value");

        verify(revokedTokenRepository).save(any());
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOps).set(keyCaptor.capture(), eq("1"), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        assertEquals(JwtTokenProvider.LOGOUT_JTI_DENYLIST_PREFIX + jti, keyCaptor.getValue());
        assertTrue(ttlCaptor.getValue() > 0 && ttlCaptor.getValue() <= 3600,
                "denylist TTL must be bounded by the remaining access-token lifetime");
    }

    @Test
    void logout_withPreHardeningTokenWithoutJti_writesNoDenylistEntry() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET_B64));
        String legacyAccess = Jwts.builder()
                .subject("legacy@test.com")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertNull(realProvider.getJtiFromToken(legacyAccess));
        bindBearerToken(legacyAccess);

        when(revokedTokenRepository.existsByTokenHash(anyString())).thenReturn(false);
        when(revokedTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.logout("refresh-token-value");

        verify(revokedTokenRepository).save(any());
        verify(raw, never()).opsForValue();
    }

    @Test
    void filter_deniesDenylistedJti_andAdmitsCleanToken() throws Exception {
        String email = "filter-" + UUID.randomUUID() + "@test.com";
        String accessToken = realProvider.generateAccessTokenWithClaims(
                email, UUID.randomUUID(), "STUDENT", UUID.randomUUID(), 1L);

        UserDetails details = new org.springframework.security.core.userdetails.User(
                email, "hash", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        when(userDetailsService.loadUserByUsername(email)).thenReturn(details);
        when(tenantRedis.getRawTemplate()).thenReturn(raw);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(realProvider, userDetailsService,
                filterUserRepository);
        ReflectionTestUtils.setField(filter, "tenantAwareRedisTemplate", tenantRedis);

        // Denylisted -> no authentication, but the chain still proceeds.
        when(raw.hasKey(JwtTokenProvider.LOGOUT_JTI_DENYLIST_PREFIX
                + realProvider.getJtiFromToken(accessToken))).thenReturn(true);
        MockHttpServletRequest deniedReq = new MockHttpServletRequest();
        deniedReq.addHeader("Authorization", "Bearer " + accessToken);
        filter.doFilter(deniedReq, new MockHttpServletResponse(), filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // Not denylisted -> authenticated as the token subject.
        when(raw.hasKey(anyString())).thenReturn(false);
        MockHttpServletRequest allowedReq = new MockHttpServletRequest();
        allowedReq.addHeader("Authorization", "Bearer " + accessToken);
        filter.doFilter(allowedReq, new MockHttpServletResponse(), filterChain);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain, times(2)).doFilter(any(), any());
    }
}
