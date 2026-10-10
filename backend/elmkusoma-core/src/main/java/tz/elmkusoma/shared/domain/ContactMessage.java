package tz.elmkusoma.shared.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A public enquiry submitted through the contact form.
 *
 * <p>Deliberately its own table rather than a row in {@code support_tickets}: that table
 * declares {@code user_id NOT NULL}, and a public enquiry has no authenticated user. Forcing
 * one would mean inventing a user per anonymous submission. Keeping them separate also means
 * the two lifecycles stay honest — a public enquiry is not a support case until staff say so.</p>
 *
 * <p>{@code notificationStatus} exists because a stored row is not evidence of delivery. The
 * enquiry is committed first, then notification is attempted, and the outcome is recorded as
 * it happens.</p>
 */
@Entity
@Table(name = "contact_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ContactMessage extends tz.elmkusoma.common.BaseEntity {


    @Column(name = "reference", nullable = false, length = 32)
    private String reference;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, length = 160)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** NEW | IN_PROGRESS | AWAITING_RESPONSE | RESOLVED | CLOSED */
    @Column(nullable = false, length = 24)
    @Builder.Default
    private String status = "NEW";

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String priority = "NORMAL";

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    /** NOT_CONFIGURED | PENDING | SENT | FAILED */
    @Column(name = "notification_status", nullable = false, length = 20)
    @Builder.Default
    private String notificationStatus = "PENDING";

    @Column(name = "notification_error", length = 500)
    private String notificationError;

    @Builder.Default
    @Column(name = "notification_attempts", nullable = false)
    private Integer notificationAttempts = 0;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    /** Set when the enquirer's address belongs to a real account, for "my enquiries". */
    @Column(name = "user_id")
    private UUID userId;

    /** Retained for abuse investigation. Never returned on any public endpoint. */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;
}