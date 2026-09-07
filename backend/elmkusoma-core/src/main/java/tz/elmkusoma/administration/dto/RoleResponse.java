package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private UUID id;
    private UUID institutionId;
    private String name;
    private String displayName;
    private String description;
    private Boolean isSystemRole;
    private Boolean isActive;
    private List<String> permissions;
    private LocalDateTime createdAt;
}
