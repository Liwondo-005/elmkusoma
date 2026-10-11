package tz.elmkusoma.liveclass.service;

import org.junit.jupiter.api.Test;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Recording-location handling.
 *
 * <p>The LiveKit protocol types {@code FileInfo.location} as a plain {@code string} and
 * documents no scheme at all: it does not promise an http URL, an {@code s3://} URI, or
 * anything else. That value is produced by the LiveKit server for the storage provider in
 * use, so a handler that only accepts {@code http} is making an unverified assumption - and
 * when it is wrong it fails closed and silently loses the recording.</p>
 *
 * <p>These tests pin the contract the application relies on instead: anything already usable
 * is passed through untouched, anything else is rebuilt from the <em>configured</em> endpoint
 * and bucket, and if no URL can be derived the answer is null so nothing is ever fabricated.</p>
 */
class LiveKitRecordingLocationTest {

    private static final String BUCKET = "elmkusoma-live-recordings";
    private static final String ENDPOINT = "https://s3.us-east-005.backblazeb2.com";

    private LiveKitService service(String bucket, String endpoint) {
        LiveKitConfig c = new LiveKitConfig();
        c.getServer().setApiKey("devkey");
        c.getServer().setApiSecret("devsecret-devsecret-devsecret-dev01!");
        c.getEgress().setOutputBucket(bucket);
        c.getEgress().setEndpoint(endpoint);
        return new LiveKitService(c);
    }

    private LiveKitService service() {
        return service(BUCKET, ENDPOINT);
    }

    // ---- already-usable locations are never rewritten ----------------------------

    @Test
    void httpsLocationIsPassedThroughUnchanged() {
        String url = "https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/a.mp4";
        assertEquals(url, service().toPlayableRecordingUrl(url));
    }

    @Test
    void httpLocationIsPassedThroughUnchanged() {
        String url = "http://localhost:9000/bucket/a.mp4";
        assertEquals(url, service().toPlayableRecordingUrl(url));
    }

    @Test
    void surroundingWhitespaceIsIgnored() {
        assertEquals("https://host/a.mp4", service().toPlayableRecordingUrl("  https://host/a.mp4  "));
    }

    // ---- non-http locations are rebuilt from configured storage -------------------

    @Test
    void s3UriIsRebuiltFromConfiguredEndpointAndBucket() {
        assertEquals(
                ENDPOINT + "/" + BUCKET + "/recordings/cls-1.mp4",
                service().toPlayableRecordingUrl("s3://" + BUCKET + "/recordings/cls-1.mp4"));
    }

    @Test
    void bareObjectKeyIsPrefixedWithTheBucket() {
        assertEquals(
                ENDPOINT + "/" + BUCKET + "/recordings/cls-1.mp4",
                service().toPlayableRecordingUrl("recordings/cls-1.mp4"));
    }

    @Test
    void objectKeyThatAlreadyCarriesTheBucketIsNotPrefixedTwice() {
        assertEquals(
                ENDPOINT + "/" + BUCKET + "/recordings/cls-1.mp4",
                service().toPlayableRecordingUrl(BUCKET + "/recordings/cls-1.mp4"));
    }

    @Test
    void trailingSlashOnTheEndpointIsNotDoubled() {
        assertEquals(
                ENDPOINT + "/" + BUCKET + "/recordings/cls-1.mp4",
                service(BUCKET, ENDPOINT + "/").toPlayableRecordingUrl("s3://" + BUCKET + "/recordings/cls-1.mp4"));
    }

    // ---- nothing usable is invented -----------------------------------------------

    @Test
    void nullAndBlankLocationsResolveToNull() {
        assertNull(service().toPlayableRecordingUrl(null));
        assertNull(service().toPlayableRecordingUrl(""));
        assertNull(service().toPlayableRecordingUrl("   "));
    }

    @Test
    void nonHttpLocationWithoutAConfiguredEndpointResolvesToNullRatherThanInventingAUrl() {
        // Plain AWS S3 (no custom endpoint): there is no host to rebuild against, so the
        // only safe answer is "cannot resolve". Inventing https://<key>.s3.amazonaws.com
        // would be a guess about a scheme the protocol never promised.
        assertNull(service(BUCKET, "").toPlayableRecordingUrl("s3://" + BUCKET + "/recordings/cls-1.mp4"));
    }

    @Test
    void nonHttpLocationWithoutAConfiguredBucketResolvesToNullRatherThanInventingAUrl() {
        assertNull(service("", ENDPOINT).toPlayableRecordingUrl("s3://some-bucket/key.mp4"));
    }

    @Test
    void bucketPrefixMatchingIsNotNaiveAboutPartialNames() {
        // A key that merely starts with the bucket's characters must still be prefixed,
        // otherwise the two would be concatenated into a single wrong path.
        assertEquals(
                ENDPOINT + "/rec/ordering/x.mp4",
                service("rec", ENDPOINT).toPlayableRecordingUrl("ordering/x.mp4"));
    }
}
