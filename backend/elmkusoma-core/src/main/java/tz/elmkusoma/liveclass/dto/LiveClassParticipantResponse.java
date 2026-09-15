package tz.elmkusoma.liveclass.dto;

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
public class LiveClassParticipantResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String role;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Long durationSeconds;
    private int participantCount;
}
