package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.LearningEvidence;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningEvidenceRepository extends JpaRepository<LearningEvidence, UUID> {

    List<LearningEvidence> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID studentId, UUID institutionId);
}
