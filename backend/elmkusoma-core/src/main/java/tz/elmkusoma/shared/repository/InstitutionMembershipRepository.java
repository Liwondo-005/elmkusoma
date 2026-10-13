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

    /**
     * Everyone holding a membership scoped to one provider - the provider's staff roster
     * (authority level D), read from the same rows that enforce ownership, so the roster can
     * never drift from what the access rules actually consult.
     *
     * <p>Includes inactive rows so a removed member still appears, greyed, rather than vanishing
     * from the owner's view and looking like their access was revoked by accident.</p>
     */
    @Query("SELECT m FROM InstitutionMembership m WHERE m.providerId = :providerId AND m.isDeleted = false")
    List<InstitutionMembership> findByProviderIdAndIsDeletedFalse(@Param("providerId") UUID providerId);

    /**
     * Every live membership for a page of users in one round trip.
     *
     * <p>Exists so the global user directory can show provider memberships without issuing one
     * query per row: a directory page of 25 users would otherwise cost 25 extra queries, on the
     * screen Platform Admin uses most.</p>
     */
    @Query("SELECT m FROM InstitutionMembership m WHERE m.userId IN :userIds AND m.isDeleted = false")
    List<InstitutionMembership> findByUserIdInAndIsDeletedFalse(@Param("userIds") List<UUID> userIds);
}