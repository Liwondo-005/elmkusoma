package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformHealthResponse {
    private String databaseStatus;
    private String apiStatus;
    private long totalUsers;
    private long activeUsers;
    private long totalInstitutions;
    private long activeInstitutions;
    private String livekitStatus;
    private String storageStatus;
    private String backgroundJobsStatus;
    private String notificationsStatus;
    private String paymentsStatus;
    private String realtimeStatus;
    private String mediaStatus;
    private String heartbeatAt;
}
