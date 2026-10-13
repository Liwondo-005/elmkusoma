package tz.elmkusoma.liveclass.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The trust boundary between "a string we persisted from an external service" and "a URL
 * this process will make an outbound request to".
 *
 * <p>Every rejection case here is a real attack the previous design was open to: a stored
 * location that points the proxy at cloud metadata, at an internal service, at a different
 * bucket on the same host, or out of the bucket prefix through {@code ..}. The endpoint and
 * bucket are configuration, not user input, which is exactly why they are the allow-list.</p>
 */
class RecordingStorageLocationTest {

    private static final String ENDPOINT = "https://s3.us-east-005.backblazeb2.com";
    private static final String BUCKET = "elmkusoma-live-recordings";

    private static Optional<String> resolve(String stored) {
        return RecordingStorageLocation.resolve(stored, ENDPOINT, BUCKET).map(RecordingStorageLocation::fetchUrl);
    }

    // ── accepted ────────────────────────────────────────────────────────────

    @Test
    void bareObjectKeyIsRebuiltFromTheConfiguredEndpointAndBucket() {
        assertEquals("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/c1/a.mp4",
                resolve("recordings/c1/a.mp4").orElseThrow());
    }

    @Test
    void s3UriWithTheConfiguredBucketIsAccepted() {
        assertEquals("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/c1/a.mp4",
                resolve("s3://elmkusoma-live-recordings/recordings/c1/a.mp4").orElseThrow());
    }

    @Test
    void anS3UriDeclaringADifferentBucketIsRejectedRatherThanReinterpreted() {
        // An s3:// URI names its bucket explicitly. Reading "recordings" as a key inside our
        // own bucket would quietly accept a location that addresses someone else's storage.
        assertFalse(resolve("s3://recordings/a.mp4").isPresent());
    }

    @Test
    void httpUrlOnTheConfiguredHostInsideTheConfiguredBucketIsAccepted() {
        assertEquals("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/a.mp4",
                resolve("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/a.mp4")
                        .orElseThrow());
    }

    @Test
    void hostComparisonIsCaseInsensitiveAndIgnoresTheDefaultPort() {
        assertEquals("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/a.mp4",
                resolve("https://S3.US-EAST-005.BACKBLAZEB2.COM:443/elmkusoma-live-recordings/a.mp4")
                        .orElseThrow());
    }

    @Test
    void aQueryStringIsPreservedBecauseSignatureMaterialLivesThere() {
        // LiveKit Cloud can hand back a pre-signed URL. Dropping the query would make an
        // otherwise valid recording unfetchable; the query is only ever sent back to the
        // same allow-listed host and is never returned to a client.
        assertEquals("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/a.mp4?X-Amz-Signature=abc",
                resolve("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/a.mp4?X-Amz-Signature=abc")
                        .orElseThrow());
    }

    @Test
    void anEndpointWithPathPrefixIsHonoured() {
        Optional<String> resolved = RecordingStorageLocation.resolve(
                "https://minio.internal.example.com/gateway/elmkusoma-live-recordings/a.mp4",
                "https://minio.internal.example.com/gateway/",
                BUCKET).map(RecordingStorageLocation::fetchUrl);
        assertEquals("https://minio.internal.example.com/gateway/elmkusoma-live-recordings/a.mp4",
                resolved.orElseThrow());
    }

    // ── rejected: SSRF / arbitrary host ─────────────────────────────────────

    @Test
    void aDifferentHostIsRejectedEvenWhenTheBucketMatches() {
        assertFalse(resolve("https://attacker.example.com/elmkusoma-live-recordings/a.mp4").isPresent(),
                "an attacker-controlled host must never become an outbound request target");
    }

    @Test
    void cloudMetadataAddressIsRejected() {
        assertFalse(resolve("http://169.254.169.254/latest/meta-data/").isPresent());
        assertFalse(resolve("http://metadata.google.internal/computeMetadata/v1/").isPresent());
    }

    @Test
    void anExplicitNonDefaultPortIsRejected() {
        assertFalse(resolve("https://s3.us-east-005.backblazeb2.com:8443/elmkusoma-live-recordings/a.mp4")
                .isPresent());
    }

    @Test
    void schemeDowngradeToPlainHttpIsRejected() {
        assertFalse(resolve("http://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/a.mp4")
                .isPresent());
    }

    @Test
    void embeddedCredentialsAreRejectedRatherThanStripped() {
        assertFalse(resolve("https://user:pass@s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/a.mp4")
                .isPresent());
    }

    @Test
    void aFragmentIsRejected() {
        assertFalse(resolve("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/a.mp4#x")
                .isPresent());
    }

    // ── rejected: arbitrary bucket ──────────────────────────────────────────

    @Test
    void aDifferentBucketOnTheSameHostIsRejected() {
        assertFalse(resolve("https://s3.us-east-005.backblazeb2.com/other-tenants/private.mp4").isPresent());
    }

    @Test
    void anS3UriNamingAnotherBucketIsRejected() {
        assertFalse(resolve("s3://other-tenants/private.mp4").isPresent());
    }

    // ── rejected: path traversal ────────────────────────────────────────────

    @Test
    void dotDotSegmentsAreRejectedInAKey() {
        assertFalse(resolve("recordings/../../secrets.env").isPresent());
        assertFalse(resolve("s3://elmkusoma-live-recordings/a/../../b.mp4").isPresent());
        assertFalse(resolve("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/a/../b.mp4")
                .isPresent());
    }

    @Test
    void aLeadingSlashKeyCannotRebaseTheUrl() {
        assertFalse(resolve("/elmkusoma-live-recordings/a.mp4").isPresent());
    }

    // ── rejected: other schemes and malformed input ─────────────────────────

    @Test
    void nonHttpSchemesAreRejected() {
        assertFalse(resolve("file:///etc/passwd").isPresent());
        assertFalse(resolve("ftp://s3.us-east-005.backblazeb2.com/a.mp4").isPresent());
        assertFalse(resolve("gcs://bucket/a.mp4").isPresent());
        assertFalse(resolve("ws://s3.us-east-005.backblazeb2.com/a.mp4").isPresent());
    }

    @Test
    void controlCharactersAndWhitespaceAreRejected() {
        // A raw newline in a stored key would otherwise be a header-injection primitive.
        assertFalse(resolve("recordings/a\nb.mp4").isPresent());
        assertFalse(resolve("recordings/a b.mp4").isPresent());
        assertFalse(resolve("recordings/a\r\nX-Evil: 1.mp4").isPresent());
    }

    @Test
    void blankMissingAndOverlongValuesAreRejected() {
        assertFalse(resolve(null).isPresent());
        assertFalse(resolve("").isPresent());
        assertFalse(resolve("   ").isPresent());
        assertFalse(resolve("a".repeat(4096)).isPresent());
    }

    @Test
    void nothingIsEverFabricatedWhenNoEndpointIsConfigured() {
        // Plain AWS S3 with no explicit endpoint: there is no allow-list to check against,
        // so the honest answer is "not configured" rather than an invented host.
        assertFalse(RecordingStorageLocation.resolve("s3://bucket/key", "", "bucket").isPresent());
        assertFalse(RecordingStorageLocation.resolve("recordings/a.mp4", null, "bucket").isPresent());
        assertFalse(RecordingStorageLocation.resolve(
                "https://s3.us-east-005.backblazeb2.com/bucket/a.mp4", "https://s3.us-east-005.backblazeb2.com", "")
                .isPresent());
    }

    @Test
    void anUnparseableEndpointFailsClosed() {
        assertFalse(RecordingStorageLocation.resolve("s3://bucket/key", "not a url", "bucket").isPresent());
    }

    // ── contract ────────────────────────────────────────────────────────────

    @Test
    void theFactoryNeverThrowsForArbitraryInput() {
        // The stored value can come from an external service, so malformed input must
        // degrade to "unavailable" rather than a 500 that leaks a stack trace.
        for (String candidate : new String[]{
                "s3://", "://", "http://", "https://", "%zz", "\u0000", "s3://\u0000/bucket/k",
                "https://[::1]/bucket/k", "https:///bucket/k"}) {
            RecordingStorageLocation.resolve(candidate, ENDPOINT, BUCKET);
        }
        assertTrue(true);
    }
}
