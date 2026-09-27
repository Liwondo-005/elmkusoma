package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "resource_taggings", uniqueConstraints = {
    @UniqueConstraint(name = "uk_resource_tagging", columnNames = {"resource_id", "tag_id"})
}, indexes = {
    @Index(name = "idx_resource_taggings_resource", columnList = "resource_id"),
    @Index(name = "idx_resource_taggings_tag", columnList = "tag_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ResourceTagging extends BaseEntity {

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "tag_id", nullable = false)
    private UUID tagId;

    @Column(name = "tagged_by")
    private UUID taggedBy;

    // Explicit getId for BaseEntity inheritance
    public UUID getId() { return getIdDirect(); }
    public void setId(UUID id) { setIdDirect(id); }
}
