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
public class ComplianceResponse {
    private Boolean isCompliant;
    private long totalChecks;
    private long passedChecks;
    private long failedChecks;
    private List<ComplianceCheck> checks;
    private String note;
}
