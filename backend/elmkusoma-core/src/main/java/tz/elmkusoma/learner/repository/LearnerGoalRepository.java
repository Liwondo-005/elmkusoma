package tz.elmkusoma.learner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learner.domain.LearningGoal;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearnerGoalRepository extends JpaRepository<LearningGoal, UUID> {

    List<LearningGoal> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    List<LearningGoal> findByUserIdAndStatusAndIsDeletedFalse(UUID userId, LearningGoal.GoalStatus status);

    long countByUserIdAndIsDeletedFalse(UUID userId);

    long countByUserIdAndStatusAndIsDeletedFalse(UUID userId, LearningGoal.GoalStatus status);
}