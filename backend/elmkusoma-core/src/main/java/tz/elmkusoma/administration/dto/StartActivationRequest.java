package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Step 1 of invitation activation: identifies the invitation and asks for a code to be sent to
 * the address it was issued for.
 *
 * <p>No password here on purpose. The secret is chosen in step 2, at the moment the account is
 * actually created, so it is never transmitted before the mailbox has been proven and it is
 * never transmitted twice.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartActivationRequest {

    @NotBlank
    @Size(min = 32, max = 32, message = "Invitation token must be 32 characters")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "Invitation token must be 32 hexadecimal characters")
    private String token;
}