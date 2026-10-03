package tz.elmkusoma.identity.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mfa_factors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "factor_type", nullable = false)
    private String factorType;

    /**
     * AES-GCM envelope (iv.ciphertext, Base64). The raw authenticator secret
     * is never persisted.
     */
    @Column(name = "secret_ciphertext", nullable = false, length = 512)
    private String secretCiphertext;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
