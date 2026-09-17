package tz.elmkusoma.parent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentActivityResponse {

    private List<ActivityItem> activities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityItem {
        private String id;
        private String type;
        private String title;
        private String description;
        private LocalDateTime timestamp;
        private String relatedEntityType;
        private String relatedEntityId;
        private String status;
    }
}
