package tz.elmkusoma.shared.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "institution_memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "campus_id")
    private UUID campusId;

    /**
     * Per-provider scope (audit B-14 / X-7). When set, this account administers exactly this
     * education provider inside the institution. When null the membership keeps its historical
     * institution-wide meaning, so no existing account changes behaviour.
     */
    @Column(name = "provider_id")
    private UUID providerId;

    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID providerId) {
        this.providerId = providerId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public UUID getCampusId() {
        return campusId;
    }

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Role {
        OWNER,
        ADMIN,
        TEACHER,
        STUDENT,
        PARENT,
        OTHER_LEARNER,
        INSTITUTION_ADMIN,
        NATIONAL_ADMIN,
        INSTRUCTOR
    }
}
