package tz.elmkusoma.administration.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class PlatformCourseResponse {
    private UUID id;
    private UUID institutionId;
    private String title;
    private String level;
    private String category;
    private Boolean isPublished;
    private Boolean isFeatured;
    private LocalDateTime createdAt;
}
