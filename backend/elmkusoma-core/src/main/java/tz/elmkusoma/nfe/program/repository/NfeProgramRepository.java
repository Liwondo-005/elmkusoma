package tz.elmkusoma.nfe.program.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.common.TenantRepository;
import tz.elmkusoma.nfe.program.domain.NfeProgram;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NfeProgramRepository extends TenantRepository<NfeProgram, UUID> {

    @Query("SELECT p FROM NfeProgram p WHERE p.institutionId = :institutionId AND p.isDeleted = false")
    List<NfeProgram> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT p FROM NfeProgram p WHERE p.id = :id AND p.institutionId = :institutionId AND p.isDeleted = false")
    Optional<NfeProgram> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);

    @Query("SELECT p FROM NfeProgram p WHERE p.providerId = :providerId AND p.isDeleted = false")
    List<NfeProgram> findByProviderId(@Param("providerId") UUID providerId);

    @Query("SELECT p FROM NfeProgram p WHERE p.institutionId = :institutionId AND p.isPublished = true AND p.isDeleted = false")
    List<NfeProgram> findPublishedByInstitutionId(@Param("institutionId") UUID institutionId);
}
