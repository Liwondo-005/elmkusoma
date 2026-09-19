package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {
    private UUID id;
    private UUID parentId;
    private UUID studentId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String serviceType;
    private LocalDateTime createdAt;
}
