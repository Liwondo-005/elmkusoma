package tz.elmkusoma.finance.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One detected financial inconsistency, recorded for a human to resolve.
 *
 * <p>Reconciliation exists to <b>find</b> problems, never to fix them. A financial record is
 * never edited to make a report balance - that is how real discrepancies get laundered away.
 * Each exception carries a reference, a type, an owner and a resolution note, and stays visible
 * until somebody decides what to do about it.</p>
 */
@Entity
@Table(name = "reconciliation_exceptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReconciliationException extends BaseEntity {

    public static final String TYPE_PENDING_UNRESOLVED = "PENDING_UNRESOLVED";
    public static final String TYPE_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String TYPE_DUPLICATE_EXTERNAL_REF = "DUPLICATE_EXTERNAL_REF";
    public static final String TYPE_MISSING_ENTITLEMENT = "MISSING_ENTITLEMENT";
    public static final String TYPE_REFUND_MISMATCH = "REFUND_MISMATCH";
    public static final String TYPE_FEE_MISMATCH = "FEE_MISMATCH";
    public static final String TYPE_INCONSISTENT_STATUS = "INCONSISTENT_STATUS";
    public static final String TYPE_MISSING_INTERNAL_RECORD = "MISSING_INTERNAL_RECORD";
    public static final String TYPE_CURRENCY_MISMATCH = "CURRENCY_MISMATCH";
    /**
     * The gateway could not be consulted, so a locally-completed payment's outcome is still
     * uncertain. Deliberately distinct from {@link #TYPE_INCONSISTENT_STATUS}, which means the
     * gateway <b>did</b> answer and disagreed. "We could not ask" must never be reported as
     * "they said no".
     */
    public static final String TYPE_GATEWAY_UNCERTAIN = "GATEWAY_UNCERTAIN";

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_IGNORED = "IGNORED";

    @Column(name = "exception_type", nullable = false, length = 40)
    private String exceptionType;

    @Column(nullable = false, length = 200)
    private String reference;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "TZS";

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_OPEN;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "discovered_at", nullable = false)
    @Builder.Default
    private LocalDateTime discoveredAt = LocalDateTime.now();

    public boolean isOpen() {
        return STATUS_OPEN.equals(status) || STATUS_IN_PROGRESS.equals(status);
    }
}
