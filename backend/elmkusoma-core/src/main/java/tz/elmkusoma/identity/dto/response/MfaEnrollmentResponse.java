package tz.elmkusoma.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaEnrollmentResponse {

    private String factorId;
    private String otpauthUri;
    private String base32Secret;
    private List<String> recoveryCodes;
    private long remainingCodes;
}
