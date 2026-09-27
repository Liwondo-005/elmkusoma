package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.StudentSavedResource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentSavedResourceRepository extends JpaRepository<StudentSavedResource, UUID> {

    Optional<StudentSavedResource> findByStudentIdAndResourceId(UUID studentId, UUID resourceId);

    List<StudentSavedResource> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<StudentSavedResource> findByResourceIdAndIsDeletedFalse(UUID resourceId);

    @Query("SELECT ssr FROM StudentSavedResource ssr WHERE ssr.studentId = :studentId AND ssr.isDeleted = false ORDER BY ssr.createdAt DESC")
    List<StudentSavedResource> findByStudentIdOrderBySavedAtDesc(@Param("studentId") UUID studentId);

    boolean existsByStudentIdAndResourceId(UUID studentId, UUID resourceId);

    long countByStudentIdAndIsDeletedFalse(UUID studentId);

    void deleteByStudentIdAndResourceId(UUID studentId, UUID resourceId);
}