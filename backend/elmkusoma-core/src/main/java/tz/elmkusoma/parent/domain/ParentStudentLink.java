package tz.elmkusoma.parent.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "parent_student_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentStudentLink extends BaseEntity {

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "relationship_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Parent.RelationshipType relationshipType = Parent.RelationshipType.GUARDIAN;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Parent parent;
}
