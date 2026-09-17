package tz.elmkusoma.oversight.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveClassesResponse {
    private Long liveNow;
    private Long scheduledToday;
    private Long completedToday;
    private Long totalThisWeek;
    private List<LiveClassSummary> liveClasses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LiveClassSummary {
        private String id;
        private String title;
        private String institutionName;
        private String institutionId;
        private String subjectName;
        private String teacherName;
        private String scheduledAt;
        private Integer durationMinutes;
        private String status;
        private Long participantCount;
        private Long maxParticipants;
    }
}
