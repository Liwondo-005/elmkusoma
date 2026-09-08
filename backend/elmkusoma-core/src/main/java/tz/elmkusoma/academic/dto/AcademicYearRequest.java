package tz.elmkusoma.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.academic.domain.EducationLevel;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearRequest {

    @NotNull(message = "Institution ID is required")
    private UUID institutionId;

    @NotNull(message = "Education level is required")
    private EducationLevel educationLevel;

    @NotBlank(message = "Year label is required")
    private String yearLabel;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean isCurrent = false;
}
