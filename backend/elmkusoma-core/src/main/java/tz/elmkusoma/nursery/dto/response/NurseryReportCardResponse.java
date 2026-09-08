package tz.elmkusoma.nursery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseryReportCardResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID academicYearId;
    private UUID termId;
    private String termName;
    private String generalRemarks;
    private String teacherComments;
    private String physicalDevelopment;
    private String cognitiveDevelopment;
    private String socialDevelopment;
    private String emotionalDevelopment;
    private String languageDevelopment;
    private String areasOfStrength;
    private String areasForImprovement;
    private String recommendationsForParents;
    private String status;
    private LocalDateTime publishedAt;
    private String preparedByName;
}