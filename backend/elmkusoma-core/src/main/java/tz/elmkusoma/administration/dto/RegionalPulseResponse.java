package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionalPulseResponse {
    private long liveNow;
    private long scheduledToday;
    private long completedToday;
    private long totalThisWeek;
    private long teachers;
    private long learners;
    private long totalLessons;
    private long publishedLessons;
    private long pendingVerifications;
    private long alertsCount;
    private LocalDateTime lastUpdated;
}
