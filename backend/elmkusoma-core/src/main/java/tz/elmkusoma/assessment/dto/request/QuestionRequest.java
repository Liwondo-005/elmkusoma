package tz.elmkusoma.assessment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tz.elmkusoma.assessment.domain.QuestionType;

import java.util.List;
import java.util.UUID;

@Data
public class QuestionRequest {

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotNull(message = "Marks are required")
    @Min(1)
    private Integer marks;

    @Min(0)
    private Integer sortOrder;

    private List<OptionRequest> options;
}
