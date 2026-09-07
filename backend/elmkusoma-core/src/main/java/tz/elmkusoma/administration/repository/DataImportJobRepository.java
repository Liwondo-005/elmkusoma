package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.DataImportJob;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataImportJobRepository extends JpaRepository<DataImportJob, UUID> {

    @Query("SELECT j FROM DataImportJob j WHERE j.institutionId = :institutionId AND j.isDeleted = false ORDER BY j.createdAt DESC")
    List<DataImportJob> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT j FROM DataImportJob j WHERE j.institutionId = :institutionId AND j.status = :status AND j.isDeleted = false")
    List<DataImportJob> findByInstitutionIdAndStatus(@Param("institutionId") UUID institutionId,
                                                      @Param("status") DataImportJob.ImportStatus status);

    @Query("SELECT COUNT(j) FROM DataImportJob j WHERE j.institutionId = :institutionId AND j.status = 'PROCESSING' AND j.isDeleted = false")
    long countProcessingByInstitutionId(@Param("institutionId") UUID institutionId);
}
