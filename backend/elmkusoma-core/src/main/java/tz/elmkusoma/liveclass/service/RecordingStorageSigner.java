package tz.elmkusoma.liveclass.service;

import org.springframework.stereotype.Component;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Signs one server-side storage read so the recording playback proxy can fetch a
 * <em>private</em> object.
 *
 * <p>{@link RecordingPlaybackService} streams a recording by making an HTTP request to the
 * location persisted at publication time. That location is an <em>unsigned</em> object URL,
 * and the bucket is private by design, so an unauthenticated request is answered with 401 and
 * the caller turns that into a 502 - no bytes, no seeking, no playback. The credentials needed
 * to read the bucket already exist in this process: they are the same
 * {@code livekit.egress.s3-*} values used to write the object in the first place. The read is
 * therefore signed here, which is the alternative to making the bucket public - and the bucket
 * stays private.</p>
 *
 * <p>Deliberate properties of this class:</p>
 *
 * <ul>
 *   <li><b>No second credential store.</b> Signing reuses {@code livekit.egress.*}, the
 *       configuration the writer already uses, so there is exactly one place where storage
 *       credentials live.</li>
 *   <li><b>No new dependency.</b> AWS Signature Version 4 is HMAC-SHA256 over the JDK's
 *       {@link Mac}; core still owns no S3 or B2 client and makes no SDK call.</li>
 *   <li><b>Never signs a foreign host.</b> The scheme, host and port must all match the
 *       configured egress endpoint, so a hostile or rotated location can never elicit a
 *       signature - and can never receive these credentials in an {@code Authorization}
 *       header.</li>
 *   <li><b>Never signs an already-authorized URL.</b> A location that carries a query string
 *       is passed through untouched: it already carries its own short-lived authorization,
 *       and re-signing it would corrupt it.</li>
 *   <li><b>Nothing is logged or returned.</b> The signature, the secret and the object key
 *       never reach a log line, an exception message, or any client - the headers are consumed
 *       by the outbound connection and discarded.</li>
 * </ul>
 */
@Component
public class RecordingStorageSigner {

    /** {@code 20150830T000000Z} shaped, always UTC, regardless of the server's zone. */
    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    /** sha256 of the empty body; every signed request here is a body-less GET. */
    private static final String EMPTY_PAYLOAD_SHA256 = sha256Hex(new byte[0]);

    private final LiveKitConfig config;

    public RecordingStorageSigner(LiveKitConfig config) {
        this.config = config;
    }

    /**
     * Headers to add to an outbound storage request, or an empty map when the request must be
     * sent unsigned (no credentials configured, the target is not the configured endpoint, or
     * the URL already carries its own authorization).
     */
    public Map<String, String> headersFor(URI target) {
        return headersFor(target, Instant.now());
    }

    Map<String, String> headersFor(URI target, Instant when) {
        LiveKitConfig.EgressConfig egress = config.getEgress();
        if (target == null || target.getHost() == null) {
            return Map.of();
        }
        String accessKey = egress.getAccessKey();
        String secret = egress.getSecret();
        if (accessKey == null || accessKey.isBlank() || secret == null || secret.isBlank()) {
            return Map.of();
        }
        // A URL that already carries a query is already authorized; leave it exactly as-is.
        if (target.getRawQuery() != null && !target.getRawQuery().isEmpty()) {
            return Map.of();
        }
        URI endpoint;
        try {
            endpoint = new URI(egress.getEndpoint());
        } catch (Exception ex) {
            return Map.of();
        }
        if (endpoint.getHost() == null
                || !endpoint.getHost().equalsIgnoreCase(target.getHost())
                || defaultPort(endpoint) != defaultPort(target)) {
            return Map.of();
        }
        String region = egress.getRegion() == null || egress.getRegion().isBlank()
                ? "auto"
                : egress.getRegion().trim();
        return authorizationHeaders("GET", target, accessKey, secret, region, when);
    }

    /**
     * Builds the three SigV4 headers for a body-less GET against {@code target}.
     *
     * <p>Package-private with an explicit clock so tests can pin the timestamp; the only
     * caller outside this class is {@link #headersFor(URI)}.</p>
     */
    static Map<String, String> authorizationHeaders(String method, URI target, String accessKey,
                                                    String secret, String region, Instant when) {
        String amzDate = AMZ_DATE.format(when);
        String dateStamp = amzDate.substring(0, 8);
        String canonicalUri = target.getRawPath() == null || target.getRawPath().isEmpty()
                ? "/"
                : target.getRawPath();

        // Sorted by header name, which is what the canonical request requires.
        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("host", hostHeaderValue(target));
        headers.put("x-amz-content-sha256", EMPTY_PAYLOAD_SHA256);
        headers.put("x-amz-date", amzDate);

        StringBuilder canonicalHeaders = new StringBuilder();
        StringBuilder signedHeaders = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            canonicalHeaders.append(entry.getKey()).append(':').append(entry.getValue().trim()).append('\n');
            if (signedHeaders.length() > 0) {
                signedHeaders.append(';');
            }
            signedHeaders.append(entry.getKey());
        }

        String canonicalRequest = method + '\n' + canonicalUri + '\n' + '\n'
                + canonicalHeaders + '\n' + signedHeaders + '\n' + EMPTY_PAYLOAD_SHA256;
        String scope = dateStamp + '/' + region + "/s3/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + scope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        byte[] signingKey = deriveSigningKey(secret.getBytes(StandardCharsets.UTF_8), dateStamp, region, "s3");
        String signature = hex(hmac(signingKey, stringToSign));

        Map<String, String> result = new TreeMap<>();
        result.put("x-amz-date", amzDate);
        result.put("x-amz-content-sha256", EMPTY_PAYLOAD_SHA256);
        result.put("Authorization", "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + scope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature);
        return result;
    }

    /**
     * The four-step SigV4 signing key: {@code AWS4secret -> date -> region -> service ->
     * aws4_request}.
     *
     * <p>Each step feeds the <em>raw bytes</em> of the previous HMAC into the next one. Two
     * separate mistakes here are invisible downstream: feeding the hex text back in instead of
     * the bytes, or deriving from the bare secret without the mandatory {@code AWS4} prefix.
     * Either one still yields a well-formed 64-character signature, so nothing in the
     * application complains - the provider just answers {@code SignatureDoesNotMatch} and
     * playback dies with a 502. {@code RecordingStorageSignerTest} pins this method against
     * the signing key published in the AWS Signature Version 4 documentation, so neither can
     * return unnoticed.</p>
     *
     * @param secretKey the raw secret, <em>without</em> any {@code AWS4} prefix; the prefix is
     *                  applied here because forgetting it is the failure this doc describes
     */
    static byte[] deriveSigningKey(byte[] secretKey, String dateStamp, String region, String service) {
        byte[] dateKey = hmac(prefixed(secretKey), dateStamp);
        byte[] regionKey = hmac(dateKey, region);
        byte[] serviceKey = hmac(regionKey, service);
        return hmac(serviceKey, "aws4_request");
    }

    /** {@code "AWS4"} in UTF-8, prepended to the secret as the SigV4 scope requires. */
    private static byte[] prefixed(byte[] secretKey) {
        byte[] prefix = "AWS4".getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[prefix.length + secretKey.length];
        System.arraycopy(prefix, 0, combined, 0, prefix.length);
        System.arraycopy(secretKey, 0, combined, prefix.length, secretKey.length);
        return combined;
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            // HmacSHA256 is mandatory on every JRE; reaching this means the JVM is broken.
            throw new IllegalStateException("Unable to sign the storage request", ex);
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash the storage request", ex);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16));
            sb.append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

    /** The Host header as {@code java.net.HttpURLConnection} will actually send it. */
    private static String hostHeaderValue(URI uri) {
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ':' + port;
    }

    private static int defaultPort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
