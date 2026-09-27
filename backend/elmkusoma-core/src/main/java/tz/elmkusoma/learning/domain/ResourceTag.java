package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "resource_tags", uniqueConstraints = {
    @UniqueConstraint(name = "uk_resource_tag_name", columnNames = {"name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ResourceTag extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = false;

    // Explicit getId for BaseEntity inheritance
    public UUID getId() { return getIdDirect(); }
    public void setId(UUID id) { setIdDirect(id); }
}
