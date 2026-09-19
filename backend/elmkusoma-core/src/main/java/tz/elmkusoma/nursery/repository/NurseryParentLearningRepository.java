package tz.elmkusoma.nursery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.nursery.domain.NurseryParentLearning;

import java.util.List;
import java.util.UUID;

@Repository
public interface NurseryParentLearningRepository extends JpaRepository<NurseryParentLearning, UUID> {
    List<NurseryParentLearning> findByStudentIdAndIsDeletedFalse(UUID studentId);
    List<NurseryParentLearning> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);
    List<NurseryParentLearning> findByStudentIdAndCompletionStatusAndIsDeletedFalse(UUID studentId, NurseryParentLearning.CompletionStatus status);
}
