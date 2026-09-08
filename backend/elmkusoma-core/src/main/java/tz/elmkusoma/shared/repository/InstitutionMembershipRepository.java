package tz.elmkusoma.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.InstitutionMembership;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstitutionMembershipRepository extends JpaRepository<InstitutionMembership, Long> {

    List<InstitutionMembership> findByUserIdAndIsActiveTrue(UUID userId);

    List<InstitutionMembership> findByInstitutionIdAndIsActiveTrue(UUID institutionId);

    boolean existsByUserIdAndInstitutionIdAndIsActiveTrue(UUID userId, UUID institutionId);
}
