package tz.elmkusoma.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.course.domain.CourseLesson;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseLessonRepository extends JpaRepository<CourseLesson, UUID> {

    List<CourseLesson> findByModuleIdAndIsDeletedFalseOrderBySortOrder(UUID moduleId);

    long countByModuleIdAndIsDeletedFalse(UUID moduleId);

    long countByIsDeletedFalse();

    @Query("SELECT COUNT(cl) FROM CourseLesson cl WHERE cl.isDeleted = false AND cl.moduleId IN (SELECT m.id FROM CourseModule m WHERE m.courseId = :courseId AND m.isDeleted = false)")
    long countByCourseIdAndIsDeletedFalse(@Param("courseId") UUID courseId);
}
