package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean isActive;
    private UUID institutionId;
    private java.time.LocalDateTime createdAt;
}
