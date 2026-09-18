package tz.elmkusoma.nfe.session.dto;

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
public class SessionResponse {

    private UUID id;
    private UUID institutionId;
    private UUID providerId;
    private UUID programId;
    private String title;
    private String description;
    private String sessionType;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String meetingUrl;
    private Integer maxParticipants;
    private String status;
    private LocalDateTime createdAt;
}
