package tz.elmkusoma.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.InstitutionMembership;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstitutionMembershipRepository extends JpaRepository<InstitutionMembership, Long> {

    List<InstitutionMembership> findByUserIdAndIsActiveTrue(UUID userId);

    List<InstitutionMembership> findByInstitutionIdAndIsActiveTrue(UUID institutionId);

    boolean existsByUserIdAndInstitutionIdAndIsActiveTrue(UUID userId, UUID institutionId);

    long countByRoleAndIsDeletedFalse(InstitutionMembership.Role role);

    @Query("SELECT COUNT(m) FROM InstitutionMembership m WHERE m.institutionId IN :institutionIds AND m.role = :role AND m.isDeleted = false")
    long countByInstitutionIdsAndRoleAndIsDeletedFalse(@Param("institutionIds") List<UUID> institutionIds, @Param("role") InstitutionMembership.Role role);

    @Query("SELECT COUNT(m) FROM InstitutionMembership m WHERE m.institutionId IN :institutionIds AND m.isDeleted = false")
    long countByInstitutionIdsAndIsDeletedFalse(@Param("institutionIds") List<UUID> institutionIds);
}