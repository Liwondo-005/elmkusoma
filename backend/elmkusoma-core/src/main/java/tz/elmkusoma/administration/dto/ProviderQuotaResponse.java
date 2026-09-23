package tz.elmkusoma.administration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProviderQuotaResponse {
    private UUID id;
    private UUID providerId;
    private UUID serviceId;
    private String serviceName;
    private String serviceCode;
    private String status;
    private Integer seatsUsed;
    private Integer maxSeats;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
