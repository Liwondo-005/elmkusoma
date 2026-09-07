package tz.elmkusoma.grading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.grading.domain.GradingRubric;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradingRubricRepository extends JpaRepository<GradingRubric, UUID> {

    List<GradingRubric> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<GradingRubric> findBySubjectIdAndIsDeletedFalse(UUID subjectId);
}