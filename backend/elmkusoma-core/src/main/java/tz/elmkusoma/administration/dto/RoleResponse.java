package tz.elmkusoma.administration.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID institutionId) { this.institutionId = institutionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsSystemRole() { return isSystemRole; }
    public void setIsSystemRole(Boolean isSystemRole) { this.isSystemRole = isSystemRole; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static RoleResponse of(UUID id, UUID institutionId, String name,
                                  String displayName, String description,
                                  Boolean isSystemRole, Boolean isActive,
                                  List<String> permissions, LocalDateTime createdAt) {
        RoleResponse resp = new RoleResponse();
        resp.id = id;
        resp.institutionId = institutionId;
        resp.name = name;
        resp.displayName = displayName;
        resp.description = description;
        resp.isSystemRole = isSystemRole;
        resp.isActive = isActive;
        resp.permissions = permissions;
        resp.createdAt = createdAt;
        return resp;
    }
}