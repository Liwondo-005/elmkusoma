package tz.elmkusoma.identity.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.identity.dto.request.*;
import tz.elmkusoma.identity.dto.response.AuthResponse;
import tz.elmkusoma.identity.service.AuthService;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        AuthResponse.UserInfo userInfo = authService.getCurrentUser(email);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted().body(ApiResponse.success("If the email exists, a reset link has been sent", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        AuthResponse response = authService.verifyMfa(request);
        return ResponseEntity.ok(ApiResponse.success("MFA verification successful", response));
    }

    @PostMapping("/mfa/enroll")
    public ResponseEntity<ApiResponse<tz.elmkusoma.identity.dto.response.MfaEnrollmentResponse>> enrollMfa() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(ApiResponse.success(
                authService.enrollMfa(authentication.getName())));
    }

    @PostMapping("/mfa/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmMfa(@Valid @RequestBody MfaConfirmRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.confirmMfa(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Authenticator enabled", null));
    }

    @GetMapping("/mfa/status")
    public ResponseEntity<ApiResponse<tz.elmkusoma.identity.dto.response.MfaStatusResponse>> mfaStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(ApiResponse.success(authService.mfaStatus(authentication.getName())));
    }

    @PostMapping("/mfa/codes/regenerate")
    public ResponseEntity<ApiResponse<tz.elmkusoma.identity.dto.response.MfaStatusResponse>> regenerateCodes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(ApiResponse.success(
                authService.regenerateRecoveryCodes(authentication.getName())));
    }

    @PostMapping("/sessions/revoke-all")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.revokeAllSessions(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked. Please sign in again.", null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        authService.sendVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.success("A 5-digit verification code has been sent to your email", null));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        authService.verifyCode(request);
        return ResponseEntity.ok(ApiResponse.success("Verification successful", null));
    }
}
