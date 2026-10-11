package tz.elmkusoma.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * What the submitter is told, and nothing more.
 *
 * <p>Deliberately does not echo the stored message body, the internal status workflow, the
 * assigned staff member, or the notification error. The reference is the only handle a visitor
 * needs, and the notification status is included precisely because the form must not imply a
 * message was delivered when it was not.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactReceipt {

    private String reference;

    /** NEW at the point of receipt. */
    private String status;

    private LocalDateTime receivedAt;

    /** NOT_CONFIGURED | PENDING | SENT | FAILED - what actually happened to the notification. */
    private String notificationStatus;
}