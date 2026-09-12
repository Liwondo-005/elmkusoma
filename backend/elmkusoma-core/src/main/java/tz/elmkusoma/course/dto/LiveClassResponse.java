package tz.elmkusoma.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveClassResponse {
    private UUID id;
    private String title;
    private String description;
    private String scheduledAt;
    private Integer durationMinutes;
    private String status;
    private String meetingUrl;
    private Integer maxParticipants;
    private String subjectName;
    private String teacherName;
    private UUID teacherId;
}
