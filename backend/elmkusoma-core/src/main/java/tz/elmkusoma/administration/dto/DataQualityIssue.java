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
public class DataQualityIssue {
    private String id;
    private String type;
    private String severity;
    private String title;
    private String description;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private String suggestedAction;
    private LocalDateTime detectedAt;
}
