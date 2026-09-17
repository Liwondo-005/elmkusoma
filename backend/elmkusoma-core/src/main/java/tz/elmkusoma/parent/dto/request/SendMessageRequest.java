package tz.elmkusoma.parent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Recipient ID is required")
    private UUID recipientId;

    @NotBlank(message = "Subject is required")
    @Size(max = 300, message = "Subject must not exceed 300 characters")
    private String subject;

    @NotBlank(message = "Message body is required")
    @Size(max = 5000, message = "Message body must not exceed 5000 characters")
    private String body;

    @Size(max = 20, message = "Message type must not exceed 20 characters")
    private String messageType = "DIRECT";
}
