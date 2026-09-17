package tz.elmkusoma.nfe.assessment.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.common.TenantRepository;
import tz.elmkusoma.nfe.assessment.domain.NfeAssessment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NfeAssessmentRepository extends TenantRepository<NfeAssessment, UUID> {

    @Query("SELECT a FROM NfeAssessment a WHERE a.providerId = :providerId AND a.institutionId = :institutionId AND a.isDeleted = false")
    List<NfeAssessment> findByProviderId(@Param("providerId") UUID providerId, @Param("institutionId") UUID institutionId);

    @Query("SELECT a FROM NfeAssessment a WHERE a.programId = :programId AND a.institutionId = :institutionId AND a.isDeleted = false")
    List<NfeAssessment> findByProgramId(@Param("programId") UUID programId, @Param("institutionId") UUID institutionId);

    @Query("SELECT a FROM NfeAssessment a WHERE a.isPublished = :isPublished AND a.institutionId = :institutionId AND a.isDeleted = false")
    List<NfeAssessment> findByIsPublished(@Param("isPublished") Boolean isPublished, @Param("institutionId") UUID institutionId);

    @Query("SELECT a FROM NfeAssessment a WHERE a.id = :id AND a.institutionId = :institutionId AND a.isDeleted = false")
    Optional<NfeAssessment> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);
}
