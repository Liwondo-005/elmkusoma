package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.ReadingAdventure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReadingAdventureRepository extends JpaRepository<ReadingAdventure, UUID> {

    List<ReadingAdventure> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID studentId, UUID institutionId);

    Optional<ReadingAdventure> findByIdAndStudentIdAndIsDeletedFalse(UUID id, UUID studentId);
}
