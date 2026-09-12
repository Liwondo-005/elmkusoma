package tz.elmkusoma.course.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateLiveClassRequest {
    private String title;
    private String description;
    private String scheduledAt;
    private Integer durationMinutes;
    private String meetingUrl;
    private UUID subjectId;
    private Integer maxParticipants;
}
