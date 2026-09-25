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
public class InstitutionServiceResponse {
    private Long id;
    private UUID institutionId;
    private String featureKey;
    private String featureName;
    private String status;
    private Boolean enabled;
    private String configuration;
    private LocalDateTime enabledAt;
    private String enabledBy;
    private String description;
}