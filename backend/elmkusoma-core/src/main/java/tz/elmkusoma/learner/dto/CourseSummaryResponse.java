package tz.elmkusoma.learner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSummaryResponse {

    private UUID id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String level;
    private String category;
    private Boolean isPublished;
    private Boolean isFeatured;
    private long moduleCount;
    private long lessonCount;
}
