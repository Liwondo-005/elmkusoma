package tz.elmkusoma.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Size(max = 4096, message = "Refresh token must not exceed 4096 characters")
    private String refreshToken;
}
