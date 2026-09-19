package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeopleMemberResponse {
    private UUID userId;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String phone;
    private String membershipRole;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private String profileImageUrl;
    private LocalDateTime memberSince;
}
