package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.InstitutionAuditLog;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstitutionAuditLogRepository extends JpaRepository<InstitutionAuditLog, UUID> {

    @Query("SELECT l FROM InstitutionAuditLog l WHERE l.institutionId = :institutionId ORDER BY l.createdAt DESC")
    List<InstitutionAuditLog> findRecentByInstitutionId(@Param("institutionId") UUID institutionId,
                                                         org.springframework.data.domain.Pageable pageable);

    long countByInstitutionIdAndAction(UUID institutionId, String action);
}
