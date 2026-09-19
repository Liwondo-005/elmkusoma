package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionSummaryResponse {
    private UUID id;
    private String name;
    private String code;
    private String type;
    private String city;
    private String region;
    private Boolean isActive;
    private java.time.LocalDateTime createdAt;
}
