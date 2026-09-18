package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.Achievement;

import java.util.List;
import java.util.UUID;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    List<Achievement> findByStudentIdAndIsDeletedFalseOrderByAchievedAtDesc(UUID studentId);
    List<Achievement> findByStudentIdAndAchievementTypeAndIsDeletedFalse(UUID studentId, String achievementType);
    long countByStudentIdAndIsDeletedFalse(UUID studentId);
    boolean existsByStudentIdAndRelatedEntityTypeAndRelatedEntityIdAndIsDeletedFalse(UUID studentId, String relatedEntityType, UUID relatedEntityId);
}
