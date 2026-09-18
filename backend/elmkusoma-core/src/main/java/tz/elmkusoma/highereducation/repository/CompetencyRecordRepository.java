package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.CompetencyRecord;
import tz.elmkusoma.highereducation.domain.CompetencyStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompetencyRecordRepository extends JpaRepository<CompetencyRecord, UUID> {

    List<CompetencyRecord> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<CompetencyRecord> findByCompetencyIdAndIsDeletedFalse(UUID competencyId);

    Optional<CompetencyRecord> findByStudentIdAndCompetencyIdAndIsDeletedFalse(UUID studentId, UUID competencyId);

    long countByCompetencyIdAndStatusAndIsDeletedFalse(UUID competencyId, CompetencyStatus status);

    List<CompetencyRecord> findByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, CompetencyStatus status);

    @Query("SELECT cr.status, COUNT(cr) FROM CompetencyRecord cr WHERE cr.studentId = :studentId AND cr.isDeleted = false GROUP BY cr.status")
    List<Object[]> countByStatusForStudent(@Param("studentId") UUID studentId);

    List<CompetencyRecord> findByStudentIdAndCompetencyIdInAndIsDeletedFalse(UUID studentId, List<UUID> competencyIds);

    boolean existsByStudentIdAndCompetencyIdAndIsDeletedFalse(UUID studentId, UUID competencyId);
}
