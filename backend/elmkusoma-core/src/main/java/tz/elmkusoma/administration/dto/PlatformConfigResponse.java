package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PlatformConfigResponse {
    private UUID id;
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private String category;
    private Boolean isSensitive;
    private Boolean isPublic;
    private String lastModifiedBy;
    private LocalDateTime updatedAt;
}
