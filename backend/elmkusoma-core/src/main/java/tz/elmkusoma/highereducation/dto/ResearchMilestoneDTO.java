package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchMilestoneDTO {

    private UUID id;

    @NotNull(message = "Research Project ID is required")
    private UUID researchProjectId;

    @NotBlank(message = "Milestone title is required")
    private String title;

    private String description;

    private LocalDate dueDate;

    @Builder.Default
    private Boolean isCompleted = false;

    private LocalDate completedDate;

    @Builder.Default
    private Integer sortOrder = 0;
}
