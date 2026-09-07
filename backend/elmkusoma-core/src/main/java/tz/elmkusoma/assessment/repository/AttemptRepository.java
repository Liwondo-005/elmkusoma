package tz.elmkusoma.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.assessment.domain.Attempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

    List<Attempt> findByAssessmentIdAndStudentIdAndIsDeletedFalse(UUID assessmentId, UUID studentId);

    Optional<Attempt> findByAssessmentIdAndStudentIdAndIsCompletedAndIsDeletedFalse(
            UUID assessmentId, UUID studentId, Boolean isCompleted);

    List<Attempt> findByAssessmentIdAndIsDeletedFalse(UUID assessmentId);
}
