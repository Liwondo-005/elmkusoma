package tz.elmkusoma.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.audit.domain.SecurityEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    @Query("SELECT s FROM SecurityEvent s WHERE s.institutionId = :institutionId ORDER BY s.createdAt DESC")
    Page<SecurityEvent> findByInstitutionId(@Param("institutionId") UUID institutionId, Pageable pageable);

    @Query("SELECT s FROM SecurityEvent s WHERE s.institutionId = :institutionId AND s.resolved = false ORDER BY s.createdAt DESC")
    List<SecurityEvent> findUnresolvedByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT s FROM SecurityEvent s WHERE s.institutionId = :institutionId AND s.severity = :severity ORDER BY s.createdAt DESC")
    List<SecurityEvent> findByInstitutionIdAndSeverity(@Param("institutionId") UUID institutionId,
                                                        @Param("severity") SecurityEvent.Severity severity);

    @Query("SELECT COUNT(s) FROM SecurityEvent s WHERE s.institutionId = :institutionId AND s.resolved = false")
    long countUnresolvedByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT COUNT(s) FROM SecurityEvent s WHERE s.institutionId = :institutionId AND s.severity = 'CRITICAL'")
    long countCriticalByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT COUNT(s) FROM SecurityEvent s WHERE s.institutionId = :institutionId AND s.eventType = 'LOGIN_FAILURE'")
    long countFailedLoginsByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT s.eventType, COUNT(s) FROM SecurityEvent s WHERE s.institutionId = :institutionId GROUP BY s.eventType ORDER BY COUNT(s) DESC")
    List<Object[]> countByEventTypeForInstitution(@Param("institutionId") UUID institutionId);

    @Query("SELECT s.severity, COUNT(s) FROM SecurityEvent s WHERE s.institutionId = :institutionId GROUP BY s.severity ORDER BY COUNT(s) DESC")
    List<Object[]> countBySeverityForInstitution(@Param("institutionId") UUID institutionId);
}
