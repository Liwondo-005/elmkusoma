package tz.elmkusoma.course.mapper;

import org.springframework.stereotype.Component;
import tz.elmkusoma.course.domain.Course;
import tz.elmkusoma.course.domain.CourseLesson;
import tz.elmkusoma.course.domain.CourseModule;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.dto.*;

@Component
public class CourseMapper {

    public CourseResponse toCourseResponse(Course course, String subjectName, Long moduleCount, Long lessonCount, String createdByName) {
        return CourseResponse.builder()
                .id(course.getId())
                .institutionId(course.getInstitutionId())
                .subjectId(course.getSubjectId())
                .subjectName(subjectName)
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .level(course.getLevel())
                .category(course.getCategory())
                .isPublished(course.getIsPublished())
                .isFeatured(course.getIsFeatured())
                .createdByName(createdByName)
                .moduleCount(moduleCount)
                .lessonCount(lessonCount)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    public CourseModuleResponse toModuleResponse(CourseModule module, Long lessonCount) {
        return CourseModuleResponse.builder()
                .id(module.getId())
                .courseId(module.getCourseId())
                .title(module.getTitle())
                .description(module.getDescription())
                .sortOrder(module.getSortOrder())
                .lessonCount(lessonCount)
                .createdAt(module.getCreatedAt())
                .build();
    }

    public CourseLessonResponse toLessonResponse(CourseLesson lesson) {
        return CourseLessonResponse.builder()
                .id(lesson.getId())
                .moduleId(lesson.getModuleId())
                .title(lesson.getTitle())
                .contentType(lesson.getContentType())
                .contentUrl(lesson.getContentUrl())
                .durationMinutes(lesson.getDurationMinutes())
                .sortOrder(lesson.getSortOrder())
                .isFree(lesson.getIsFree())
                .createdAt(lesson.getCreatedAt())
                .build();
    }
}
