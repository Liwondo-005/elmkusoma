package tz.elmkusoma.grading.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private BigDecimal minMark;
    private BigDecimal maxMark;
    private Boolean isDefault;
    private Boolean isActive;
    private List<GradeBoundaryResponse> gradeBoundaries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}