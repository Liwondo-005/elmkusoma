package tz.elmkusoma.nfe.provider.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.common.TenantRepository;
import tz.elmkusoma.nfe.provider.domain.EducationProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EducationProviderRepository extends TenantRepository<EducationProvider, UUID> {

    @Query("SELECT p FROM EducationProvider p WHERE p.institutionId = :institutionId AND p.isDeleted = false")
    List<EducationProvider> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT p FROM EducationProvider p WHERE p.id = :id AND p.institutionId = :institutionId AND p.isDeleted = false")
    Optional<EducationProvider> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);

    @Query("SELECT p FROM EducationProvider p WHERE p.institutionId = :institutionId AND p.isActive = true AND p.isDeleted = false")
    List<EducationProvider> findActiveByInstitutionId(@Param("institutionId") UUID institutionId);

    boolean existsByNameAndInstitutionIdAndIsDeletedFalse(String name, UUID institutionId);
}
