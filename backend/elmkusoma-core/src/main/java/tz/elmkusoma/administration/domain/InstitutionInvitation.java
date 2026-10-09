package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "institution_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "token", nullable = false)
    private String token;

    // @Builder.Default is required, not decorative: Lombok's builder initialises its own
    // fields to null and ignores plain field initialisers unless they are annotated. Without
    // it these two went to the database as NULL and every invitation insert failed the
    // NOT NULL constraint on institution_invitations (surfacing as a 409).
    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
