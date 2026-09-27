package tz.elmkusoma.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTutorialAnalyticsResponse {
    private UUID videoTutorialId;
    private String title;
    private Long totalStudents;
    private Long completedStudents;
    private Double completionRate;
    private Double avgCompletionPercentage;
    private Long totalWatchTimeSeconds;
}
