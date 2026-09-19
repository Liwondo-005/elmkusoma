package tz.elmkusoma.administration.dto;

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
public class LiveClassSummaryResponse {
    private UUID id;
    private String title;
    private String status;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private LocalDateTime createdAt;
}
