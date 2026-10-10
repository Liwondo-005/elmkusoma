package tz.elmkusoma.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A caller's own enquiry.
 *
 * <p>No message body, no IP, no user agent, no notification detail, no assignment. A visitor
 * can see that they wrote in and where it stands, and nothing about how the platform handles it
 * internally.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageSummary {

    private String reference;
    private String subject;
    private String category;
    private String status;
    private LocalDateTime receivedAt;
    private LocalDateTime lastUpdateAt;
    private long replyCount;
}