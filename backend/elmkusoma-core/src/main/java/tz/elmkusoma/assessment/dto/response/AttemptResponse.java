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
public class AttemptResponse {

    private UUID id;
    private UUID assessmentId;
    private UUID studentId;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Boolean isCompleted;
    private List<AnswerResponse> answers;
    private AssessmentResultResponse result;
}
