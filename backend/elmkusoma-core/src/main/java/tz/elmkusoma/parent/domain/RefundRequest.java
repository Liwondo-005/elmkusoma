package tz.elmkusoma.parent.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A refund <b>request</b>. It is deliberately not a refund.
 *
 * <p>No live payment gateway is enabled, so nothing here can have returned money to anybody.
 * The lifecycle therefore keeps REQUESTED, UNDER_REVIEW, APPROVED, PROCESSING and COMPLETED as
 * distinct states: an approved refund is a decision to refund, and only COMPLETED - which cannot
 * be reached without actual verified processing - represents money that has actually moved. The
 * UI must never present APPROVED or PROCESSING as "refunded".</p>
 */
@Entity
@Table(name = "refund_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RefundRequest extends BaseEntity {

    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String ENTITLEMENT_REVOKE = "REVOKE";
    public static final String ENTITLEMENT_RETAIN = "RETAIN";
    public static final String ENTITLEMENT_PENDING = "PENDING";

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    /** Requested refund. May be less than the payment - partial refunds are supported. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = STATUS_REQUESTED;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "refund_reference", length = 200)
    private String refundReference;

    /** What happened to the entitlement: REVOKE, RETAIN, or PENDING a decision. */
    @Column(name = "entitlement_action", length = 30)
    @Builder.Default
    private String entitlementAction = ENTITLEMENT_PENDING;

    /** A refund request cannot become a refund without passing through a decision. */
    public boolean isOpen() {
        return STATUS_REQUESTED.equals(status) || STATUS_UNDER_REVIEW.equals(status)
                || STATUS_APPROVED.equals(status) || STATUS_PROCESSING.equals(status);
    }

    public boolean isTerminal() {
        return STATUS_COMPLETED.equals(status) || STATUS_REJECTED.equals(status)
                || STATUS_CANCELLED.equals(status);
    }
}
