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
public class ComplianceCheck {
    private String id;
    private String name;
    private String category;
    private Boolean passed;
    private String description;
    private String remediation;
    private LocalDateTime lastChecked;
}
