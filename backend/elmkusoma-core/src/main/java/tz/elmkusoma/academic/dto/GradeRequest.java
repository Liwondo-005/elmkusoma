package tz.elmkusoma.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.academic.domain.EducationLevel;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeRequest {

    @NotNull(message = "Institution ID is required")
    private UUID institutionId;

    @NotNull(message = "Education level is required")
    private EducationLevel educationLevel;

    @NotBlank(message = "Grade name is required")
    private String name;

    private String code;

    private Integer sortOrder = 0;
}
