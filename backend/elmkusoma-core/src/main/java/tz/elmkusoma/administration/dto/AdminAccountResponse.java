package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAccountResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private String role;
    private String assignedRoleName;
    private List<String> permissions;
    private String scope;           // PLATFORM or institution id
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastModifiedAt;
    private LocalDateTime expiresAt;
    private Long recentActionCount;
}
