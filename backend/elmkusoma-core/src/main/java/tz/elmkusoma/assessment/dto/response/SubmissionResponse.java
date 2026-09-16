package tz.elmkusoma.assessment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {

    private UUID attemptId;
    private UUID studentId;
    private UUID assessmentId;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Boolean isCompleted;
    private List<AnswerResponse> answers;
    private Integer totalScore;
    private Boolean isPassed;
    private UUID gradedBy;
    private LocalDateTime gradedAt;
    private String feedback;
}
