package tz.elmkusoma.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.audit.domain.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE a.institutionId = :institutionId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByInstitutionId(@Param("institutionId") UUID institutionId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.institutionId = :institutionId AND a.createdAt BETWEEN :from AND :to ORDER BY a.createdAt DESC")
    List<AuditLog> findByInstitutionIdAndDateRange(@Param("institutionId") UUID institutionId,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.institutionId = :institutionId AND a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<AuditLog> findByEntityTypeAndEntityId(@Param("institutionId") UUID institutionId,
                                                @Param("entityType") String entityType,
                                                @Param("entityId") UUID entityId);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.institutionId = :institutionId")
    long countByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.institutionId = :institutionId GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByActionForInstitution(@Param("institutionId") UUID institutionId);
}
