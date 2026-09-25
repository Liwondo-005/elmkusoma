package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.InstitutionService;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstitutionServiceRepository extends JpaRepository<InstitutionService, UUID> {

    List<InstitutionService> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT i FROM InstitutionService i WHERE i.institutionId = :institutionId AND i.featureKey = :featureKey AND i.isDeleted = false")
    InstitutionService findByInstitutionIdAndFeatureKey(@Param("institutionId") UUID institutionId, @Param("featureKey") String featureKey);

    boolean existsByInstitutionIdAndFeatureKeyAndIsDeletedFalse(UUID institutionId, String featureKey);
}