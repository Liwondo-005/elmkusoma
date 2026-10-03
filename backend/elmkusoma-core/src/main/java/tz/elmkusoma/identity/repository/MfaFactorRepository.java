package tz.elmkusoma.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.identity.domain.MfaFactor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfaFactorRepository extends JpaRepository<MfaFactor, UUID> {

    List<MfaFactor> findByUserIdAndVerifiedTrue(UUID userId);

    Optional<MfaFactor> findFirstByUserIdAndVerifiedTrueOrderByVerifiedAtDesc(UUID userId);

    List<MfaFactor> findByUserId(UUID userId);
}
