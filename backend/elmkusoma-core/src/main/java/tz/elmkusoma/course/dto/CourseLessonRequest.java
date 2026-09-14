package tz.elmkusoma.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseLessonRequest {

    @NotBlank(message = "Lesson title is required")
    private String title;

    @NotBlank(message = "Content type is required")
    private String contentType;

    private String contentUrl;

    private Integer durationMinutes;

    private Integer sortOrder;

    private Boolean isFree;

    private UUID moduleId;
}
