package tz.elmkusoma.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseLessonResponse {

    private UUID id;
    private UUID moduleId;
    private String title;
    private String contentType;
    private String contentUrl;
    private Integer durationMinutes;
    private Integer sortOrder;
    private Boolean isFree;
    private LocalDateTime createdAt;
}
