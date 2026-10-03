package tz.elmkusoma.identity.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tz.elmkusoma.config.security.RateLimitService;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.identity.domain.MfaFactor;
import tz.elmkusoma.identity.domain.RecoveryCode;
import tz.elmkusoma.identity.repository.MfaFactorRepository;
import tz.elmkusoma.identity.repository.RecoveryCodeRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TOTP correctness (RFC 6238 vectors) + enrollment/confirmation/recovery-code behavior.
 */
@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock private MfaFactorRepository mfaFactorRepository;
    @Mock private RecoveryCodeRepository recoveryCodeRepository;
    @Mock private UserRepository userRepository;
    @Mock private MfaEncryptionService mfaEncryptionService;
    @Mock private RateLimitService rateLimitService;

    @InjectMocks
    private MfaService mfaService;

    @org.junit.jupiter.api.BeforeEach
    void allowBudgets() {
        org.mockito.Mockito.lenient()
                .when(rateLimitService.allow(
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(true);
    }

    // RFC 6238 Appendix B, SHA-1, secret "12345678901234567890".
    private static byte[] rfcSecret() {
        return "12345678901234567890".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    @Test
    void totp_matchesRfc6238Vectors() {
        assertEquals("287082", TotpCodes.generate(rfcSecret(), Instant.ofEpochSecond(59)));
        assertEquals("081804", TotpCodes.generate(rfcSecret(), Instant.ofEpochSecond(1111111109L)));
        assertEquals("050471", TotpCodes.generate(rfcSecret(), Instant.ofEpochSecond(1111111111L)));
        assertEquals("005924", TotpCodes.generate(rfcSecret(), Instant.ofEpochSecond(1234567890L)));
        assertEquals("279037", TotpCodes.generate(rfcSecret(), Instant.ofEpochSecond(2000000000L)));
    }

    @Test
    void totp_rejectsWrongCodeAndMalformed() {
        assertFalse(TotpCodes.verify(rfcSecret(), "000000", Instant.ofEpochSecond(59)));
        assertFalse(TotpCodes.verify(rfcSecret(), "abcdef", Instant.ofEpochSecond(59)));
        assertFalse(TotpCodes.verify(rfcSecret(), null, Instant.ofEpochSecond(59)));
        assertTrue(TotpCodes.verify(rfcSecret(), "287082", Instant.ofEpochSecond(59)));
    }

    @Test
    void base32_roundTrip() {
        String secret = TotpCodes.randomSecret(new SecureRandom());
        assertEquals(32, secret.length());
        byte[] decoded = TotpCodes.decodeBase32(secret);
        assertEquals(20, decoded.length);
        assertThrows(IllegalArgumentException.class, () -> TotpCodes.decodeBase32("!!!!"));
    }

    @Test
    void confirmEnrollment_wrongCode_rejected() {
        UUID userId = UUID.randomUUID();
        UUID factorId = UUID.randomUUID();
        MfaFactor factor = MfaFactor.builder()
                .id(factorId).userId(userId).factorType("TOTP")
                .secretCiphertext("env").label("app").verified(false).build();
        when(mfaFactorRepository.findById(factorId)).thenReturn(Optional.of(factor));
        // Base32 of the RFC test secret "12345678901234567890".
        when(mfaEncryptionService.decrypt("env"))
                .thenReturn("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        // Wrong code must fail regardless of the stored secret.
        assertThrows(ForbiddenException.class,
                () -> mfaService.confirmEnrollment(userId, factorId, "000000"));
        assertFalse(factor.getVerified());
    }

    @Test
    void confirmEnrollment_correctCode_verifies() {
        UUID userId = UUID.randomUUID();
        UUID factorId = UUID.randomUUID();
        MfaFactor factor = MfaFactor.builder()
                .id(factorId).userId(userId).factorType("TOTP")
                .secretCiphertext("env").label("app").verified(false).build();
        when(mfaFactorRepository.findById(factorId)).thenReturn(Optional.of(factor));
        String base32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        when(mfaEncryptionService.decrypt("env")).thenReturn(base32);
        String code = TotpCodes.generate(TotpCodes.decodeBase32(base32), Instant.now());

        mfaService.confirmEnrollment(userId, factorId, code);

        assertTrue(factor.getVerified());
        assertNotNull(factor.getVerifiedAt());
    }

    @Test
    void recoveryCodes_generateAndConsumeOnce() {
        UUID userId = UUID.randomUUID();
        when(recoveryCodeRepository.findByUserIdAndUsedFalse(userId)).thenReturn(List.of());
        when(recoveryCodeRepository.save(any(RecoveryCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<String> codes = mfaService.generateRecoveryCodes(userId);

        assertEquals(10, codes.size());
        assertTrue(codes.stream().allMatch(c -> c.matches("[A-Z2-9]{4}-[A-Z2-9]{4}")));
        assertEquals(10, codes.stream().distinct().count());

        String consumedHash = MfaService.sha256(codes.get(0));
        RecoveryCode stored = RecoveryCode.builder()
                .id(UUID.randomUUID()).userId(userId).codeHash(consumedHash).used(false).build();
        // First lookup finds it unused; after consumption the store no longer returns it.
        when(recoveryCodeRepository.findByUserIdAndUsedFalse(userId))
                .thenReturn(List.of(stored), List.of());

        assertTrue(mfaService.consumeRecoveryCode(userId, codes.get(0)));
        assertTrue(stored.getUsed());
        assertFalse(mfaService.consumeRecoveryCode(userId, codes.get(0)));
    }

    @Test
    void encryption_roundTripWithRealService() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        MfaEncryptionService real = new MfaEncryptionService();
        ReflectionTestUtils.setField(real, "base64Key", Base64.getEncoder().encodeToString(key));
        String envelope = real.encrypt("JBSWY3DPEHPK3PXP");
        assertEquals("JBSWY3DPEHPK3PXP", real.decrypt(envelope));
        // Missing key fails fast instead of storing recoverable secrets.
        MfaEncryptionService unconfigured = new MfaEncryptionService();
        assertThrows(IllegalStateException.class, () -> unconfigured.encrypt("x"));
    }
}
