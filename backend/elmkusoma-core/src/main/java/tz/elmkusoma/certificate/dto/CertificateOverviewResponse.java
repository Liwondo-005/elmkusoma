package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Platform certificate overview — real counts only (no invented trends or percentages).
 * verificationActivity30Days counts audit rows of type Certificate/VIEW written by the
 * public verification endpoint during the last 30 days.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateOverviewResponse {
    private long total;
    private long issued;
    private long revoked;
    private long draft;
    private Map<String, Long> byType;
    private long verificationActivity30Days;
    private LocalDateTime generatedAt;
}
