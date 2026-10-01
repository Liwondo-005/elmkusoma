package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveClassSummary {
    private UUID id;
    private String title;
    private String status;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private Integer maxParticipants;
    private UUID subjectId;
    private String subjectName;
    private UUID teacherId;
    private String teacherName;
    private UUID institutionId;
    private String institutionName;
}
