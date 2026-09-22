package tz.elmkusoma.learner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class LearningGoalRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String goalType = "PERSONAL";

    private LocalDate targetDate;

    private Integer progressPercentage = 0;

    private String status = "ACTIVE";
}