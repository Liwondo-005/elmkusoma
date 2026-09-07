package tz.elmkusoma.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private String phone;
    private String role;
    private boolean active;
    private boolean emailVerified;
    private String profileImageUrl;
}
