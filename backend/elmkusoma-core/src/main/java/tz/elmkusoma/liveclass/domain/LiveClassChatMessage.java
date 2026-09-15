package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

@Entity
@Table(name = "live_class_chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveClassChatMessage extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private java.util.UUID liveClassId;

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "message_type")
    @Builder.Default
    private String messageType = "CHAT";

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private java.time.LocalDateTime sentAt = java.time.LocalDateTime.now();
}
