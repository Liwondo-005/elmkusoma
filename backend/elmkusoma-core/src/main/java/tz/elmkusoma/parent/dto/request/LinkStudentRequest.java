package tz.elmkusoma.parent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkStudentRequest {

    @NotBlank(message = "Student ID is required")
    private String studentId;

    private String relationshipType;

    private Boolean isPrimary;
}
