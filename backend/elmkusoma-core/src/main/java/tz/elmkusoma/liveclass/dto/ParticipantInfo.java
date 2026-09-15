package tz.elmkusoma.liveclass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantInfo {
    private String userId;
    private String userName;
    private String role;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Long durationSeconds;
    private boolean online;
}
