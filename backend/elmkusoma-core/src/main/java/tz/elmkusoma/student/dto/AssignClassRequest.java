package tz.elmkusoma.student.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignClassRequest {

    @NotNull(message = "Class group ID is required")
    private UUID classGroupId;

    @NotNull(message = "Academic year ID is required")
    private UUID academicYearId;

    @NotNull(message = "Term ID is required")
    private UUID termId;
}
