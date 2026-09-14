package tz.elmkusoma.course.dto;

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
public class AnnouncementResponse {
    private UUID id;
    private String title;
    private String content;
    private UUID classGroupId;
    private String className;
    private UUID subjectId;
    private String subjectName;
    private String priority;
    private String authorName;
    private UUID authorId;
    private LocalDateTime createdAt;
}
