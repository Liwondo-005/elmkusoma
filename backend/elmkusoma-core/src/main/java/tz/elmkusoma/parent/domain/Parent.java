package tz.elmkusoma.parent.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;
import tz.elmkusoma.shared.domain.User;

import java.util.UUID;

@Entity
@Table(name = "parents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parent extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "relationship_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RelationshipType relationshipType = RelationshipType.GUARDIAN;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public enum RelationshipType {
        FATHER,
        MOTHER,
        GUARDIAN,
        OTHER
    }

    public String getFullName() {
        if (user != null) {
            return user.getFullName();
        }
        return null;
    }
}
