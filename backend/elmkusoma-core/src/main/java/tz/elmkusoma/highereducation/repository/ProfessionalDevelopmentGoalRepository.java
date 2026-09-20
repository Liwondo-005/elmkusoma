package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.highereducation.domain.ProfessionalDevelopmentGoal;

import java.util.List;
import tz.elmkusoma.highereducation.domain.DevGoalStatus;
import java.util.UUID;

public interface ProfessionalDevelopmentGoalRepository extends JpaRepository<ProfessionalDevelopmentGoal, UUID> {
    List<ProfessionalDevelopmentGoal> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<ProfessionalDevelopmentGoal> findByStudentIdAndStatus(UUID studentId, DevGoalStatus status);
}
