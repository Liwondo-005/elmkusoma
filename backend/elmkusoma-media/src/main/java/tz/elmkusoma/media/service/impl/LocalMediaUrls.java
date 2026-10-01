package tz.elmkusoma.media.service.impl;

import io.jsonwebtoken.io.Decoders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * URL signing for the local filesystem storage backend.  Mirrors the semantics
 * of MinIO presigned URLs: a short-lived URL that grants access to exactly one
 * object without carrying a JWT.  HMAC-SHA256 over the shared JWT secret keeps
 * the key material identical to the rest of the service.
 */
public final class LocalMediaUrls {

    private LocalMediaUrls() {
    }

    public static String sign(String objectKey, long expiresAtEpochSeconds, String base64Secret) {
        return hmac(objectKey + "|" + expiresAtEpochSeconds, base64Secret);
    }

    public static boolean verify(String objectKey, long expiresAtEpochSeconds, String signature,
                                 String base64Secret) {
        if (objectKey == null || objectKey.isBlank() || signature == null || signature.isBlank()) {
            return false;
        }
        String expected = sign(objectKey, expiresAtEpochSeconds, base64Secret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeKey(String objectKey) {
        return URLEncoder.encode(objectKey, StandardCharsets.UTF_8);
    }

    private static String hmac(String payload, String base64Secret) {
        try {
            byte[] key = Decoders.BASE64.decode(base64Secret);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign local media URL", e);
        }
    }
}
