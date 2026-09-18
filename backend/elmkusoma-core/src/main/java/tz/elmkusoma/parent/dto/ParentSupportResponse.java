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
public class ParentSupportResponse {

    private List<SupportTicketItem> tickets;
    private long openCount;
    private long resolvedCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupportTicketItem {
        private String id;
        private String subject;
        private String description;
        private String category;
        private String priority;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime resolvedAt;
        private int messageCount;
    }
}
