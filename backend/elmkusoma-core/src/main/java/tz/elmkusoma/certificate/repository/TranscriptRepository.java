package tz.elmkusoma.certificate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.certificate.domain.Transcript;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {

    Optional<Transcript> findByIdAndIsDeletedFalse(UUID id);

    Optional<Transcript> findBySerialNumberAndIsDeletedFalse(String serialNumber);

    @Query("SELECT t FROM Transcript t WHERE t.institutionId = :institutionId AND t.isDeleted = false ORDER BY t.createdAt DESC")
    List<Transcript> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT t FROM Transcript t WHERE t.studentId = :studentId AND t.isDeleted = false ORDER BY t.createdAt DESC")
    List<Transcript> findAllByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT t FROM Transcript t WHERE t.studentId = :studentId AND t.academicYear = :academicYear AND t.term = :term AND t.isDeleted = false")
    Optional<Transcript> findByStudentAndPeriod(@Param("studentId") UUID studentId,
                                                 @Param("academicYear") String academicYear,
                                                 @Param("term") String term);
}
