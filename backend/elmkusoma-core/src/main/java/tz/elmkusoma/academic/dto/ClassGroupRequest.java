package tz.elmkusoma.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassGroupRequest {

    @NotNull(message = "Institution ID is required")
    private UUID institutionId;

    @NotNull(message = "Grade ID is required")
    private UUID gradeId;

    @NotNull(message = "Academic year ID is required")
    private UUID academicYearId;

    @NotNull(message = "Term ID is required")
    private UUID termId;

    @NotBlank(message = "Class name is required")
    private String name;

    private String section;

    private Integer capacity;

    private UUID classTeacherId;
}
