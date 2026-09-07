package tz.elmkusoma.shared.domain;

import jakarta.persistence.*;
import lombok.*;

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
    private java.util.UUID userId;

    @Column(name = "institution_id", nullable = false)
    private java.util.UUID institutionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public enum Role {
        OWNER,
        ADMIN,
        TEACHER,
        STUDENT
    }
}
