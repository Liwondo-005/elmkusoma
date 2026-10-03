package tz.elmkusoma.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 128, message = "Password must be 8-128 characters")
    private String newPassword;

    /**
     * Step-up proof, REQUIRED when the account holds a verified MFA factor:
     * either a current TOTP code or one unused recovery code.
     */
    private String totpCode;

    private String recoveryCode;
}
