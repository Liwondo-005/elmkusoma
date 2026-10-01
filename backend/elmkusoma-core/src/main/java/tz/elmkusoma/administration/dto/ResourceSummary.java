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
public class ResourceSummary {
    private UUID id;
    private String title;
    private String type;
    private String mimeType;
    private Long fileSize;
    private String visibility;
    private UUID institutionId;
    private String institutionName;
    private LocalDateTime createdAt;
}
