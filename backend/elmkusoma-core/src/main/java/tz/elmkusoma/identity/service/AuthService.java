package tz.elmkusoma.identity.service;

import tz.elmkusoma.identity.dto.request.*;
import tz.elmkusoma.identity.dto.response.AuthResponse;
import tz.elmkusoma.identity.dto.response.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    AuthResponse.UserInfo getCurrentUser(String email);

    /**
     * Starts a recovery. Returns the raw one-time token for the delivery
     * layer (email/SMS); only its hash is persisted. Callers must never put
     * the raw value in API responses or logs. Returns {@code null} when the
     * account does not exist (response stays generic either way).
     */
    String forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    /**
     * Authenticated password change. Never uses a reset token: the caller
     * proves possession of the current password instead.
     */
    void changePassword(String email, ChangePasswordRequest request);

    void verifyEmail(VerifyEmailRequest request);

    void logout(String refreshToken);

    void sendVerificationCode(SendVerificationCodeRequest request);

    void verifyCode(VerifyCodeRequest request);
}
