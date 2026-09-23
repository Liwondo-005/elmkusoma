package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventResponse {
    private UUID id;
    private String source;
    private String eventType;
    private String verificationStatus;
    private String processingResult;
    private String errorDetails;
    private Integer retryCount;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
}
