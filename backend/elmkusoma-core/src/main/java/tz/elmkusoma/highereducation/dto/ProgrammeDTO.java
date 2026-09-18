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

    private Integer creditHours;

    @Builder.Default
    private Boolean isActive = true;
}
