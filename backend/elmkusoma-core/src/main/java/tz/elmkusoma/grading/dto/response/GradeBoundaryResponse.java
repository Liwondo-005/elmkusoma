package tz.elmkusoma.grading.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeBoundaryResponse {
    private UUID id;
    private UUID gradingScaleId;
    private String gradeLabel;
    private String gradeName;
    private BigDecimal minPercentage;
    private BigDecimal maxPercentage;
    private BigDecimal gpaPoints;
    private Integer sortOrder;
}