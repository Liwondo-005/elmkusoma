package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.highereducation.domain.LearningCollaboration;

import java.util.List;
import java.util.UUID;

public interface LearningCollaborationRepository extends JpaRepository<LearningCollaboration, UUID> {
    List<LearningCollaboration> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<LearningCollaboration> findByStudentIdAndStatus(UUID studentId, String status);
    List<LearningCollaboration> findByPeerStudentId(UUID peerStudentId);
}
