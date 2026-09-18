package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.MistakeLabEntry;

import java.util.List;
import java.util.UUID;

@Repository
public interface MistakeLabEntryRepository extends JpaRepository<MistakeLabEntry, UUID> {

    List<MistakeLabEntry> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID studentId, UUID institutionId);
}
