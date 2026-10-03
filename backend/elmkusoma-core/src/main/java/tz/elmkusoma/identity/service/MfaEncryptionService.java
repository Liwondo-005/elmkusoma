package tz.elmkusoma.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM envelope for TOTP secrets at rest.
 *
 * <p>The data key comes exclusively from the {@code MFA_ENCRYPTION_KEY}
 * environment variable (Base64, 32 bytes). Enrollment fails fast with a clear
 * error when it is absent — there is intentionally no default, so a
 * misconfigured deployment cannot silently store recoverable secrets.
 */
@Service
public class MfaEncryptionService {

    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecureRandom random = new SecureRandom();

    @Value("${MFA_ENCRYPTION_KEY:}")
    private String base64Key = "";

    private SecretKeySpec dataKey() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "MFA_ENCRYPTION_KEY is not set; refusing to handle authenticator secrets");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY is not valid Base64", e);
        }
        if (raw.length != 32) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY must decode to 32 bytes (AES-256)");
        }
        return new SecretKeySpec(raw, "AES");
    }

    /** Returns {@code base64(iv) + "." + base64(ciphertext)}. */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, dataKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + "."
                    + Base64.getEncoder().encodeToString(cipherText);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("MFA secret encryption failed", e);
        }
    }

    public String decrypt(String envelope) {
        try {
            String[] parts = envelope.split("\\.", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Malformed MFA secret envelope");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, dataKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("MFA secret decryption failed", e);
        }
    }
}
