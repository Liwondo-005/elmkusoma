package tz.elmkusoma.assessment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {

    private UUID id;
    private UUID attemptId;
    private UUID questionId;
    private UUID selectedOptionId;
    private String textAnswer;
    private Boolean isCorrect;
    private Integer marksObtained;
}
