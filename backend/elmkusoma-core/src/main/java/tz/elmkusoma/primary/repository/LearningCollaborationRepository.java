package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.LearningCollaboration;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningCollaborationRepository extends JpaRepository<LearningCollaboration, UUID> {

    List<LearningCollaboration> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID studentId, UUID institutionId);
}
