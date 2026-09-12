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
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.identity.domain.EmailVerificationToken;
import tz.elmkusoma.identity.domain.PasswordResetToken;
import tz.elmkusoma.identity.dto.request.*;
import tz.elmkusoma.identity.dto.response.AuthResponse;
import tz.elmkusoma.identity.repository.EmailVerificationTokenRepository;
import tz.elmkusoma.identity.repository.PasswordResetTokenRepository;
import tz.elmkusoma.identity.service.AuthService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDateTime;
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

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           EmailVerificationTokenRepository emailVerificationTokenRepository,
                           StudentRepository studentRepository,
                           StudentClassAssignmentRepository studentClassAssignmentRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.studentRepository = studentRepository;
        this.studentClassAssignmentRepository = studentClassAssignmentRepository;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User.Role role;
        try {
            role = User.Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }

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

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .user(buildUserInfo(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!user.getIsActive()) {
            throw new ForbiddenException("Account is deactivated. Please contact support.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
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

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ForbiddenException("Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

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
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Reset token"));

        if (!resetToken.isValid()) {
            throw new ForbiddenException("Reset token is invalid or has expired");
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
        log.info("Logout requested for token");
    }

    private AuthResponse.UserInfo buildUserInfo(User user) {
        String classGroupId = null;
        String institutionId = null;

        var studentOpt = studentRepository.findByUserIdAndIsDeletedFalse(user.getId());
        if (studentOpt.isPresent()) {
            var assignments = studentClassAssignmentRepository
                    .findByStudentIdAndIsDeletedFalse(studentOpt.get().getId());
            classGroupId = assignments.stream()
                    .filter(StudentClassAssignment::getIsActive)
                    .findFirst()
                    .map(a -> a.getClassGroupId().toString())
                    .orElse(null);
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
                .build();
    }
}
