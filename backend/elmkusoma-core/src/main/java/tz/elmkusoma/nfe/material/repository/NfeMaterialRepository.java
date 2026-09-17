package tz.elmkusoma.nfe.material.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.common.TenantRepository;
import tz.elmkusoma.nfe.material.domain.NfeMaterial;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NfeMaterialRepository extends TenantRepository<NfeMaterial, UUID> {

    @Query("SELECT m FROM NfeMaterial m WHERE m.institutionId = :institutionId AND m.isDeleted = false")
    List<NfeMaterial> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT m FROM NfeMaterial m WHERE m.id = :id AND m.institutionId = :institutionId AND m.isDeleted = false")
    Optional<NfeMaterial> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);

    @Query("SELECT m FROM NfeMaterial m WHERE m.providerId = :providerId AND m.isDeleted = false")
    List<NfeMaterial> findByProviderId(@Param("providerId") UUID providerId);

    @Query("SELECT m FROM NfeMaterial m WHERE m.programId = :programId AND m.isDeleted = false")
    List<NfeMaterial> findByProgramId(@Param("programId") UUID programId);
}
