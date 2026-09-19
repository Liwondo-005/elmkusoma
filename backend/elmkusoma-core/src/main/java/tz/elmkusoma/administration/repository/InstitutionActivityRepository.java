package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.InstitutionActivity;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstitutionActivityRepository extends JpaRepository<InstitutionActivity, UUID> {

    @Query("SELECT a FROM InstitutionActivity a WHERE a.institutionId = :institutionId ORDER BY a.createdAt DESC")
    List<InstitutionActivity> findRecentByInstitutionId(@Param("institutionId") UUID institutionId,
                                                         org.springframework.data.domain.Pageable pageable);

    long countByInstitutionIdAndIsReadFalse(UUID institutionId);

    @Query("SELECT a FROM InstitutionActivity a WHERE a.institutionId = :institutionId AND a.activityType = :type ORDER BY a.createdAt DESC")
    List<InstitutionActivity> findByInstitutionIdAndType(@Param("institutionId") UUID institutionId,
                                                          @Param("type") String type,
                                                          org.springframework.data.domain.Pageable pageable);
}
