package tz.elmkusoma.nfe.session.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.common.TenantRepository;
import tz.elmkusoma.nfe.session.domain.NfeSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NfeSessionRepository extends TenantRepository<NfeSession, UUID> {

    @Query("SELECT s FROM NfeSession s WHERE s.institutionId = :institutionId AND s.isDeleted = false")
    List<NfeSession> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT s FROM NfeSession s WHERE s.id = :id AND s.institutionId = :institutionId AND s.isDeleted = false")
    Optional<NfeSession> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);

    @Query("SELECT s FROM NfeSession s WHERE s.providerId = :providerId AND s.isDeleted = false")
    List<NfeSession> findByProviderId(@Param("providerId") UUID providerId);

    @Query("SELECT s FROM NfeSession s WHERE s.programId = :programId AND s.isDeleted = false")
    List<NfeSession> findByProgramId(@Param("programId") UUID programId);

    @Query("SELECT s FROM NfeSession s WHERE s.status = :status AND s.institutionId = :institutionId AND s.isDeleted = false")
    List<NfeSession> findByStatusAndInstitutionId(@Param("status") String status, @Param("institutionId") UUID institutionId);
}
