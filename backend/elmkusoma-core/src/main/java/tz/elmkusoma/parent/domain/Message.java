package tz.elmkusoma.parent.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "parent_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Message extends BaseEntity {

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "subject", nullable = false, length = 300)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "SENT";

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType = "DIRECT";

    public enum MessageStatus {
        SENT, DELIVERED, READ, ARCHIVED
    }

    public enum MessageType {
        DIRECT, INQUIRY, FEEDBACK, URGENT
    }
}
