package tz.elmkusoma.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.course.domain.CourseLesson;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseLessonRepository extends JpaRepository<CourseLesson, UUID> {

    List<CourseLesson> findByModuleIdAndIsDeletedFalseOrderBySortOrder(UUID moduleId);

    long countByModuleIdAndIsDeletedFalse(UUID moduleId);

    long countByIsDeletedFalse();
}
