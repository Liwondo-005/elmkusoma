package tz.elmkusoma.certificate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One approval decision for one stage of one certificate.
 *
 * <p>Shared by every certificate family. What varies is how many stages a policy requires, which
 * is configuration rather than a second workflow - see {@code CertificateApprovalPolicy}.</p>
 *
 * <p>Decisions are historical. A rejected or withdrawn row is kept rather than deleted: removing it
 * would let a certificate that was refused be quietly re-approved later with no evidence that it
 * had ever been refused. The unique index that keeps one current row per (certificate, stage)
 * therefore only covers PENDING and APPROVED rows.</p>
 */
// Deliberately no @UniqueConstraint on (certificate_id, stage) here. V161 enforces that with a
// PARTIAL unique index covering only rows that are still current (PENDING/APPROVED). A plain JPA
// unique constraint would be unconditional and would therefore forbid the legitimate case of a
// stage holding a REJECTED decision on record alongside a later re-submitted PENDING one, which is
// exactly the resubmission flow the partial index exists to permit.
@Entity
@Table(name = "certificate_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateApproval extends BaseEntity {

    @Column(name = "certificate_id", nullable = false)
    private UUID certificateId;

    /** Which gate this decision satisfies. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 30)
    private ApprovalStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_status", nullable = false, length = 20)
    private DecisionStatus decisionStatus = DecisionStatus.PENDING;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    /**
     * The template version the actor saw when deciding.
     *
     * <p>A later template edit must not silently make an approval apply to a design nobody
     * approved.</p>
     */
    @Column(name = "template_version")
    private Integer templateVersion;

    /** Required for a rejection; optional for an approval. */
    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @PrePersist
    void stamp() {
        if (decisionStatus == null) {
            decisionStatus = DecisionStatus.PENDING;
        }
    }

    public enum ApprovalStage {
        /** The certificate was prepared and submitted for review. */
        PREPARATION,
        /** Learning or completion evidence was checked against trusted records. */
        VALIDATION,
        /** The institution authorises issuance. */
        APPROVAL
    }

    public enum DecisionStatus {
        PENDING,
        APPROVED,
        REJECTED,
        WITHDRAWN
    }
}
