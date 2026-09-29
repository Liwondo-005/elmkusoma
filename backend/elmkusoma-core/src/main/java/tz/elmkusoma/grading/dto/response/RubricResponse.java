package tz.elmkusoma.grading.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Rubric with its criteria (wires the grading_rubrics / rubric_criteria tables). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID subjectId;
    private BigDecimal totalPoints;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<Criteria> criteria;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Criteria {
        private UUID id;
        private String name;
        private String description;
        private BigDecimal maxPoints;
        private Integer sortOrder;
    }
}
