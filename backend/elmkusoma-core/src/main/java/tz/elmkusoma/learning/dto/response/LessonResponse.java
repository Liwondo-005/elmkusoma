package tz.elmkusoma.learning.dto.response;

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
public class LessonResponse {

    private UUID id;
    private UUID subjectId;
    private UUID classGroupId;
    private String title;
    private String description;
    private String contentText;
    private String videoUrl;
    private String fileAttachments;
    private Integer sortOrder;
    private Boolean isPublished;
    private LocalDateTime createdAt;
}
