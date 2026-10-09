package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Step 2 of invitation activation: the emailed code proves control of the invited mailbox, and
 * the password chosen here becomes the new account's secret.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmActivationRequest {

    @NotBlank
    @Size(min = 32, max = 32, message = "Invitation token must be 32 characters")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "Invitation token must be 32 hexadecimal characters")
    private String token;

    @NotBlank
    @Pattern(regexp = "^[0-9]{5}$", message = "Verification code must be 5 digits")
    private String code;

    @NotBlank
    @Size(min = 8, max = 128, message = "Password must be 8-128 characters")
    private String password;
}