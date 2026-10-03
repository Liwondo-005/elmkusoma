package tz.elmkusoma.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MfaConfirmRequest {

    @NotBlank
    private String factorId;

    @NotBlank
    private String code;
}
