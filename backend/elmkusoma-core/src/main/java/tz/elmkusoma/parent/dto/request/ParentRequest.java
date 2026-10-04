package tz.elmkusoma.parent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ParentRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @Size(max = 255, message = "Occupation must not exceed 255 characters")
    private String occupation;

    private String relationshipType;

    @Size(max = 255, message = "Emergency contact must not exceed 255 characters")
    private String emergencyContact;
}
