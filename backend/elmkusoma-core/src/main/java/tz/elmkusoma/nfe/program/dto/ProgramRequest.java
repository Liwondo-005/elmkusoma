package tz.elmkusoma.nfe.program.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramRequest {

    @NotBlank(message = "Provider ID is required")
    private String providerId;

    @NotBlank(message = "Program title is required")
    private String title;

    private String description;

    @NotBlank(message = "Program type is required")
    private String programType;

    private String category;

    private String targetAudience;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer maxParticipants;

    private Boolean isPublished;
}
