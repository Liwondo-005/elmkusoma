package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyOverviewResponse {

    private String parentName;
    private Integer totalChildren;
    private List<ChildSummary> children;
    private List<ActionItem> todayActions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildSummary {
        private String studentId;
        private String name;
        private String admissionNumber;
        private String className;
        private String educationLevel;
        private String relationshipType;
        private Double attendancePercentage;
        private String latestGrade;
        private Boolean isPrimary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        private String type;
        private String title;
        private String detail;
        private String childName;
        private LocalDate date;
        private String priority;
    }
}
