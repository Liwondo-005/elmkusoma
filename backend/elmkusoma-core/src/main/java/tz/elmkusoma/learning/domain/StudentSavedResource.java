package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_saved_resources", uniqueConstraints = {
    @UniqueConstraint(name = "uk_student_resource", columnNames = {"student_id", "resource_id"})
}, indexes = {
    @Index(name = "idx_saved_resources_student", columnList = "student_id"),
    @Index(name = "idx_saved_resources_resource", columnList = "resource_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentSavedResource extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;


    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Explicit getId for BaseEntity inheritance
    public UUID getId() { return getIdDirect(); }
    public void setId(UUID id) { setIdDirect(id); }
}