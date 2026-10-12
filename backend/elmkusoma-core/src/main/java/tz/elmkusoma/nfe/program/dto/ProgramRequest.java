package tz.elmkusoma.nfe.program.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

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

    /** Ecosystem link (audit B-11): optional platform course this program delivers. */
    private UUID courseId;

    /** When true, learners of the linked course may enrol on this program. */
    private Boolean enrollmentOpen;
}
