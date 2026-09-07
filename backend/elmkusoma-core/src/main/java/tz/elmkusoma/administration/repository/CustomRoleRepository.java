package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.CustomRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomRoleRepository extends JpaRepository<CustomRole, UUID> {

    Optional<CustomRole> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT r FROM CustomRole r WHERE r.institutionId = :institutionId AND r.isDeleted = false")
    List<CustomRole> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    boolean existsByNameAndInstitutionIdAndIsDeletedFalse(String name, UUID institutionId);
}
