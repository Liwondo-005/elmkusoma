package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.highereducation.domain.ProgrammeType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeDTO {

    private UUID id;

    @NotBlank(message = "Programme name is required")
    private String name;

    private String code;

    private String description;

    @NotNull(message = "Programme type is required")
    private ProgrammeType programmeType;

    private EducationLevel educationLevel;

    private Integer durationMonths;

    /**
     * Owning department (Department -> Programme). Nullable: institutions that do not model
     * departments, and existing programmes, keep working without one.
     */
    private UUID departmentId;

    /** Denormalised for display only; never accepted as input. */
    private String departmentName;

    /**
     * Human-readable duration derived from durationMonths (12 -> "1 year",
     * 18 -> "1.5 years", 24 -> "2 years", 36 -> "3 years"). Read-only.
     */
    private String durationDisplay;

    @Builder.Default
    private Boolean isActive = true;
}
