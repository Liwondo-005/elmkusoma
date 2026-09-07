package tz.elmkusoma.assessment.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SubmitAssessmentRequest {

    private List<AnswerSubmission> answers;

    @Data
    public static class AnswerSubmission {
        private UUID questionId;
        private UUID selectedOptionId;
        private String textAnswer;
    }
}
