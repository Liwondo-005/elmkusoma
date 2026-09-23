package tz.elmkusoma.administration.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class PlatformMediaResponse {
    private UUID id;
    private UUID institutionId;
    private String title;
    private String mediaType;
    private String status;
    private LocalDateTime createdAt;
}
