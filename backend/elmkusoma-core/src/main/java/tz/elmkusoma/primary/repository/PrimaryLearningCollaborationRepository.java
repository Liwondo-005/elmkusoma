package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.PrimaryLearningCollaboration;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrimaryLearningCollaborationRepository extends JpaRepository<PrimaryLearningCollaboration, UUID> {

    List<PrimaryLearningCollaboration> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID studentId, UUID institutionId);
}
