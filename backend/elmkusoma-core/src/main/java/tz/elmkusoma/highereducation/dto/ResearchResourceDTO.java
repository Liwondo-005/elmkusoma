package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchResourceDTO {

    private UUID id;

    @NotNull(message = "Research Project ID is required")
    private UUID researchProjectId;

    @NotBlank(message = "Resource title is required")
    private String title;

    private String description;

    private String resourceType;

    private String fileUrl;

    private String citation;

    @Builder.Default
    private Integer sortOrder = 0;
}
