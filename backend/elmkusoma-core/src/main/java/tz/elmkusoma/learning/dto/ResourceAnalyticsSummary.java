package tz.elmkusoma.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceAnalyticsSummary {
    private Long totalViews;
    private Long totalDownloads;
    private Long uniqueViewers;
    private Long uniqueDownloaders;
    private Long totalWatchTimeSeconds;
    private Double avgWatchTimeSeconds;
}