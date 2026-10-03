package tz.elmkusoma.identity.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * Minimal RFC 6238 TOTP (SHA-1, 30-second step, 6 digits).
 *
 * <p>Hand-rolled over JCA only (no new dependencies) with injectable time so
 * verification is unit-testable against the RFC vectors. Accepts the current
 * step plus or minus one step to tolerate clock skew.
 */
public final class TotpCodes {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int SKEW_STEPS = 1;

    private TotpCodes() {
    }

    public static String generate(byte[] secret, Instant at) {
        long counter = at.getEpochSecond() / TIME_STEP_SECONDS;
        int code = hotp(secret, counter) % 1_000_000;
        return String.format("%06d", code);
    }

    public static boolean verify(byte[] secret, String code, Instant at) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long counter = at.getEpochSecond() / TIME_STEP_SECONDS;
        for (long c = counter - SKEW_STEPS; c <= counter + SKEW_STEPS; c++) {
            int expected = hotp(secret, c) % 1_000_000;
            if (String.format("%06d", expected).equals(code)) {
                return true;
            }
        }
        return false;
    }

    private static int hotp(byte[] secret, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0F;
            return ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA1 unavailable", e);
        }
    }

    /**
     * RFC 4648 Base32 decoding (no padding required) for authenticator secrets.
     */
    public static byte[] decodeBase32(String base32) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        String clean = base32.trim().replace("=", "").toUpperCase();
        ByteBuffer out = ByteBuffer.allocate(clean.length() * 5 / 8 + 1);
        int buffer = 0;
        int bits = 0;
        for (char c : clean.toCharArray()) {
            int value = alphabet.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid Base32 character");
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                out.put((byte) ((buffer >> bits) & 0xFF));
            }
        }
        out.flip();
        byte[] result = new byte[out.remaining()];
        out.get(result);
        return result;
    }

    /**
     * Random 160-bit secret encoded as Base32 (32 chars), suitable for
     * authenticator apps and otpauth:// URIs.
     */
    public static String randomSecret(java.security.SecureRandom random) {
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : secret) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                sb.append(alphabet.charAt((buffer >> bits) & 0x1F));
            }
        }
        if (bits > 0) {
            sb.append(alphabet.charAt((buffer << (5 - bits)) & 0x1F));
        }
        return sb.toString();
    }
}
