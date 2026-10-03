package tz.elmkusoma.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.config.security.RateLimitService;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.RateLimitExceededException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.identity.domain.MfaFactor;
import tz.elmkusoma.identity.domain.RecoveryCode;
import tz.elmkusoma.identity.repository.MfaFactorRepository;
import tz.elmkusoma.identity.repository.RecoveryCodeRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Single shared step-up authentication system (TOTP + single-use recovery
 * codes) for all roles. No per-role MFA variants.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int RECOVERY_CODE_COUNT = 10;

    private final MfaFactorRepository mfaFactorRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final UserRepository userRepository;
    private final MfaEncryptionService mfaEncryptionService;
    private final RateLimitService rateLimitService;

    public record Enrollment(String factorId, String otpauthUri, String base32Secret) {
    }

    private void checkBudget(String bucket, String account, int limit, long windowMillis) {
        if (rateLimitService != null && account != null
                && !rateLimitService.allow(bucket + ":" + account, limit, windowMillis)) {
            throw new RateLimitExceededException();
        }
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Starts enrollment: persists an UNVERIFIED encrypted factor, returns provisioning material once. */
    @Transactional
    public Enrollment startEnrollment(UUID userId, String email) {
        MfaFactor factor = MfaFactor.builder()
                .userId(userId)
                .factorType("TOTP")
                .secretCiphertext(mfaEncryptionService.encrypt(TotpCodes.randomSecret(SECURE_RANDOM)))
                .label("Authenticator app")
                .verified(false)
                .build();
        // Encrypt first, then keep raw only in memory for the URI below.
        String rawSecret = mfaEncryptionService.decrypt(factor.getSecretCiphertext());
        factor = mfaFactorRepository.save(factor);
        String uri = "otpauth://totp/ELMKUSOMA:" + email + "?secret=" + rawSecret
                + "&issuer=ELMKUSOMA&digits=6&period=30";
        log.info("MFA enrollment started for user {}", userId);
        return new Enrollment(factor.getId().toString(), uri, rawSecret);
    }

    /** Confirms enrollment by verifying a code from the provisioned app. */
    @Transactional
    public void confirmEnrollment(UUID userId, UUID factorId, String code) {
        checkBudget("mfa-confirm", userId.toString(), 10, 60_000L);
        MfaFactor factor = mfaFactorRepository.findById(factorId)
                .orElseThrow(() -> new ResourceNotFoundException("MFA factor", "id", factorId));
        if (!factor.getUserId().equals(userId)) {
            throw new ForbiddenException("MFA factor does not belong to this account");
        }
        if (Boolean.TRUE.equals(factor.getVerified())) {
            return;
        }
        String rawSecret = mfaEncryptionService.decrypt(factor.getSecretCiphertext());
        if (!TotpCodes.verify(TotpCodes.decodeBase32(rawSecret), code, Instant.now())) {
            throw new ForbiddenException("Invalid authenticator code");
        }
        factor.setVerified(true);
        factor.setVerifiedAt(LocalDateTime.now());
        factor.setLastUsedAt(LocalDateTime.now());
        mfaFactorRepository.save(factor);
        log.info("MFA enrollment confirmed for user {}", userId);
    }

    /** Verifies a TOTP code against the latest verified factor. */
    @Transactional
    public void verifyTotp(UUID userId, String code) {
        checkBudget("mfa-verify", userId.toString(), 10, 60_000L);
        MfaFactor factor = mfaFactorRepository.findFirstByUserIdAndVerifiedTrueOrderByVerifiedAtDesc(userId)
                .orElseThrow(() -> new ForbiddenException("No verified authenticator for this account"));
        String rawSecret = mfaEncryptionService.decrypt(factor.getSecretCiphertext());
        if (!TotpCodes.verify(TotpCodes.decodeBase32(rawSecret), code, Instant.now())) {
            throw new ForbiddenException("Invalid authenticator code");
        }
        factor.setLastUsedAt(LocalDateTime.now());
        mfaFactorRepository.save(factor);
    }

    public boolean hasVerifiedFactor(UUID userId) {
        return mfaFactorRepository.findFirstByUserIdAndVerifiedTrueOrderByVerifiedAtDesc(userId).isPresent();
    }

    /**
     * Issues a fresh set of single-use recovery codes, burning any unused
     * predecessors. Raw codes are returned ONCE for display; only hashes persist.
     */
    @Transactional
    public List<String> generateRecoveryCodes(UUID userId) {
        recoveryCodeRepository.findByUserIdAndUsedFalse(userId).forEach(c -> {
            c.setUsed(true);
            c.setUsedAt(LocalDateTime.now());
            recoveryCodeRepository.save(c);
        });
        List<String> raw = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                sb.append(CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.substring(0, 4) + "-" + sb.substring(4);
            recoveryCodeRepository.save(RecoveryCode.builder()
                    .userId(userId)
                    .codeHash(sha256(code))
                    .used(false)
                    .build());
            raw.add(code);
        }
        log.info("Recovery codes regenerated for user {}", userId);
        return raw;
    }

    /** Consumes one recovery code; returns false (no oracle detail) when none match. */
    @Transactional
    public boolean consumeRecoveryCode(UUID userId, String code) {
        checkBudget("recovery-code", userId.toString(), 10, 3_600_000L);
        String normalized = code != null ? code.trim().toUpperCase() : "";
        for (RecoveryCode candidate : recoveryCodeRepository.findByUserIdAndUsedFalse(userId)) {
            if (sha256(normalized).equals(candidate.getCodeHash())) {
                candidate.setUsed(true);
                candidate.setUsedAt(LocalDateTime.now());
                recoveryCodeRepository.save(candidate);
                log.info("Recovery code consumed for user {}", userId);
                return true;
            }
        }
        return false;
    }

    public long remainingCodes(UUID userId) {
        return recoveryCodeRepository.findByUserIdAndUsedFalse(userId).size();
    }

    /**
     * Burns every factor for the account (used by emergency recovery when the
     * authenticator itself is lost), forcing clean re-enrollment afterwards.
     */
    @Transactional
    public void revokeFactors(UUID userId) {
        mfaFactorRepository.findByUserId(userId).forEach(factor -> {
            factor.setVerified(false);
            mfaFactorRepository.save(factor);
        });
        log.info("MFA factors revoked for user {}", userId);
    }
}
