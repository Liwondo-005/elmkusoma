package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resource_annotations", indexes = {
    @Index(name = "idx_annotations_resource", columnList = "resource_id"),
    @Index(name = "idx_annotations_student", columnList = "student_id"),
    @Index(name = "idx_annotations_parent", columnList = "parent_annotation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ResourceAnnotation extends BaseEntity {

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "position_data", columnDefinition = "JSONB")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String positionData;

    @Column(name = "is_private")
    @Builder.Default
    private Boolean isPrivate = true;

    @Column(name = "parent_annotation_id")
    private UUID parentAnnotationId;




    // Explicit getId for BaseEntity inheritance
    public UUID getId() { return getIdDirect(); }
    public void setId(UUID id) { setIdDirect(id); }
}