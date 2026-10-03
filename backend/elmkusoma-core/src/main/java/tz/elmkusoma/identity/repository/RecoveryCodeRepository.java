package tz.elmkusoma.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.identity.domain.RecoveryCode;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, UUID> {

    List<RecoveryCode> findByUserIdAndUsedFalse(UUID userId);

    List<RecoveryCode> findByUserId(UUID userId);
}
