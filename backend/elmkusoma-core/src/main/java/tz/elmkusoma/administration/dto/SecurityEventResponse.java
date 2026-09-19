package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEventResponse {
    private UUID id;
    private UUID userId;
    private String eventType;
    private String severity;
    private String description;
    private String ipAddress;
    private Boolean resolved;
    private LocalDateTime createdAt;
}
