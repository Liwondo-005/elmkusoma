package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInvitationRequest {
    @NotBlank
    @Size(min = 32, max = 32, message = "Invitation token must be 32 characters")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "Invitation token must be 32 hexadecimal characters")
    private String token;

    @NotBlank
    @Size(min = 8)
    private String password;
}