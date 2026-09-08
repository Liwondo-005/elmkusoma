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
public class OptionResponse {

    private UUID id;
    private UUID questionId;
    private String optionText;
    private Boolean isCorrect;
    private Integer sortOrder;
}
