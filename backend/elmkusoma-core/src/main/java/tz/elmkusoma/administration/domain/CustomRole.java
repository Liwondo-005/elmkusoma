package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "custom_roles")
public class CustomRole extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_system_role", nullable = false)
    private Boolean isSystemRole = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public UUID getId() { return super.getId(); }
    public UUID getInstitutionId() { return super.getInstitutionId(); }
    public void setInstitutionId(UUID institutionId) { super.setInstitutionId(institutionId); }
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

    public static CustomRole of(String name, String displayName,
                                String description, UUID institutionId) {
        CustomRole role = new CustomRole();
        role.name = name;
        role.displayName = displayName;
        role.description = description;
        role.institutionId = institutionId;
        role.isSystemRole = false;
        role.isActive = true;
        return role;
    }
}