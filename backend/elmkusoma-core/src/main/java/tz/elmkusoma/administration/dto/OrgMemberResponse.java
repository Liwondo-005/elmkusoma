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
public class OrgMemberResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String userRole;
    private String membershipRole;
    private Boolean isActive;
    private Boolean userActive;
    private LocalDateTime joinedAt;
}
