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
public class ReportCardResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID academicYearId;
    private UUID termId;
    private String termName;
    private UUID gradingScaleId;
    private BigDecimal totalMarks;
    private BigDecimal averageMark;
    private String overallGrade;
    private BigDecimal gpa;
    private Integer classRank;
    private Integer totalStudentsInClass;
    private String remarks;
    private String status;
    private LocalDateTime publishedAt;
    private List<SubjectGradeResponse> subjectGrades;
    private LocalDateTime createdAt;
}