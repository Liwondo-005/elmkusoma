package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.LearningGoal;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningGoalRepository extends JpaRepository<LearningGoal, UUID> {
    List<LearningGoal> findByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID studentId);
    List<LearningGoal> findByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, String status);
    long countByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, String status);
}
