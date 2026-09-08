package tz.elmkusoma.grading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.grading.domain.RubricCriteria;

import java.util.List;
import java.util.UUID;

@Repository
public interface RubricCriteriaRepository extends JpaRepository<RubricCriteria, UUID> {

    List<RubricCriteria> findByRubricIdAndIsDeletedFalse(UUID rubricId);
}