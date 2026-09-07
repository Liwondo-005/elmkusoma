package tz.elmkusoma.identity.service;

import tz.elmkusoma.identity.dto.request.*;
import tz.elmkusoma.identity.dto.response.AuthResponse;
import tz.elmkusoma.identity.dto.response.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void verifyEmail(VerifyEmailRequest request);

    void logout(String refreshToken);
}
