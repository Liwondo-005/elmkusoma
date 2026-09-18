package tz.elmkusoma.nfe.program.dto;

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
public class ProgramResponse {

    private UUID id;
    private UUID institutionId;
    private UUID providerId;
    private String title;
    private String description;
    private String programType;
    private String category;
    private String targetAudience;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxParticipants;
    private Boolean isPublished;
    private LocalDateTime createdAt;
}
