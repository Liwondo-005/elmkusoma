package tz.elmkusoma.liveclass.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The recording byte path, exercised against a real local HTTP server.
 *
 * <p>No B2 credentials are needed here and none are simulated: the proxy speaks plain HTTP
 * to whatever host the configured endpoint names, so an in-process {@link HttpServer} gives
 * genuine status codes, genuine {@code Range} semantics and genuine streaming. Nothing in
 * this class proves anything about Backblaze B2 reachability, key permissions or path-style
 * addressing - that still requires a real recording and a real bucket.</p>
 */
class RecordingPlaybackServiceTest {

    private static final String BUCKET = "test-recordings";
    private static final String OBJECT = "recordings/class-1/lesson.mp4";
    private static final byte[] CONTENT = "0123456789abcdefghij".getBytes(StandardCharsets.UTF_8);

    private static HttpServer server;
    private static String endpoint;
    private static RecordingPlaybackService service;

    @BeforeAll
    static void startStorage() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/redirect/" + BUCKET + "/" + OBJECT)) {
                exchange.getResponseHeaders().add("Location", "https://attacker.example.com/steal");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }
            if (!path.equals("/" + BUCKET + "/" + OBJECT)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            if ("forbidden".equals(exchange.getRequestURI().getQuery())) {
                exchange.sendResponseHeaders(403, -1);
                exchange.close();
                return;
            }
            exchange.getResponseHeaders().add("Content-Type", "video/mp4");
            String range = exchange.getRequestHeaders().getFirst("Range");
            if (range != null && range.matches("bytes=\\d*-\\d*")) {
                int start = 0;
                int end = CONTENT.length - 1;
                if (range.startsWith("bytes=-")) {
                    start = CONTENT.length - Integer.parseInt(range.substring("bytes=-".length()));
                } else {
                    String[] bounds = range.substring("bytes=".length()).split("-", -1);
                    start = Integer.parseInt(bounds[0]);
                    if (!bounds[1].isBlank()) {
                        end = Integer.parseInt(bounds[1]);
                    }
                }
                end = Math.min(end, CONTENT.length - 1);
                if (start > end || start >= CONTENT.length) {
                    exchange.getResponseHeaders().add("Content-Range", "bytes */" + CONTENT.length);
                    exchange.sendResponseHeaders(416, -1);
                    exchange.close();
                    return;
                }
                byte[] slice = java.util.Arrays.copyOfRange(CONTENT, start, end + 1);
                exchange.getResponseHeaders().add("Content-Range",
                        "bytes " + start + "-" + end + "/" + CONTENT.length);
                exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
                exchange.sendResponseHeaders(206, slice.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(slice);
                }
                return;
            }
            exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
            exchange.sendResponseHeaders(200, CONTENT.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(CONTENT);
            }
        });
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort();

        LiveKitConfig config = new LiveKitConfig();
        config.getEgress().setEndpoint(endpoint);
        config.getEgress().setOutputBucket(BUCKET);
        service = new RecordingPlaybackService(config, new RecordingStorageSigner(config), 2000, 5000);
    }

    @AfterAll
    static void stopStorage() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void reset() {
        // No per-test state; the server is deterministic.
    }

    private static byte[] body(ResponseEntity<Resource> response) throws IOException {
        try (var in = response.getBody().getInputStream()) {
            return in.readAllBytes();
        }
    }

    @Test
    void aBareObjectKeyIsRebuiltAgainstTheConfiguredEndpointAndBucket() {
        Optional<String> url = service.resolveFetchUrl(OBJECT);
        assertEquals(endpoint + "/" + BUCKET + "/" + OBJECT, url.orElseThrow());
    }

    @Test
    void aFullFetchStreamsTheWholeObject() throws Exception {
        ResponseEntity<Resource> response = service.stream(
                service.resolveFetchUrl(OBJECT).orElseThrow(), null).orElseThrow();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("video/mp4", response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        assertEquals(String.valueOf(CONTENT.length), response.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH));
        assertEquals("private, no-store", response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals(new String(CONTENT, StandardCharsets.UTF_8), new String(body(response), StandardCharsets.UTF_8));
    }

    @Test
    void aRangeRequestReturns206WithTheRequestedSlice() throws Exception {
        ResponseEntity<Resource> response = service.stream(
                service.resolveFetchUrl(OBJECT).orElseThrow(), "bytes=0-9").orElseThrow();

        assertEquals(206, response.getStatusCode().value());
        assertEquals("bytes 0-9/" + CONTENT.length, response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE));
        assertEquals("0123456789", new String(body(response), StandardCharsets.UTF_8));
    }

    @Test
    void anOpenEndedRangeAndASuffixRangeBothWork() throws Exception {
        ResponseEntity<Resource> openEnded = service.stream(
                service.resolveFetchUrl(OBJECT).orElseThrow(), "bytes=10-").orElseThrow();
        assertEquals("abcdefghij", new String(body(openEnded), StandardCharsets.UTF_8));

        ResponseEntity<Resource> suffix = service.stream(
                service.resolveFetchUrl(OBJECT).orElseThrow(), "bytes=-5").orElseThrow();
        assertEquals("ghij".length() + 1, body(suffix).length);
    }

    @Test
    void aMalformedOrMultiRangeIsDroppedAndTheWholeObjectIsServed() throws Exception {
        // Note: "bytes=99999-" is a *valid* range that this object cannot satisfy, so the
        // storage server rightly answers 416 and the proxy reports an upstream failure. Only
        // syntactically invalid ranges are silently dropped here.
        for (String bad : new String[]{"bytes=abc", "bytes=0-9,20-29", "items=0-9", "nonsense", "bytes="}) {
            ResponseEntity<Resource> response = service.stream(
                    service.resolveFetchUrl(OBJECT).orElseThrow(), bad).orElseThrow();
            assertEquals(200, response.getStatusCode().value(), "range '" + bad + "' must be dropped");
            assertEquals(CONTENT.length, body(response).length, "range '" + bad + "' must be dropped");
        }
    }

    @Test
    void anUpstreamErrorStatusBecomesAnEmptyResultWithNoDetailLeaked() {
        Optional<ResponseEntity<Resource>> response = service.stream(
                service.resolveFetchUrl(OBJECT).orElseThrow() + "?forbidden", null);
        assertTrue(response.isEmpty(), "an upstream 403 must not be replayed to the client");
    }

    @Test
    void aRedirectIsNeverFollowedAndNeverReplayedToTheClient() {
        // Following it would turn an authenticated endpoint into an open redirect; replaying
        // it would hand the browser an attacker-chosen location.
        Optional<ResponseEntity<Resource>> response = service.stream(
                endpoint + "/redirect/" + BUCKET + "/" + OBJECT, null);
        assertTrue(response.isEmpty(), "a 3xx from storage must be treated as a failure");
    }

    @Test
    void aLocationOutsideTheAllowListIsNeverFetched() {
        assertTrue(service.resolveFetchUrl("https://attacker.example.com/" + BUCKET + "/" + OBJECT).isEmpty());
        assertTrue(service.resolveFetchUrl("https://s3.us-east-005.backblazeb2.com/" + BUCKET + "/" + OBJECT)
                .isEmpty());
        assertTrue(service.resolveFetchUrl("file:///etc/passwd").isEmpty());
        assertTrue(service.resolveFetchUrl(null).isEmpty());
    }

    @Test
    void aLocationThatWouldTraverseOutOfTheBucketIsNeverFetched() {
        assertTrue(service.resolveFetchUrl(endpoint + "/" + BUCKET + "/../etc/passwd").isEmpty());
        assertTrue(service.resolveFetchUrl("recordings/../../etc/passwd").isEmpty());
    }
}
