package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
}
