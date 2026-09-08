package tz.elmkusoma.assessment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private UUID id;
    private UUID assessmentId;
    private String questionType;
    private String questionText;
    private Integer marks;
    private Integer sortOrder;
    private List<OptionResponse> options;
}
