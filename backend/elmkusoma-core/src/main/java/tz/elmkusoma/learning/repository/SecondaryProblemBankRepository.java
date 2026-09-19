package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.SecondaryProblemBank;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecondaryProblemBankRepository extends JpaRepository<SecondaryProblemBank, UUID> {
    List<SecondaryProblemBank> findBySubjectIdAndIsDeletedFalse(UUID subjectId);
    List<SecondaryProblemBank> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);
    List<SecondaryProblemBank> findBySubjectIdAndDifficultyLevelAndIsDeletedFalse(UUID subjectId, SecondaryProblemBank.DifficultyLevel level);
}
