package tz.elmkusoma.course.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateLiveClassRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotBlank(message = "Scheduled time is required")
    private String scheduledAt;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 480, message = "Duration must not exceed 480 minutes")
    private Integer durationMinutes;

    @Size(max = 500, message = "Meeting URL must not exceed 500 characters")
    private String meetingUrl;

    private UUID subjectId;

    private UUID classGroupId;

    @Min(value = 1, message = "Max participants must be at least 1")
    @Max(value = 10000, message = "Max participants must not exceed 10000")
    private Integer maxParticipants;
}
