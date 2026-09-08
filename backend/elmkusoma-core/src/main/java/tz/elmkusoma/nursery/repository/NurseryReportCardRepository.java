package tz.elmkusoma.nursery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.nursery.domain.NurseryReportCard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NurseryReportCardRepository extends JpaRepository<NurseryReportCard, UUID> {

    List<NurseryReportCard> findByStudentIdAndIsDeletedFalse(UUID studentId);

    Optional<NurseryReportCard> findByStudentIdAndTermIdAndIsDeletedFalse(UUID studentId, UUID termId);

    List<NurseryReportCard> findByTermIdAndIsDeletedFalse(UUID termId);

    List<NurseryReportCard> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}