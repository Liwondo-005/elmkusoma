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
public class CourseSummary {
    private UUID id;
    private String title;
    private String level;
    private String category;
    private String subject;
    private UUID institutionId;
    private String institutionName;
    private Boolean isPublished;
    private LocalDateTime createdAt;
}
