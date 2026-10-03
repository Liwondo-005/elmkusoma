package tz.elmkusoma.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MfaVerifyRequest {

    @NotBlank
    private String mfaToken;

    @NotBlank
    private String code;
}
