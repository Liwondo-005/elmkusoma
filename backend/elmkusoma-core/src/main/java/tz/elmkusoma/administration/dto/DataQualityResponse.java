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
public class DataQualityResponse {
    private long totalIssues;
    private long criticalIssues;
    private long warningIssues;
    private long infoIssues;
    private List<DataQualityIssue> issues;
}
