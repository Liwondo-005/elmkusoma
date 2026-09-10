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
public class ParentResultResponse {

    private String studentName;
    private String className;
    private List<ReportCardItem> reportCards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportCardItem {
        private String id;
        private String term;
        private String academicYear;
        private String overallGrade;
        private Double averageMark;
        private Double gpa;
        private Integer classRank;
        private Integer totalStudentsInClass;
        private String remarks;
        private String status;
        private LocalDateTime publishedAt;
    }
}
