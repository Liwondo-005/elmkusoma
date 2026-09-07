package tz.elmkusoma.assessment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResultResponse {

    private UUID id;
    private UUID assessmentId;
    private UUID studentId;
    private UUID attemptId;
    private Integer totalScore;
    private Boolean isPassed;
    private UUID gradedBy;
    private LocalDateTime gradedAt;
    private String feedback;
}
