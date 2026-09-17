package tz.elmkusoma.parent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentCalendarResponse {

    private List<CalendarEvent> events;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalendarEvent {
        private String id;
        private String type;
        private String title;
        private String description;
        private LocalDateTime start;
        private LocalDateTime end;
        private String status;
        private String relatedEntityType;
        private String relatedEntityId;
        private boolean isActionRequired;
    }
}
