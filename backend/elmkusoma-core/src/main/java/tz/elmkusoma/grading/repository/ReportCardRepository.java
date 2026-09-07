package tz.elmkusoma.grading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.grading.domain.ReportCard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportCardRepository extends JpaRepository<ReportCard, UUID> {

    List<ReportCard> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<ReportCard> findByTermIdAndIsDeletedFalse(UUID termId);

    Optional<ReportCard> findByStudentIdAndTermIdAndIsDeletedFalse(UUID studentId, UUID termId);

    List<ReportCard> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT rc FROM ReportCard rc WHERE rc.termId = :termId AND rc.isDeleted = false ORDER BY rc.classRank ASC")
    List<ReportCard> findRankedByTermId(@Param("termId") UUID termId);
}