package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.InstitutionInvitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionInvitationRepository extends JpaRepository<InstitutionInvitation, UUID> {

    Optional<InstitutionInvitation> findByTokenAndIsDeletedFalse(String token);

    @Query("SELECT i FROM InstitutionInvitation i WHERE i.institutionId = :institutionId AND i.isDeleted = false ORDER BY i.createdAt DESC")
    List<InstitutionInvitation> findByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT i FROM InstitutionInvitation i WHERE i.institutionId = :institutionId AND i.status = 'PENDING' AND i.isDeleted = false")
    List<InstitutionInvitation> findPendingByInstitutionId(@Param("institutionId") UUID institutionId);

    boolean existsByEmailAndInstitutionIdAndStatusAndIsDeletedFalse(String email, UUID institutionId, String status);
}
