package tz.elmkusoma.teacher.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeacherAssignmentRequest {

    @NotBlank(message = "Class group ID is required")
    private String classGroupId;

    @NotBlank(message = "Subject ID is required")
    private String subjectId;

    private String academicYear;
}
