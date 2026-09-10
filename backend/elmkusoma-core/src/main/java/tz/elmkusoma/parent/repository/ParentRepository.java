package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.Parent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {

    @Query("SELECT p FROM Parent p WHERE p.institutionId = :institutionId AND p.isDeleted = false")
    List<Parent> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT p FROM Parent p WHERE p.id = :id AND p.institutionId = :institutionId AND p.isDeleted = false")
    Optional<Parent> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);

    @Query("SELECT p FROM Parent p WHERE p.userId = :userId AND p.institutionId = :institutionId AND p.isDeleted = false")
    Optional<Parent> findByUserIdAndInstitutionId(@Param("userId") UUID userId, @Param("institutionId") UUID institutionId);

    boolean existsByUserIdAndInstitutionIdAndIsDeletedFalse(UUID userId, UUID institutionId);

    @Query("SELECT p FROM Parent p WHERE p.userId = :userId AND p.isDeleted = false")
    List<Parent> findAllByUserId(@Param("userId") UUID userId);
}
