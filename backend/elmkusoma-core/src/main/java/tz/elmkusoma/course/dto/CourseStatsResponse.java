package tz.elmkusoma.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseStatsResponse {

    private Long totalCourses;
    private Long publishedCourses;
    private Long draftCourses;
    private Long featuredCourses;
    private Long totalModules;
    private Long totalLessons;
    private Long liveClassesScheduled;
    private Long liveClassesCompleted;
    private Map<String, Long> coursesByLevel;
}
