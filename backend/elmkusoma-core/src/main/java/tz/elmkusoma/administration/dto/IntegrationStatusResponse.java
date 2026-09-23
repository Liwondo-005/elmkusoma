package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationStatusResponse {
    private String key;
    private String name;
    private String category;
    private String connectionStatus;
    private LocalDateTime lastSuccessAt;
    private Integer failureCount;
    private String webhookStatus;
    private String retryStatus;
    private String configStatus;
    private String diagnostics;
    private String probeDetail;
}
