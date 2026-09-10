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
public class CourseRequest {

    @NotBlank(message = "Course title is required")
    private String title;

    private String description;

    private UUID subjectId;

    private String level;

    private String category;

    private String thumbnailUrl;

    private Boolean isPublished;

    private Boolean isFeatured;
}
