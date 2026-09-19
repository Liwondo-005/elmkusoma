package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.SecondaryConceptBank;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecondaryConceptBankRepository extends JpaRepository<SecondaryConceptBank, UUID> {
    List<SecondaryConceptBank> findBySubjectIdAndIsDeletedFalse(UUID subjectId);
    List<SecondaryConceptBank> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);
    List<SecondaryConceptBank> findBySubjectIdAndDifficultyLevelAndIsDeletedFalse(UUID subjectId, SecondaryConceptBank.DifficultyLevel level);
}
