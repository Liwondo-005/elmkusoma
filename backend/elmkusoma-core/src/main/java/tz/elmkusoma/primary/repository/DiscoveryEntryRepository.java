package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.DiscoveryEntry;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscoveryEntryRepository extends JpaRepository<DiscoveryEntry, UUID> {

    List<DiscoveryEntry> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID studentId, UUID institutionId);
}
