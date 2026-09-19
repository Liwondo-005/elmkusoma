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
    private Integer maxParticipants;
    private String subjectName;
    private String teacherName;
    private UUID teacherId;
    private UUID subjectId;
    private UUID classGroupId;
    private String recordingUrl;
    private Integer currentParticipants;
    private Boolean canJoin;
    private String createdAt;
    private Boolean recordingEnabled;
    private String sessionType;
    private String timezone;
    private Boolean isRecurring;
    private String recurrencePattern;
    private String recurrenceEndDate;
    private Boolean lobbyEnabled;
}
