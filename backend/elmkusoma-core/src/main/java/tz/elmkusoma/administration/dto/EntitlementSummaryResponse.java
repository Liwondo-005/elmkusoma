package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EntitlementSummaryResponse {
    private UUID id;
    private UUID userId;
    private UUID studentId;
    private String serviceType;
    private UUID serviceId;
    private String status;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
