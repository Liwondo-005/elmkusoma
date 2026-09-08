package tz.elmkusoma.enrollment.dto.response;

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
public class TransferResponse {

    private UUID id;
    private UUID enrollmentId;
    private UUID fromClassGroupId;
    private UUID toClassGroupId;
    private String reason;
    private LocalDateTime transferredAt;
    private UUID transferredBy;
    private LocalDateTime createdAt;
}
