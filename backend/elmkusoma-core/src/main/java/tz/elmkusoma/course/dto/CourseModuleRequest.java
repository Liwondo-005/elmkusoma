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
public class CourseModuleRequest {

    @NotBlank(message = "Module title is required")
    private String title;

    /**
     * Module code for programme-style modules. Optional, and deliberately not required, so
     * standalone provider courses are never forced into university-style codes.
     */
    private String moduleCode;

    private String description;

    private Integer sortOrder;

    private UUID courseId;
}
