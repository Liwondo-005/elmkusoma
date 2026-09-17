package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentLiveClassResponse {

    private String studentName;
    private String className;
    private List<LiveClassItem> liveClasses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveClassItem {
        private String id;
        private String title;
        private String description;
        private String status;
        private LocalDateTime scheduledAt;
        private Integer durationMinutes;
        private String teacherName;
    }
}
