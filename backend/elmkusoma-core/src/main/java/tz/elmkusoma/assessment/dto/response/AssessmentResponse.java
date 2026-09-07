package tz.elmkusoma.assessment.dto.response;

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
    private UUID subjectId;
    private UUID classGroupId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer totalMarks;
    private Integer passMarks;
    private Boolean isPublished;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
}
