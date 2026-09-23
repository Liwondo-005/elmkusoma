package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionStatusResponse {
    private Map<String, String> config;
    private String lastSweep;
    private Long archivedAuditCount;
    private Long totalAuditCount;
    private Long purgedSoftDeletedReports;
}
