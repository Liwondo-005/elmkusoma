package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin-facing view of a contact enquiry.
 *
 * <p>Includes the IP address and user agent: support needs them to judge abuse, and this is
 * only ever built behind {@code hasRole('ADMIN')}. No other role receives this shape.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageViewDto {

    private UUID id;
    private String reference;
    private String name;
    private String email;
    private String category;
    private String subject;
    private String message;
    private String status;
    private String priority;
    private UUID assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String notificationStatus;
    private String notificationError;
    private Integer notificationAttempts;
    private LocalDateTime notifiedAt;
    private UUID userId;
    private String ipAddress;
    private String userAgent;
}