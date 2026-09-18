package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.CompetencyAssessment;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompetencyAssessmentRepository extends JpaRepository<CompetencyAssessment, UUID> {

    List<CompetencyAssessment> findByCompetencyIdAndIsDeletedFalse(UUID competencyId);

    List<CompetencyAssessment> findByAssessmentIdAndIsDeletedFalse(UUID assessmentId);

    boolean existsByCompetencyIdAndAssessmentIdAndIsDeletedFalse(UUID competencyId, UUID assessmentId);
}
