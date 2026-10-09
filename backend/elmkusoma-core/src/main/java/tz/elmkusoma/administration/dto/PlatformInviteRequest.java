package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Platform-admin-issued invitation for a role the platform alone may grant
 * (PROVIDER_ADMIN, PROVIDER_STAFF and the platform administration roles).
 *
 * <p>Deliberately has no password field. Provisioning by password means the platform admin
 * chooses a permanent credential, transmits it out of band and never learns when the holder
 * rotates it. An expiring, single-delivery token keeps the platform as the authority while
 * letting the recipient choose their own secret at redemption.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformInviteRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private UUID institutionId;

    @NotBlank
    private String role;

    private String firstName;

    private String lastName;
}
