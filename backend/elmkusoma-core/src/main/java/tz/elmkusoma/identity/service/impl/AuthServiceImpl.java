package tz.elmkusoma.identity.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.config.security.JwtTokenProvider;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.identity.domain.EmailVerificationToken;
import tz.elmkusoma.identity.domain.PasswordResetToken;
import tz.elmkusoma.identity.domain.VerificationCode;
import tz.elmkusoma.identity.dto.request.*;
import tz.elmkusoma.identity.dto.response.AuthResponse;
import tz.elmkusoma.identity.repository.EmailVerificationTokenRepository;
import tz.elmkusoma.identity.repository.PasswordResetTokenRepository;
import tz.elmkusoma.identity.repository.RevokedTokenRepository;
import tz.elmkusoma.identity.repository.VerificationCodeRepository;
import tz.elmkusoma.identity.service.AuthService;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final StudentRepository studentRepository;
    private final StudentClassAssignmentRepository studentClassAssignmentRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final tz.elmkusoma.parent.repository.ParentRepository parentRepository;
    private final tz.elmkusoma.teacher.repository.TeacherRepository teacherRepository;
    private final tz.elmkusoma.shared.repository.InstitutionMembershipRepository membershipRepository;
    private final tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository;
    private final tz.elmkusoma.academic.repository.ClassGroupRepository classGroupRepository;
    private final tz.elmkusoma.academic.repository.AcademicYearRepository academicYearRepository;
    private final tz.elmkusoma.enrollment.repository.EnrollmentRepository enrollmentRepository;

    private static final UUID HQ_INSTITUTION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tz.elmkusoma.administration.service.PlatformPolicyService platformPolicyService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tz.elmkusoma.identity.service.LoginAttemptService loginAttemptService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tz.elmkusoma.audit.service.AuditService auditService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tz.elmkusoma.identity.service.OtpMailService otpMailService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tz.elmkusoma.config.TenantAwareRedisTemplate tenantAwareRedisTemplate;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           EmailVerificationTokenRepository emailVerificationTokenRepository,
                           StudentRepository studentRepository,
                           StudentClassAssignmentRepository studentClassAssignmentRepository,
                           RevokedTokenRepository revokedTokenRepository,
                           VerificationCodeRepository verificationCodeRepository,
                           tz.elmkusoma.parent.repository.ParentRepository parentRepository,
                           tz.elmkusoma.teacher.repository.TeacherRepository teacherRepository,
                           tz.elmkusoma.shared.repository.InstitutionMembershipRepository membershipRepository,
                           tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository,
                           tz.elmkusoma.academic.repository.ClassGroupRepository classGroupRepository,
                           tz.elmkusoma.academic.repository.AcademicYearRepository academicYearRepository,
                           tz.elmkusoma.enrollment.repository.EnrollmentRepository enrollmentRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.studentRepository = studentRepository;
        this.studentClassAssignmentRepository = studentClassAssignmentRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.parentRepository = parentRepository;
        this.teacherRepository = teacherRepository;
        this.membershipRepository = membershipRepository;
        this.institutionRepository = institutionRepository;
        this.classGroupRepository = classGroupRepository;
        this.academicYearRepository = academicYearRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    private static final java.util.Set<User.Role> PUBLIC_REGISTRATION_ROLES = java.util.Set.of(
            User.Role.STUDENT,
            User.Role.TEACHER,
            User.Role.PARENT,
            User.Role.OTHER_LEARNER
    );

    private User.Role resolveRegistrationRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return User.Role.STUDENT;
        }
        User.Role role;
        try {
            role = User.Role.valueOf(requestedRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + requestedRole);
        }
        if (!PUBLIC_REGISTRATION_ROLES.contains(role)) {
            throw new IllegalArgumentException("Role '" + role + "' is not allowed for public registration");
        }
        return role;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (platformPolicyService != null && !platformPolicyService.registrationEnabled()) {
            throw new IllegalStateException("Platform policy forbids public registration");
        }
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            // Neutral message: must not confirm whether the email is registered
            // (account-enumeration defence). Still 400 via IllegalArgumentException.
            throw new IllegalArgumentException("Unable to complete registration with the provided details");
        }

        User.Role role = resolveRegistrationRole(request.getRole());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(role)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        if (role == User.Role.STUDENT && request.getLearningLevel() != null && !request.getLearningLevel().isBlank()) {
            try {
                user.setLearningLevel(User.LearningLevel.valueOf(request.getLearningLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid learning level: {}, skipping", request.getLearningLevel());
            }
        }

        if (role == User.Role.STUDENT && request.getSecondaryStage() != null && !request.getSecondaryStage().isBlank()) {
            try {
                user.setSecondaryStage(User.SecondaryStage.valueOf(request.getSecondaryStage().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid secondary stage: {}, skipping", request.getSecondaryStage());
            }
        }

        if (role == User.Role.STUDENT && request.getForm() != null && !request.getForm().isBlank()) {
            try {
                user.setForm(User.Form.valueOf(request.getForm().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid form: {}, skipping", request.getForm());
            }
        }

        UUID instId = user.getInstitutionId();
        if (instId == null) {
            instId = resolveDefaultInstitutionId();
            user.setInstitutionId(instId);
        }
        user = userRepository.save(user);

        ensureMembership(user, instId, role);

        if (role == User.Role.STUDENT || role == User.Role.OTHER_LEARNER) {
            ensureStudentProfile(user, instId);
        } else if (role == User.Role.PARENT) {
            ensureParentProfile(user, instId);
        } else if (role == User.Role.TEACHER) {
            ensureTeacherProfile(user, instId);
        }

        log.info("User registered successfully: {}", user.getEmail());

        String accessToken = jwtTokenProvider.generateAccessTokenWithClaims(
                user.getEmail(), user.getId(), user.getRole().name(), instId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .user(buildUserInfo(user))
                .build();
    }

    private UUID resolveDefaultInstitutionId() {
        return institutionRepository.findByIdAndIsDeletedFalse(HQ_INSTITUTION_ID)
                .map(tz.elmkusoma.shared.domain.Institution::getId)
                .orElse(HQ_INSTITUTION_ID);
    }

    private void ensureMembership(User user, UUID institutionId, User.Role role) {
        if (institutionId == null || user.getId() == null) {
            return;
        }
        if (membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(user.getId(), institutionId)) {
            return;
        }
        InstitutionMembership.Role membershipRole = switch (role) {
            case TEACHER -> InstitutionMembership.Role.TEACHER;
            case PARENT -> InstitutionMembership.Role.PARENT;
            case STUDENT, OTHER_LEARNER -> InstitutionMembership.Role.STUDENT;
            default -> InstitutionMembership.Role.STUDENT;
        };
        InstitutionMembership membership = InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(institutionId)
                .role(membershipRole)
                .isActive(true)
                .isDeleted(false)
                .build();
        membershipRepository.save(membership);
    }

    private void ensureStudentProfile(User user, UUID institutionId) {
        if (studentRepository.existsByUserIdAndIsDeletedFalse(user.getId())) {
            return;
        }
        Student student = Student.builder()
                .userId(user.getId())
                .admissionNumber(generateAdmissionNumber())
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(java.time.LocalDate.now())
                .build();
        student.setInstitutionId(institutionId);
        studentRepository.save(student);
    }

    private void ensureParentProfile(User user, UUID institutionId) {
        if (institutionId == null) {
            return;
        }
        if (parentRepository.findByUserIdAndIsDeletedFalse(user.getId()).isPresent()) {
            return;
        }
        var parent = tz.elmkusoma.parent.domain.Parent.builder()
                .userId(user.getId())
                .relationshipType(tz.elmkusoma.parent.domain.Parent.RelationshipType.GUARDIAN)
                .build();
        parent.setInstitutionId(institutionId);
        parentRepository.save(parent);
    }

    private void ensureTeacherProfile(User user, UUID institutionId) {
        if (institutionId == null) {
            return;
        }
        if (teacherRepository.findByUserIdAndInstitutionId(user.getId(), institutionId).isPresent()) {
            return;
        }
        var teacher = tz.elmkusoma.teacher.domain.Teacher.builder()
                .userId(user.getId())
                .status(tz.elmkusoma.teacher.domain.TeacherStatus.ACTIVE)
                .build();
        teacher.setInstitutionId(institutionId);
        teacherRepository.save(teacher);
    }

    private String generateAdmissionNumber() {
        String candidate;
        int attempts = 0;
        do {
            candidate = "ADM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            attempts++;
            if (attempts > 10) {
                candidate = "ADM-" + System.currentTimeMillis();
                break;
            }
        } while (studentRepository.existsByAdmissionNumberAndIsDeletedFalse(candidate));
        return candidate;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String clientIp = resolveLoginClientIp();
        if (loginAttemptService != null) {
            loginAttemptService.checkBlocked(request.getEmail(), clientIp);
        }

        final Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            boolean newlyBlocked = false;
            if (loginAttemptService != null) {
                try {
                    newlyBlocked = loginAttemptService.recordFailure(request.getEmail(), clientIp);
                } catch (Exception redisEx) {
                    log.warn("Login rate-limit record failed (failing open)", redisEx);
                }
            }
            recordLoginFailureAudit(request.getEmail(), clientIp, newlyBlocked);
            throw ex;
        }

        if (loginAttemptService != null) {
            try {
                loginAttemptService.recordSuccess(request.getEmail(), clientIp);
            } catch (Exception redisEx) {
                log.warn("Login rate-limit clear failed (failing open)", redisEx);
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!user.getIsActive()) {
            throw new ForbiddenException("Account is deactivated. Please contact support.");
        }

        String accessToken = jwtTokenProvider.generateAccessTokenWithClaims(
                user.getEmail(), user.getId(), user.getRole().name(), user.getInstitutionId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .user(buildUserInfo(user))
                .build();
    }

    @Override
    public AuthResponse.UserInfo getCurrentUser(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return buildUserInfo(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Lenient validation: pre-hardening 7d refresh tokens carry no iss and
        // must keep working until natural expiry (alg is still pinned to HS256).
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new ForbiddenException("Invalid or expired refresh token");
        }

        String presentedHash = hashToken(refreshToken);
        if (revokedTokenRepository.existsByTokenHash(presentedHash)) {
            throw new ForbiddenException("Refresh token has been revoked");
        }

        String email = jwtTokenProvider.getEmailFromRefreshToken(refreshToken);
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String newAccessToken = jwtTokenProvider.generateAccessTokenWithClaims(
                user.getEmail(), user.getId(), user.getRole().name(), user.getInstitutionId());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // Rotation: revoke the PRESENTED refresh token before returning its
        // replacement, so a stolen token cannot be replayed after rotation.
        // Immediate revoke races concurrent double-refresh (second tab sees
        // "revoked" and must re-login) — accepted over silent replay windows.
        if (!revokedTokenRepository.existsByTokenHash(presentedHash)) {
            revokedTokenRepository.save(tz.elmkusoma.identity.domain.RevokedToken.builder()
                    .tokenHash(presentedHash)
                    .revokedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build());
        }

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .user(buildUserInfo(user))
                .build();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .ifPresent(user -> {
                    PasswordResetToken resetToken = PasswordResetToken.builder()
                            .token(UUID.randomUUID().toString())
                            .userId(user.getId())
                            .expiresAt(LocalDateTime.now().plusHours(24))
                            .used(false)
                            .build();
                    passwordResetTokenRepository.save(resetToken);
                    log.info("Password reset token created for user: {}", user.getEmail());
                });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // Single 400 for unknown/invalid/expired: distinct 404 vs 403 responses
        // let attackers probe token validity (enumeration oracle).
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Reset token is invalid or has expired"));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Reset token is invalid or has expired");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", resetToken.getUserId()));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Same failure signal as login: a wrong current password throws
        // BadCredentialsException, which GlobalExceptionHandler maps to
        // 401 {"success":false,"error":"Invalid credentials"} — identical to a
        // failed login, so the response reveals nothing beyond "rejected".
        // Neither password value is ever logged.
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getEmail());

        // Token lifecycle note: pre-existing access tokens stay valid until
        // their natural expiry (<=1h, jwt.access-token-expiration-ms=3600000)
        // and refresh-token rotation continues unchanged. There is no
        // per-device/session tracking in this codebase, so selective revocation
        // of other sessions is not possible here — deliberately not built.
    }

    @Override
    public void verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Verification token"));

        if (!verificationToken.isValid()) {
            throw new ForbiddenException("Verification token is invalid or has expired");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", verificationToken.getUserId()));

        user.setIsEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Override
    public void logout(String refreshToken) {
        String hash = hashToken(refreshToken);

        if (!revokedTokenRepository.existsByTokenHash(hash)) {
            tz.elmkusoma.identity.domain.RevokedToken revokedToken = tz.elmkusoma.identity.domain.RevokedToken.builder()
                    .tokenHash(hash)
                    .revokedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            revokedTokenRepository.save(revokedToken);
        }
        log.info("Refresh token revoked successfully");

        // Access-token revocation via jti denylist (Redis, no migration): the
        // access token travels in the Authorization header of this same request,
        // resolved here so the endpoint/service shape is unchanged.
        denyCurrentAccessToken();
    }

    private void denyCurrentAccessToken() {
        if (tenantAwareRedisTemplate == null) {
            return;
        }
        String accessToken = resolveCurrentBearerToken();
        if (accessToken == null) {
            return;
        }
        String jti = jwtTokenProvider.getJtiFromToken(accessToken);
        if (jti == null || jti.isBlank()) {
            // Pre-hardening access token (no jti): expires naturally within 1h.
            return;
        }
        java.util.Date exp = jwtTokenProvider.getExpirationFromToken(accessToken);
        if (exp == null) {
            return;
        }
        long ttlSeconds = (exp.getTime() - System.currentTimeMillis()) / 1000;
        if (ttlSeconds <= 0) {
            return; // Already expired — nothing to deny.
        }
        try {
            // Global key via the RAW template: JwtAuthenticationFilter runs before
            // the organization context is resolved, so a tenant-prefixed key
            // written here would never match the filter's read. jti is globally
            // unique, so no tenant scoping is needed.
            tenantAwareRedisTemplate.getRawTemplate().opsForValue().set(
                    tz.elmkusoma.config.security.JwtTokenProvider.LOGOUT_JTI_DENYLIST_PREFIX + jti,
                    "1", ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ex) {
            // Refresh-token revocation above already succeeded; a denylist write
            // failure must not fail the request (fail open, like the filter).
            log.warn("Logout access-token denylist write failed (failing open)", ex);
        }
    }

    private String resolveCurrentBearerToken() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                String header = sra.getRequest().getHeader("Authorization");
                if (header != null && header.startsWith("Bearer ")) {
                    return header.substring("Bearer ".length());
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve current access token for logout denylist", e);
        }
        return null;
    }

    @Override
    public void sendVerificationCode(SendVerificationCodeRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        java.util.Optional<VerificationCode> existing = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email);
        if (existing.isPresent()) {
            VerificationCode last = existing.get();
            if (last.getCreatedAt() != null && last.getCreatedAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Please wait 60 seconds before requesting a new code");
            }
        }

        String code = String.format("%05d", SECURE_RANDOM.nextInt(100000));

        // A resend supersedes every older still-unused code, so only the latest
        // code can ever verify — a stale code must not stay live after a resend.
        for (VerificationCode stale : verificationCodeRepository.findAllByEmailAndUsedFalseOrderByCreatedAtDesc(email)) {
            stale.setUsed(true);
            verificationCodeRepository.save(stale);
        }

        VerificationCode verificationCode = VerificationCode.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();
        verificationCodeRepository.save(verificationCode);

        log.info("Verification code sent to {}", email);

        // Real delivery: best-effort SMTP hand-off (never fails the request —
        // the code is already persisted above and can be resent after cooldown).
        if (otpMailService != null) {
            otpMailService.sendVerificationCode(email, code);
        }
    }

    @Override
    // Failed verification is a *recorded outcome*, not an aborted mutation: the
    // attempt increment and the used-flag writes that precede each throw must
    // survive. Class-level @Transactional would otherwise roll them back with
    // the IllegalArgumentException (pre-existing: expired/lockout used-flags
    // silently never persisted either).
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verifyCode(VerifyCodeRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String code = request.getCode().trim();

        java.util.Optional<VerificationCode> matched = verificationCodeRepository
                .findTopByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(email, code);

        if (matched.isEmpty()) {
            // Wrong code: count the failed attempt against the latest live code so
            // the 5-attempt cap actually binds. (Previously attempts only moved on
            // a matching lookup, which made the cap unreachable for guessers.)
            VerificationCode latest = verificationCodeRepository
                    .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                    .orElse(null);
            if (latest != null && !latest.isExpired()) {
                if (latest.getAttempts() >= 5) {
                    throw new IllegalArgumentException("Too many failed attempts. Please request a new code.");
                }
                latest.setAttempts(latest.getAttempts() + 1);
                verificationCodeRepository.save(latest);
            }
            // Same message as the expired path (anti-enumeration, locked by tests).
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        VerificationCode verificationCode = matched.get();

        if (verificationCode.isExpired()) {
            verificationCode.setUsed(true);
            verificationCodeRepository.save(verificationCode);
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        if (verificationCode.getAttempts() >= 5) {
            verificationCode.setUsed(true);
            verificationCodeRepository.save(verificationCode);
            throw new IllegalArgumentException("Too many failed attempts. Please request a new code.");
        }

        verificationCode.setAttempts(verificationCode.getAttempts() + 1);
        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);
    }

    private String resolveLoginClientIp() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                return tz.elmkusoma.identity.service.LoginAttemptService
                        .resolveClientIp(sra.getRequest());
            }
        } catch (Exception e) {
            log.debug("Could not resolve client IP for login rate-limit", e);
        }
        return "unknown";
    }

    private void recordLoginFailureAudit(String email, String clientIp, boolean newlyBlocked) {
        if (auditService == null) {
            return;
        }
        try {
            String userAgent = null;
            try {
                var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
                if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                    userAgent = sra.getRequest().getHeader("User-Agent");
                }
            } catch (Exception ignored) {
            }
            auditService.recordSecurityEvent(null, null, email,
                    tz.elmkusoma.audit.domain.SecurityEvent.SecurityEventType.LOGIN_FAILURE,
                    "Failed login attempt",
                    tz.elmkusoma.audit.domain.SecurityEvent.Severity.WARNING,
                    clientIp, userAgent);
            if (newlyBlocked) {
                auditService.recordSecurityEvent(null, null, email,
                        tz.elmkusoma.audit.domain.SecurityEvent.SecurityEventType.ACCOUNT_LOCKED,
                        "Login temporarily blocked after repeated failures",
                        tz.elmkusoma.audit.domain.SecurityEvent.Severity.ERROR,
                        clientIp, userAgent);
            }
        } catch (Exception e) {
            log.warn("Login failure audit failed (must not break login)", e);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private AuthResponse.UserInfo buildUserInfo(User user) {
        String classGroupId = null;
        String institutionId = null;

        var studentOpt = studentRepository.findByUserIdAndIsDeletedFalse(user.getId());
        if (studentOpt.isPresent()) {
            var studentId = studentOpt.get().getId();
            var assignments = studentClassAssignmentRepository
                    .findByStudentIdAndIsDeletedFalse(studentId);
            classGroupId = assignments.stream()
                    .filter(StudentClassAssignment::getIsActive)
                    .findFirst()
                    .map(a -> a.getClassGroupId().toString())
                    .orElse(null);
            // Fallback: an ENROLLED enrollment carries the class group when no term-based
            // class assignment exists yet — learners need a class to see their work.
            if (classGroupId == null && enrollmentRepository != null) {
                classGroupId = enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId)
                        .stream()
                        .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ENROLLED)
                        .map(e -> e.getClassGroupId().toString())
                        .findFirst()
                        .orElse(null);
            }
        }

        if (user.getInstitutionId() != null) {
            institutionId = user.getInstitutionId().toString();
        }

        return AuthResponse.UserInfo.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .emailVerified(user.getIsEmailVerified())
                .institutionId(institutionId)
                .classGroupId(classGroupId)
                .learningLevel(user.getLearningLevel() != null ? user.getLearningLevel().name() : null)
                .secondaryStage(user.getSecondaryStage() != null ? user.getSecondaryStage().name() : null)
                .form(user.getForm() != null ? user.getForm().name() : null)
                .regionId(user.getRegionId() != null ? user.getRegionId().toString() : null)
                .districtId(user.getDistrictId() != null ? user.getDistrictId().toString() : null)
                .build();
    }
}
