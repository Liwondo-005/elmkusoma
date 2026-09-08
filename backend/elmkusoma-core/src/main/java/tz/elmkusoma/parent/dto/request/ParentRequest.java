package tz.elmkusoma.parent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParentRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    private String occupation;

    private String relationshipType;

    private String emergencyContact;
}
