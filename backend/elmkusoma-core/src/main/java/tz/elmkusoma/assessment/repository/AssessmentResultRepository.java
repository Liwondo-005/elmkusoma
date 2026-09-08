package tz.elmkusoma.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.assessment.domain.AssessmentResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, UUID> {

    Optional<AssessmentResult> findByAssessmentIdAndStudentIdAndIsDeletedFalse(
            UUID assessmentId, UUID studentId);

    List<AssessmentResult> findByAssessmentIdAndIsDeletedFalse(UUID assessmentId);
}
