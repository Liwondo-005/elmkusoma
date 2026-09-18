package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.LogbookEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LogbookEntryRepository extends JpaRepository<LogbookEntry, UUID> {

    List<LogbookEntry> findByPlacementIdAndIsDeletedFalse(UUID placementId);

    Optional<LogbookEntry> findByPlacementIdAndEntryDateAndIsDeletedFalse(UUID placementId, LocalDate entryDate);

    List<LogbookEntry> findByPlacementIdAndIsApprovedFalseAndIsDeletedFalse(UUID placementId);
}
