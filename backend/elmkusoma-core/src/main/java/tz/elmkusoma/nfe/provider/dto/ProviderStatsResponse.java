package tz.elmkusoma.nfe.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderStatsResponse {
    private long totalPrograms;
    private long activePrograms;
    private long totalLearners;
    private long activeLearners;
    private long totalSessions;
    private long completedSessions;
    private long totalCertificates;
    private long totalProviders;
    private long activeProviders;
}
