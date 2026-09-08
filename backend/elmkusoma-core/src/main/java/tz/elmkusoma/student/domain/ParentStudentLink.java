package tz.elmkusoma.student.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "parent_student_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ParentStudentLink extends BaseEntity {

    @Column(name = "parent_user_id", nullable = false)
    private UUID parentUserId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "relationship", nullable = false)
    private String relationship;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
