package tz.elmkusoma.parent.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A receipt for money actually received, linked to exactly one confirmed payment.
 *
 * <p>A receipt is only issued for a payment that reached COMPLETED - a PENDING payment has not
 * been paid and must not produce a receipt. Each payment yields at most one receipt, enforced by
 * a unique index, because a receipt documents a single confirmed payment.</p>
 *
 * <p>Refunds never rewrite a receipt. The original document stays as issued and the refund is
 * recorded as a credit note number and reason on it, so the historical record of what was
 * charged remains intact while the correction is visible alongside it.</p>
 */
@Entity
@Table(name = "payment_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PaymentReceipt extends BaseEntity {

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 64)
    private String receiptNumber;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "service_name", length = 200)
    private String serviceName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "TZS";

    @Column(name = "payment_channel", length = 50)
    private String paymentChannel;

    @Column(name = "provider_reference", length = 200)
    private String providerReference;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "policy_key", length = 100)
    private String policyKey;

    @Column(name = "policy_version")
    private Integer policyVersion;

    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    /** Credit note reference when part or all of this receipt was refunded. */
    @Column(name = "credit_note_number", length = 64)
    private String creditNoteNumber;

    @Column(name = "credit_note_reason", length = 500)
    private String creditNoteReason;

    public boolean hasCreditNote() {
        return creditNoteNumber != null;
    }
}
