package tz.elmkusoma.course.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateAnnouncementRequest {
    private String title;
    private String content;
    private UUID classGroupId;
    private UUID subjectId;
    private String priority;
}
