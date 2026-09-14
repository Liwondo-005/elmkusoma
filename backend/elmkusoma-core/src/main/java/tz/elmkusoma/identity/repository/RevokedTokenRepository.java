package tz.elmkusoma.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.identity.domain.RevokedToken;

import java.util.Optional;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    Optional<RevokedToken> findByTokenHash(String tokenHash);

    boolean existsByTokenHash(String tokenHash);
}
