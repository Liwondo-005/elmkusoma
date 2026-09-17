package tz.elmkusoma.nfe.assessment.dto;

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
public class AssessmentResponse {

    private UUID id;
    private UUID institutionId;
    private UUID providerId;
    private UUID programId;
    private String title;
    private String description;
    private String assessmentType;
    private Integer totalMarks;
    private Integer passMarks;
    private Integer timeLimitMinutes;
    private Boolean isPublished;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
}
