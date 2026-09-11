package tz.elmkusoma.student.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByAdmissionNumberAndIsDeletedFalse(String admissionNumber);

    Optional<Student> findByUserIdAndIsDeletedFalse(UUID userId);

    List<Student> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<Student> findByInstitutionIdAndStatusAndIsDeletedFalse(UUID institutionId, StudentStatus status);

    boolean existsByAdmissionNumberAndIsDeletedFalse(String admissionNumber);

    boolean existsByUserIdAndIsDeletedFalse(UUID userId);

    @Query("SELECT s FROM Student s JOIN tz.elmkusoma.shared.domain.User u ON s.userId = u.id " +
           "WHERE s.institutionId = :institutionId AND s.isDeleted = false AND u.isDeleted = false " +
           "AND (LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.gender) = LOWER(:query) " +
           "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Student> search(@Param("institutionId") UUID institutionId, @Param("query") String query);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.institutionId = :institutionId AND s.isDeleted = false")
    long countByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.institutionId = :institutionId AND s.status = :status AND s.isDeleted = false")
    long countByInstitutionIdAndStatus(@Param("institutionId") UUID institutionId, @Param("status") StudentStatus status);
}
