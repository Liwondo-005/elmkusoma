package tz.elmkusoma.grading.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One-request class gradebook: every learner of the class with their assignment
 * submissions and assessment results, plus per-student totals. Replaces the N+1
 * client loops over per-student endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradebookResponse {

    private UUID classGroupId;
    private String className;
    private List<Row> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private UUID studentId;
        private String studentName;
        private String admissionNumber;
        private List<Item> assignments;
        private List<Item> assessments;
        /** Sum of graded scores. */
        private Integer totalObtained;
        /** Sum of max marks across graded items. */
        private Integer totalObtainable;
        /** totalObtained / totalObtainable * 100; null when nothing is graded yet. */
        private BigDecimal averagePercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private UUID id;
        private String title;
        private Integer maxMarks;
        /** Score; null when ungraded/unattempted. */
        private Integer score;
        /**
         * Assignments: NOT_SUBMITTED | DRAFT | SUBMITTED | GRADED.
         * Assessments: NOT_ATTEMPTED | COMPLETED.
         */
        private String status;
        private LocalDateTime dueDate;
        private LocalDateTime submittedAt;
        private LocalDateTime gradedAt;
    }
}
