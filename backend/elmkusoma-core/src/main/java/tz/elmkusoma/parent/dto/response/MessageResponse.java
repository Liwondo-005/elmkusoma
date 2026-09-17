package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private String id;
    private String senderId;
    private String senderName;
    private String recipientId;
    private String recipientName;
    private String subject;
    private String body;
    private String status;
    private Boolean isRead;
    private String messageType;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
