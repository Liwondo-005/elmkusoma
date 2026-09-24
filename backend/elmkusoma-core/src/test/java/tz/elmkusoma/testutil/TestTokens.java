package tz.elmkusoma.testutil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;

/**
 * Builds JWTs for tests: user access tokens (same secret/config as JwtTokenProvider)
 * and LiveKit webhook Authorization headers (HS256 over raw body, iss=devkey, sha256 hex claim).
 */
public final class TestTokens {

    public static final String ADMIN_EMAIL = "admin@elmkusoma.tz";
    public static final String TEACHER_EMAIL = "teacher@elmkusoma.tz";
    public static final String STUDENT_EMAIL = "student@elmkusoma.tz";
    public static final String LEARNER_EMAIL = "learner@elmkusoma.tz";
    public static final String OTHER_STUDENT_EMAIL = "otherstudent@elmkusoma.tz";

    /** Must match application-test.properties jwt.secret (Base64). */
    private static final String JWT_SECRET_B64 = "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLTIwMjQ=";
    /** Must match livekit.server.api-key / livekit.server.api-secret. */
    private static final String LIVEKIT_API_KEY = "devkey";
    private static final String LIVEKIT_API_SECRET = "devsecret";

    private TestTokens() {
    }

    public static String userToken(String email) {
        byte[] keyBytes = Decoders.BASE64.decode(JWT_SECRET_B64);
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(Keys.hmacShaKeyFor(keyBytes))
                .compact();
    }

    public static String adminToken() {
        return userToken(ADMIN_EMAIL);
    }

    public static String teacherToken() {
        return userToken(TEACHER_EMAIL);
    }

    public static String studentToken() {
        return userToken(STUDENT_EMAIL);
    }

    public static String learnerToken() {
        return userToken(LEARNER_EMAIL);
    }

    public static String otherStudentToken() {
        return userToken(OTHER_STUDENT_EMAIL);
    }

    /**
     * Authorization header value ("Bearer …") that LiveKitWebhookController.verifyWebhookSignature
     * accepts for the exact raw body bytes.
     */
    public static String webhookAuthHeader(String jsonBody) {
        try {
            byte[] body = jsonBody.getBytes(StandardCharsets.UTF_8);
            String headerJson = "{\"alg\":\"HS256\"}";
            String payloadJson = "{\"iss\":\"" + LIVEKIT_API_KEY + "\",\"sha256\":\"" + sha256Hex(body) + "\"}";
            String signingInput = base64Url(headerJson.getBytes(StandardCharsets.UTF_8))
                    + "."
                    + base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(LIVEKIT_API_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            return "Bearer " + signingInput + "." + base64Url(sig);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign webhook body", e);
        }
    }

    private static String sha256Hex(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
