package tz.elmkusoma.learning.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.VideoTutorial;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoTutorialRepository extends JpaRepository<VideoTutorial, UUID> {

    List<VideoTutorial> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<VideoTutorial> findByLessonIdAndIsDeletedFalse(UUID lessonId);

    List<VideoTutorial> findByModuleIdAndIsDeletedFalse(UUID moduleId);

    List<VideoTutorial> findByCourseIdAndIsDeletedFalse(UUID courseId);

    List<VideoTutorial> findByCreatedByAndIsDeletedFalse(String createdBy);

    @Query("SELECT v FROM VideoTutorial v WHERE v.institutionId = :institutionId AND v.isDeleted = false AND v.visibility IN :visibilities")
    List<VideoTutorial> findByInstitutionIdAndVisibilities(@Param("institutionId") UUID institutionId, @Param("visibilities") List<String> visibilities);

    @Query("SELECT v FROM VideoTutorial v WHERE v.lessonId = :lessonId AND v.isDeleted = false AND v.visibility IN :visibilities ORDER BY v.sortOrder")
    List<VideoTutorial> findVisibleByLessonId(@Param("lessonId") UUID lessonId, @Param("visibilities") List<String> visibilities);

    @Query("SELECT v FROM VideoTutorial v WHERE v.moduleId = :moduleId AND v.isDeleted = false AND v.visibility IN :visibilities ORDER BY v.sortOrder")
    List<VideoTutorial> findVisibleByModuleId(@Param("moduleId") UUID moduleId, @Param("visibilities") List<String> visibilities);

    @Query("SELECT v FROM VideoTutorial v WHERE v.courseId = :courseId AND v.isDeleted = false AND v.visibility IN :visibilities ORDER BY v.sortOrder")
    List<VideoTutorial> findVisibleByCourseId(@Param("courseId") UUID courseId, @Param("visibilities") List<String> visibilities);

    Page<VideoTutorial> findByInstitutionIdAndIsDeletedFalse(UUID institutionId, org.springframework.data.domain.Pageable pageable);

    long countByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    long countByLessonIdAndIsDeletedFalse(UUID lessonId);
}