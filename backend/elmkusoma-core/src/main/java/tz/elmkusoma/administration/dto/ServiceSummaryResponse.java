package tz.elmkusoma.administration.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ServiceSummaryResponse {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private String category;
    private Boolean isActive;
    private Boolean requiresVerification;
    private Integer maxSeats;
    private BigDecimal monthlyPrice;
    private String currency;
    private LocalDateTime createdAt;
}
