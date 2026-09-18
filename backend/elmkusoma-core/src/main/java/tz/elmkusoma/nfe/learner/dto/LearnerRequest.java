package tz.elmkusoma.nfe.learner.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerRequest {

    @NotNull(message = "Provider ID is required")
    private UUID providerId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    private String participantNumber;

    private String occupation;

    private String organization;

    private String status;
}
