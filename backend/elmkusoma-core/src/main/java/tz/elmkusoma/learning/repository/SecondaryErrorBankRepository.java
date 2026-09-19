package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.SecondaryErrorBank;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecondaryErrorBankRepository extends JpaRepository<SecondaryErrorBank, UUID> {
    List<SecondaryErrorBank> findBySubjectIdAndIsDeletedFalse(UUID subjectId);
    List<SecondaryErrorBank> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);
    List<SecondaryErrorBank> findBySubjectIdAndCategoryAndIsDeletedFalse(UUID subjectId, String category);
}
