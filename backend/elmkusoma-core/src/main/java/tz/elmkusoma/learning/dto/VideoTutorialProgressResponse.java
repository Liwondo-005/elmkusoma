package tz.elmkusoma.learning.dto;

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
public class VideoTutorialProgressResponse {
    private UUID id;
    private UUID videoTutorialId;
    private UUID studentId;
    private Integer positionSeconds;
    private Boolean completed;
    private Double completionPercentage;
    private LocalDateTime lastWatchedAt;
    private LocalDateTime completedAt;
    private Integer watchCount;
    private Integer totalWatchTimeSeconds;
    private Integer lastPositionSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}