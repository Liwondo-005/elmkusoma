package tz.elmkusoma.teacher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.teacher.domain.Teacher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    @Query("SELECT t FROM Teacher t WHERE t.institutionId = :institutionId AND t.isDeleted = false")
    List<Teacher> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT t FROM Teacher t WHERE t.id = :id AND t.institutionId = :institutionId AND t.isDeleted = false")
    Optional<Teacher> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);

    @Query("SELECT t FROM Teacher t WHERE t.userId = :userId AND t.institutionId = :institutionId AND t.isDeleted = false")
    Optional<Teacher> findByUserIdAndInstitutionId(@Param("userId") UUID userId, @Param("institutionId") UUID institutionId);

    boolean existsByUserIdAndInstitutionIdAndIsDeletedFalse(UUID userId, UUID institutionId);
}
