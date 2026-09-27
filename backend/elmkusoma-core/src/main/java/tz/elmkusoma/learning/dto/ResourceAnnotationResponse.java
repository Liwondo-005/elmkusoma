package tz.elmkusoma.learning.dto;

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
public class ResourceAnnotationResponse {
    private UUID id;
    private UUID resourceId;
    private UUID studentId;
    private String studentName;
    private String content;
    private String positionData;
    private Boolean isPrivate;
    private UUID parentAnnotationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
