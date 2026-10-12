package tz.elmkusoma.liveclass.service;

import org.junit.jupiter.api.Test;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The private-bucket read signature.
 *
 * <p>Without this signature the playback proxy sends an unauthenticated GET at a private
 * object, B2 answers {@code 401 UnauthorizedAccess}, and the controller turns that into a 502
 * - the recording never plays even though the object exists and the key can read it. That
 * failure mode is invisible to the rest of the suite, because every other playback test points
 * the proxy at a stub server that answers any request. So this class pins the parts that a
 * stub server cannot: the key derivation, the refusal to sign anything but the configured
 * endpoint, and the shape of the headers that actually go on the wire.</p>
 */
class RecordingStorageSignerTest {

    /** Published in the AWS Signature Version 4 documentation for key derivation. */
    private static final String AWS_DOC_SECRET = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private static final String AWS_DOC_KEY_HEX =
            "c4afb1cc5771d871763a393e44b703571b55cc28424d1a5e86da6ed3c154a4b9";

    private static final Instant PINNED = Instant.parse("2026-10-12T11:27:38Z");
    private static final String ENDPOINT = "https://s3.example.test:9443";

    private static LiveKitConfig configWith(String endpoint, String accessKey, String secret) {
        LiveKitConfig config = new LiveKitConfig();
        config.getEgress().setEndpoint(endpoint);
        config.getEgress().setOutputBucket("recordings-bucket");
        config.getEgress().setRegion("eu-central-1");
        config.getEgress().setAccessKey(accessKey);
        config.getEgress().setSecret(secret);
        return config;
    }

    /**
     * The four-step HMAC chain must feed each step's <em>raw bytes</em> into the next.
     *
     * <p>Feeding the hex text back in instead still produces a well-formed 64-character
     * signature, so nothing downstream complains - the provider simply answers
     * {@code SignatureDoesNotMatch} and playback dies with a 502. This vector is the reason
     * that mistake cannot come back silently.</p>
     */
    @Test
    void theSigningKeyMatchesThePublishedAwsVector() {
        byte[] key = RecordingStorageSigner.deriveSigningKey(
                AWS_DOC_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "20150830", "us-east-1", "iam");
        StringBuilder hex = new StringBuilder();
        for (byte b : key) {
            // Masked deliberately: %x on a negative byte sign-extends to 8 characters, which
            // would corrupt the comparison instead of failing it cleanly.
            hex.append(String.format("%02x", b & 0xff));
        }
        assertEquals(AWS_DOC_KEY_HEX, hex.toString(),
                "the SigV4 signing-key chain must reproduce the AWS-published vector");
    }

    @Test
    void aRequestForTheConfiguredEndpointIsSigned() {
        LiveKitConfig config = configWith(ENDPOINT, "AKIAEXAMPLE", "secret-value");
        Map<String, String> headers = new RecordingStorageSigner(config)
                .headersFor(URI.create(ENDPOINT + "/recordings-bucket/recordings/cls/lesson.mp4"), PINNED);

        assertTrue(headers.containsKey("Authorization"), "the request must carry a signature");
        assertTrue(headers.get("Authorization").startsWith("AWS4-HMAC-SHA256 Credential=AKIAEXAMPLE/"),
                "the credential scope must name the configured access key");
        assertTrue(headers.get("Authorization").contains("/20261012/eu-central-1/s3/aws4_request"),
                "the scope must carry the request date, the configured region, service s3 and aws4_request");
        assertTrue(headers.get("Authorization").contains("SignedHeaders=host;x-amz-content-sha256;x-amz-date"),
                "host, payload hash and date must all be signed");
        assertTrue(headers.get("Authorization").matches(".*Signature=[0-9a-f]{64}$"),
                "the signature must be 64 hex characters");
        assertEquals("20261012T112738Z", headers.get("x-amz-date"), "the timestamp must be UTC and pinned");
        assertEquals(64, headers.get("x-amz-content-sha256").length(),
                "the payload hash must be present so the provider can verify the body-less GET");
    }

    @Test
    void theSameRequestSignedTwiceAtTheSameInstantIsIdentical() {
        LiveKitConfig config = configWith(ENDPOINT, "AKIAEXAMPLE", "secret-value");
        RecordingStorageSigner signer = new RecordingStorageSigner(config);
        URI target = URI.create(ENDPOINT + "/recordings-bucket/recordings/cls/lesson.mp4");

        assertEquals(signer.headersFor(target, PINNED), signer.headersFor(target, PINNED),
                "signing must be deterministic, or a retried request would be rejected");
    }

    @Test
    void aForeignHostIsNeverSigned() {
        LiveKitConfig config = configWith(ENDPOINT, "AKIAEXAMPLE", "secret-value");
        RecordingStorageSigner signer = new RecordingStorageSigner(config);

        assertTrue(signer.headersFor(URI.create("https://attacker.example.com/recordings-bucket/x"), PINNED).isEmpty(),
                "a foreign host must never receive a signature - and never see the credentials");
        assertTrue(signer.headersFor(URI.create("https://s3.example.test.evil.com/recordings-bucket/x"), PINNED).isEmpty(),
                "a suffix-spoofed host must not pass the allow-list");
        assertTrue(signer.headersFor(URI.create(ENDPOINT.replace(":9443", ":443") + "/recordings-bucket/x"), PINNED).isEmpty(),
                "a different port on the right host is still a different authority");
    }

    @Test
    void anAlreadyAuthorizedUrlIsLeftUntouched() {
        LiveKitConfig config = configWith(ENDPOINT, "AKIAEXAMPLE", "secret-value");
        Map<String, String> headers = new RecordingStorageSigner(config).headersFor(
                URI.create(ENDPOINT + "/recordings-bucket/x?X-Amz-Signature=deadbeef"), PINNED);

        assertTrue(headers.isEmpty(), "a URL carrying its own signature must not be re-signed");
    }

    @Test
    void withoutCredentialsNothingIsSigned() {
        LiveKitConfig noKeys = configWith(ENDPOINT, "", "");
        assertTrue(new RecordingStorageSigner(noKeys)
                        .headersFor(URI.create(ENDPOINT + "/recordings-bucket/x"), PINNED).isEmpty(),
                "with no egress credentials the request must go out unsigned, never half-signed");

        LiveKitConfig secretOnly = configWith(ENDPOINT, "AKIAEXAMPLE", "");
        assertTrue(new RecordingStorageSigner(secretOnly)
                        .headersFor(URI.create(ENDPOINT + "/recordings-bucket/x"), PINNED).isEmpty(),
                "an access key without a secret must not produce a signature");
    }

    @Test
    void aDefaultPortEndpointStillSignsItsOwnHost() {
        LiveKitConfig config = configWith("https://s3.example.test", "AKIAEXAMPLE", "secret-value");
        Map<String, String> headers = new RecordingStorageSigner(config)
                .headersFor(URI.create("https://s3.example.test/recordings-bucket/x"), PINNED);

        assertFalse(headers.isEmpty(), "an endpoint without an explicit port must still match its host");
        assertTrue(headers.get("Authorization").contains("SignedHeaders=host;x-amz-content-sha256;x-amz-date"));
    }

    @Test
    void aMissingRegionFallsBackToAutoRatherThanFailing() {
        LiveKitConfig config = configWith(ENDPOINT, "AKIAEXAMPLE", "secret-value");
        config.getEgress().setRegion("");
        Map<String, String> headers = new RecordingStorageSigner(config)
                .headersFor(URI.create(ENDPOINT + "/recordings-bucket/x"), PINNED);

        assertTrue(headers.get("Authorization").contains("/auto/s3/aws4_request"),
                "Backblaze accepts the literal region 'auto'; a blank region must not abort signing");
    }
}
