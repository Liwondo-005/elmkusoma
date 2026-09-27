package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_delegations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdminDelegation extends BaseEntity {

    @Column(name = "delegator_id", nullable = false)
    private UUID delegatorId;

    @Column(name = "delegate_id", nullable = false)
    private UUID delegateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String permissions;

    @Column(nullable = false)
    private String scope = "PLATFORM";

    @Column(nullable = false, length = 60)
    private String authority = "GENERAL_ADMIN";

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_ids", columnDefinition = "jsonb")
    private String resourceIds;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "revocation_reason")
    private String revocationReason;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
