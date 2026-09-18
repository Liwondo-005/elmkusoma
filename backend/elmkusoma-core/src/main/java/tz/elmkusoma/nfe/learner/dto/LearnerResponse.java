package tz.elmkusoma.nfe.learner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerResponse {

    private UUID id;
    private UUID institutionId;
    private UUID providerId;
    private UUID userId;
    private String participantNumber;
    private String occupation;
    private String organization;
    private String status;
    private LocalDateTime createdAt;
}
