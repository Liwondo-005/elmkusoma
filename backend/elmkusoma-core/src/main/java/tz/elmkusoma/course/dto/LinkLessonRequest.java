package tz.elmkusoma.course.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Body for linking an existing Lesson to an existing Live Class
 * (Lesson ↔ Live Class connection).
 */
@Data
public class LinkLessonRequest {

    @NotNull(message = "lessonId is required")
    private UUID lessonId;
}
