package tz.elmkusoma.event.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_registrations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EventRegistration extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "REGISTERED";

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "attended")
    @Builder.Default
    private Boolean attended = false;

    @Column(name = "attended_at")
    private LocalDateTime attendedAt;

    /**
     * The payment that paid for this seat, or null for a free event.
     *
     * <p>A registration and a payment are kept as separate states on purpose. A seat exists
     * before it is paid for, and exists after a refund; collapsing them would mean a pending
     * prompt and a confirmed receipt were the same fact, and the ledger could no longer be
     * reconciled against attendance.</p>
     */
    @Column(name = "payment_id")
    private UUID paymentId;

    /**
     * What was actually taken for these seats, snapshotted at confirmation.
     *
     * <p>Deliberately not recomputed from the event's current price: an event that is repriced
     * next month must not restate what this attendee paid this month.</p>
     */
    @Column(name = "amount_paid", precision = 12, scale = 2)
    private java.math.BigDecimal amountPaid;

    /**
     * Seats held by this registration. One for an individual booking, more for an organiser
     * buying a group. Capacity is consumed by this number, not by the row count, so a group
     * booking genuinely takes the seats it bought.
     */
    @Column(name = "seats", nullable = false, columnDefinition = "integer default 1 not null")
    @Builder.Default
    private Integer seats = 1;

    /**
     * True only when this seat is actually held and paid for.
     *
     * <p>Pending payment and cancelled states are excluded so a queued prompt cannot occupy a
     * seat, and so a cancelled seat is released back to the event immediately.</p>
     */
    public boolean consumesCapacity() {
        return "REGISTERED".equals(status) || "ATTENDED".equals(status);
    }
    @Column(name = "certificate_id")
    private UUID certificateId;

    /**
     * Seats default to one however this row was built.
     *
     * <p>{@code @Builder.Default} makes Lombok move the field initialiser into the builder and
     * leave the field itself unset, so {@code new EventRegistration()} followed by setters -
     * which several existing tests and mappers do - would persist a null seat count and trip the
     * not-null constraint. Setting it on persist instead of in the initialiser covers every
     * construction path: builder, no-args constructor and setters alike.</p>
     */
    @PrePersist
    void defaultSeats() {
        if (seats == null) {
            seats = 1;
        }
    }
    public enum RegistrationStatus {
        REGISTERED, CANCELLED, WAITLISTED, ATTENDED, NO_SHOW, PAYMENT_PENDING
    }
}
