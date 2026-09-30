package tz.elmkusoma.offering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.learning.domain.Resource;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningOfferingRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 300, message = "Title must be at most 300 characters")
    private String title;

    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    private UUID subjectId;

    private UUID courseId;

    private EducationLevel educationLevel;

    private String thumbnailUrl;

    private Resource.ResourceVisibility visibility;

    private String status;

    private Boolean independent;
}
