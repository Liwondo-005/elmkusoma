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
import tz.elmkusoma.config.security.RateLimitService;
import tz.elmkusoma.exception.RateLimitExceededException;
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
    private final RateLimitService rateLimitService;

    private static final long MINUTE_MILLIS = 60_000L;
    private static final long HOUR_MILLIS = 3_600_000L;

    /**
     * Per-account throttle evaluated on the parsed DTO (the filter only sees
     * IPs). Keyed on the submitted identifier so hammering one account can
     * never lock out others — and the 429 is identical for existing and
     * non-existing accounts.
     */
    private void checkAccountBudget(String bucket, String account, int limit, long windowMillis) {
        if (rateLimitService == null || account == null || account.isBlank()) {
            return;
        }
        if (!rateLimitService.allow(bucket + ":" + account.toLowerCase().trim(), limit, windowMillis)) {
            throw new RateLimitExceededException();
        }
    }
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
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    /**
     * Bumps the session-invalidation counter, killing every previously issued
     * access and refresh token for the user on their next use.
     */
    private void bumpSecurityVersion(User user) {
        long current = user.getSecurityVersion() != null ? user.getSecurityVersion() : 1L;
        user.setSecurityVersion(current + 1);
    }

    private void revokeRefreshToken(String refreshToken) {
        String hash = hashToken(refreshToken);
        if (!revokedTokenRepository.existsByTokenHash(hash)) {
            tz.elmkusoma.identity.domain.RevokedToken revokedToken = tz.elmkusoma.identity.domain.RevokedToken.builder()
                    .tokenHash(hash)
                    .revokedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            revokedTokenRepository.save(revokedToken);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tz.elmkusoma.administration.service.PlatformPolicyService platformPolicyService;

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
                            tz.elmkusoma.enrollment.repository.EnrollmentRepository enrollmentRepository,
                            RateLimitService rateLimitService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.rateLimitService = rateLimitService;
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
            throw new IllegalArgumentException("An account with this email already exists");
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
                user.getEmail(), user.getId(), user.getRole().name(), instId, user.getSecurityVersion());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getSecurityVersion());

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
        // Per-account throttle before the (timed) password check so success
        // and failure are indistinguishable and cheap to reject when hot.
        checkAccountBudget("login", request.getEmail(), 10, MINUTE_MILLIS);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!user.getIsActive()) {
            throw new ForbiddenException("Account is deactivated. Please contact support.");
        }

        String accessToken = jwtTokenProvider.generateAccessTokenWithClaims(
                user.getEmail(), user.getId(), user.getRole().name(), user.getInstitutionId(),
                user.getSecurityVersion());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getSecurityVersion());

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

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ForbiddenException("Invalid or expired refresh token");
        }

        if (revokedTokenRepository.existsByTokenHash(hashToken(refreshToken))) {
            throw new ForbiddenException("Refresh token has been revoked");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Session-invalidation gate: a refresh token minted before the last
        // password reset/change cannot be used anymore.
        Long tokenVersion = jwtTokenProvider.getSecurityVersionFromToken(refreshToken);
        if (tokenVersion == null || !tokenVersion.equals(user.getSecurityVersion())) {
            throw new ForbiddenException("Refresh token has been revoked");
        }

        String newAccessToken = jwtTokenProvider.generateAccessTokenWithClaims(
                user.getEmail(), user.getId(), user.getRole().name(), user.getInstitutionId(),
                user.getSecurityVersion());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getSecurityVersion());

        // Destructive rotation: the presented refresh token dies with this call.
        revokeRefreshToken(refreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .user(buildUserInfo(user))
                .build();
    }

    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        // Same budget whether or not the account exists: no oracle.
        checkAccountBudget("forgot", request.getEmail(), 10, HOUR_MILLIS);
        // Invalidate any outstanding tokens first so only the newest can run.
        userRepository.findByEmailAndIsDeletedFalse(request.getEmail()).ifPresent(user ->
            passwordResetTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getUsed()))
                    .forEach(t -> {
                        t.setUsed(true);
                        passwordResetTokenRepository.save(t);
                    }));
        final String[] issued = { null };
        userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .ifPresent(user -> {
                    String rawToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();
                    PasswordResetToken resetToken = PasswordResetToken.builder()
                            // Only the hash is persisted; the raw value is
                            // handed to the delivery layer via the return value.
                            .token(hashToken(rawToken))
                            .userId(user.getId())
                            .expiresAt(LocalDateTime.now().plusHours(24))
                            .used(false)
                            .build();
                    passwordResetTokenRepository.save(resetToken);
                    log.info("Password reset token created for user: {}", user.getEmail());
                    issued[0] = rawToken;
                });
        return issued[0];
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // Guessing budget per presented token value.
        checkAccountBudget("reset-token", request.getToken(), 10, MINUTE_MILLIS);
        // Tokens are stored hashed; the presented raw value is hashed for lookup.
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(hashToken(request.getToken()))
                .orElseThrow(() -> new ResourceNotFoundException("Reset token"));

        if (!resetToken.isValid()) {
            throw new ForbiddenException("Reset token is invalid or has expired");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", resetToken.getUserId()));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // Invalidate every previously issued session/token for this account.
        bumpSecurityVersion(user);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        // Burn any other outstanding reset tokens so only this flow could run.
        passwordResetTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(t -> !Boolean.TRUE.equals(t.getUsed()) && !t.getId().equals(resetToken.getId()))
                .forEach(t -> {
                    t.setUsed(true);
                    passwordResetTokenRepository.save(t);
                });

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    @Override
    public void verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenAndUsedFalse(hashToken(request.getToken()))
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
        revokeRefreshToken(refreshToken);
        log.info("Refresh token revoked successfully");
    }

    @Override
    public void sendVerificationCode(SendVerificationCodeRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        checkAccountBudget("send-code", email, 10, HOUR_MILLIS);

        java.util.Optional<VerificationCode> existing = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email);
        if (existing.isPresent()) {
            VerificationCode last = existing.get();
            if (last.getCreatedAt() != null && last.getCreatedAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Please wait 60 seconds before requesting a new code");
            }
        }

        // Cryptographically secure code; only its hash is persisted and the raw
        // value is never logged (it must travel only through the delivery channel).
        String code = String.format("%05d", SECURE_RANDOM.nextInt(100000));

        VerificationCode verificationCode = VerificationCode.builder()
                .email(email)
                .code(hashToken(code))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();
        verificationCodeRepository.save(verificationCode);

        log.info("Verification code issued for user: {}", email);
    }

    @Override
    public void verifyCode(VerifyCodeRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String code = request.getCode().trim();
        checkAccountBudget("verify-code", email, 30, HOUR_MILLIS);

        VerificationCode verificationCode = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code"));

        if (verificationCode.isExpired()) {
            verificationCode.setUsed(true);
            verificationCodeRepository.save(verificationCode);
            throw new IllegalArgumentException("Verification code has expired. Please request a new one.");
        }

        if (verificationCode.getAttempts() >= 5) {
            verificationCode.setUsed(true);
            verificationCodeRepository.save(verificationCode);
            throw new IllegalArgumentException("Too many failed attempts. Please request a new code.");
        }

        // Wrong guesses count against the attempt cap (previously only a
        // correct code touched the row, allowing unlimited guessing).
        if (!hashToken(code).equals(verificationCode.getCode())) {
            verificationCode.setAttempts(verificationCode.getAttempts() + 1);
            verificationCodeRepository.save(verificationCode);
            throw new IllegalArgumentException("Invalid verification code");
        }

        verificationCode.setAttempts(verificationCode.getAttempts() + 1);
        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);
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
