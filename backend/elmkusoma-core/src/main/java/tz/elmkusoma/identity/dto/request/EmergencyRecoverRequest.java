package tz.elmkusoma.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmergencyRecoverRequest {

    @NotBlank
    @Email
    private String email;

    /**
     * One pre-enrolled single-use recovery code. Consumed on success.
     */
    @NotBlank
    private String recoveryCode;

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword;
}
