package tz.elmkusoma.nfe.learner.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.common.TenantRepository;
import tz.elmkusoma.nfe.learner.domain.NfeLearner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NfeLearnerRepository extends TenantRepository<NfeLearner, UUID> {

    @Query("SELECT l FROM NfeLearner l WHERE l.institutionId = :institutionId AND l.isDeleted = false")
    List<NfeLearner> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT l FROM NfeLearner l WHERE l.id = :id AND l.institutionId = :institutionId AND l.isDeleted = false")
    Optional<NfeLearner> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);

    @Query("SELECT l FROM NfeLearner l WHERE l.providerId = :providerId AND l.isDeleted = false")
    List<NfeLearner> findByProviderId(@Param("providerId") UUID providerId);

    @Query("SELECT l FROM NfeLearner l WHERE l.userId = :userId AND l.isDeleted = false")
    List<NfeLearner> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT l FROM NfeLearner l WHERE l.institutionId = :institutionId AND l.status = :status AND l.isDeleted = false")
    List<NfeLearner> findByStatusAndInstitutionId(@Param("status") String status, @Param("institutionId") UUID institutionId);

    @Query("SELECT COUNT(l) FROM NfeLearner l WHERE l.institutionId = :institutionId AND l.isDeleted = false")
    long countByInstitutionId(@Param("institutionId") UUID institutionId);
}
