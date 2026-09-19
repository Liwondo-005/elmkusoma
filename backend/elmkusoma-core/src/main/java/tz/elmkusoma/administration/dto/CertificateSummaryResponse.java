package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateSummaryResponse {
    private UUID id;
    private UUID studentId;
    private String serialNumber;
    private String title;
    private LocalDateTime issueDate;
    private String status;
}
