package tz.elmkusoma.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.identity.domain.VerificationCode;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    Optional<VerificationCode> findTopByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(String email, String code);

    Optional<VerificationCode> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
}
