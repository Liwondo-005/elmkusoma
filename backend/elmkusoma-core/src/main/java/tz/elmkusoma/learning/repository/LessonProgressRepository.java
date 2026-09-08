package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.LessonProgress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    Optional<LessonProgress> findByLessonIdAndStudentIdAndIsDeletedFalse(UUID lessonId, UUID studentId);

    List<LessonProgress> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<LessonProgress> findByLessonIdAndIsDeletedFalse(UUID lessonId);

    @Query("SELECT lp FROM LessonProgress lp WHERE lp.studentId = :studentId " +
           "AND lp.lessonId IN (SELECT l.id FROM Lesson l WHERE l.classGroupId = :classGroupId) " +
           "AND lp.isDeleted = false")
    List<LessonProgress> findByStudentAndClassGroup(@Param("studentId") UUID studentId,
                                                     @Param("classGroupId") UUID classGroupId);

    @Query("SELECT COALESCE(AVG(lp.completionPercentage), 0) FROM LessonProgress lp " +
           "WHERE lp.studentId = :studentId AND lp.isDeleted = false")
    Double getAverageCompletionByStudent(@Param("studentId") UUID studentId);
}
