package tz.elmkusoma.nursery.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateNurseryActivityRequest {

    @NotNull(message = "Class group ID is required")
    private UUID classGroupId;

    @NotBlank(message = "Activity name is required")
    @Size(max = 200)
    private String activityName;

    @NotBlank(message = "Activity type is required")
    private String activityType;

    @Size(max = 2000)
    private String description;

    private String instructions;

    private Integer durationMinutes;

    private Integer maxParticipants;

    private String materialsNeeded;

    private String learningObjectives;

    private String ageGroup;

    @NotNull(message = "Activity date is required")
    private LocalDate activityDate;
}