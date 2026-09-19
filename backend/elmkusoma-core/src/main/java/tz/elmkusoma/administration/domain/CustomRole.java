package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "custom_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
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

    public static CustomRole of(String name, String displayName,
                                String description, UUID institutionId) {
        CustomRole role = new CustomRole();
        role.setName(name);
        role.setDisplayName(displayName);
        role.setDescription(description);
        role.setInstitutionId(institutionId);
        role.setIsSystemRole(false);
        role.setIsActive(true);
        return role;
    }
}
