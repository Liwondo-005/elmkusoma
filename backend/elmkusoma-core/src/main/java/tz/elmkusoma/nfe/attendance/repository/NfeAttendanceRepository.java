package tz.elmkusoma.nfe.attendance.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.common.TenantRepository;
import tz.elmkusoma.nfe.attendance.domain.NfeAttendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NfeAttendanceRepository extends TenantRepository<NfeAttendance, UUID> {

    @Query("SELECT a FROM NfeAttendance a WHERE a.sessionId = :sessionId AND a.institutionId = :institutionId AND a.isDeleted = false")
    List<NfeAttendance> findBySessionId(@Param("sessionId") UUID sessionId, @Param("institutionId") UUID institutionId);

    @Query("SELECT a FROM NfeAttendance a WHERE a.learnerId = :learnerId AND a.institutionId = :institutionId AND a.isDeleted = false")
    List<NfeAttendance> findByLearnerId(@Param("learnerId") UUID learnerId, @Param("institutionId") UUID institutionId);

    @Query("SELECT a FROM NfeAttendance a WHERE a.providerId = :providerId AND a.institutionId = :institutionId AND a.isDeleted = false")
    List<NfeAttendance> findByProviderId(@Param("providerId") UUID providerId, @Param("institutionId") UUID institutionId);

    @Query("SELECT a FROM NfeAttendance a WHERE a.id = :id AND a.institutionId = :institutionId AND a.isDeleted = false")
    Optional<NfeAttendance> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);
}
