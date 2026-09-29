package tz.elmkusoma.assessment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/** Autosave payload: persists one answer for an active attempt without grading it. */
@Data
public class SaveAnswerRequest {

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    /** Selected option id for MCQ / TRUE_FALSE questions. */
    private UUID selectedOptionId;

    /** Typed answer for SHORT_ANSWER / ESSAY questions. */
    private String textAnswer;
}
