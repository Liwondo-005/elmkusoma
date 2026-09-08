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
public class SubjectGradeResponse {
    private UUID id;
    private UUID subjectId;
    private String subjectName;
    private BigDecimal marksObtained;
    private String grade;
    private BigDecimal gradePoints;
    private String teacherRemarks;
    private String assessedByName;
}