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
public class ParentAnnouncementResponse {

    private List<AnnouncementItem> announcements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnouncementItem {
        private String id;
        private String title;
        private String content;
        private String priority;
        private String authorName;
        private String className;
        private String subjectName;
        private LocalDateTime createdAt;
    }
}
