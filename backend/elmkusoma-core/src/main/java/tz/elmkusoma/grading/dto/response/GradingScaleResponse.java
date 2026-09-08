package tz.elmkusoma.grading.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingScaleResponse {
    private UUID id;
    private UUID institutionId;
    private String name;
    private String description;
    private String scaleType;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}