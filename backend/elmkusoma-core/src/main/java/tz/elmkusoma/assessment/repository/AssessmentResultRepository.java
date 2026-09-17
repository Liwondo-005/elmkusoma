package tz.elmkusoma.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT ar FROM AssessmentResult ar WHERE ar.studentId = :studentId AND ar.isDeleted = false ORDER BY ar.createdAt DESC")
    List<AssessmentResult> findByStudentIdAndIsDeletedFalse(@Param("studentId") UUID studentId);
}
