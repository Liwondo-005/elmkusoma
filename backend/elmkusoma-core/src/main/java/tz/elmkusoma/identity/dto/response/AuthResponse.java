package tz.elmkusoma.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserInfo user;
    /**
     * Step-up challenge: true when the account holds a verified MFA factor and
     * only a short-lived {@code mfaToken} is issued. The client must complete
     * {@code POST /v1/auth/mfa/verify} to receive real tokens.
     */
    private boolean mfaRequired;
    private String mfaToken;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private boolean emailVerified;
        private String institutionId;
        private String classGroupId;
        private String learningLevel;
        private String secondaryStage;
        private String form;
        private String regionId;
        private String districtId;
    }
}
