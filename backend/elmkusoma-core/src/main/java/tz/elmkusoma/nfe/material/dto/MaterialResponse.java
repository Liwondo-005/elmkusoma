package tz.elmkusoma.nfe.material.dto;

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
public class MaterialResponse {

    private UUID id;
    private UUID institutionId;
    private UUID providerId;
    private UUID programId;
    private String title;
    private String description;
    private String materialType;
    private String contentUrl;
    private Integer sortOrder;
    private Boolean isFree;
    private LocalDateTime createdAt;
}
