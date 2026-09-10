package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentAssignmentResponse {

    private String studentName;
    private String className;
    private List<AssignmentItem> pending;
    private List<AssignmentItem> completed;
    private List<AssignmentItem> overdue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentItem {
        private String id;
        private String title;
        private String subject;
        private LocalDateTime dueDate;
        private Integer totalMarks;
        private Integer obtainedMarks;
        private String status;
        private String remarks;
    }
}
