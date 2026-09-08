package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.Lesson;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findBySubjectIdAndClassGroupIdAndIsDeletedFalseOrderBySortOrder(
            UUID subjectId, UUID classGroupId);

    List<Lesson> findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(UUID classGroupId);

    List<Lesson> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}
