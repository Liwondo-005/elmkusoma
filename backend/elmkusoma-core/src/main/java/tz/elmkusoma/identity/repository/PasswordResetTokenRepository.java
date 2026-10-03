package tz.elmkusoma.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.identity.domain.PasswordResetToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    List<PasswordResetToken> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
