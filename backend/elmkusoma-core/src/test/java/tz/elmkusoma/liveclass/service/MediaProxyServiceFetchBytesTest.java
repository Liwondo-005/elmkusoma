package tz.elmkusoma.liveclass.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression: fetchBytes receives ALREADY percent-encoded signed URLs
 * (key=inst%2Fmedia%2Ffile.pdf). Passing such a URL to RestTemplate's String
 * overload re-runs URI-template expansion and double-encodes it
 * (%2F -> %252F), so the media service validates the signature over the
 * wrong object key and rejects the fetch — downloads failed with 500.
 */
class MediaProxyServiceFetchBytesTest {

    @Test
    void fetchBytes_preservesPercentEncodingOfSignedUrl() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        AtomicReference<String> receivedUri = new AtomicReference<>();
        byte[] payload = "stored-bytes".getBytes(StandardCharsets.UTF_8);
        server.createContext("/api/v1/media/local-content", exchange -> {
            receivedUri.set(exchange.getRequestURI().toString());
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort()
                    + "/api/v1/media/local-content"
                    + "?key=bbbb1111-1111-1111-1111-111111111101%2Fmedia%2Ffile.pdf"
                    + "&exp=1790767987"
                    + "&sig=WCY0HKZR2TNs9nCRvPV-la2PQJ9jgmjS6N0nMKELOhU";

            byte[] out = new MediaProxyService().fetchBytes(url);

            assertArrayEquals(payload, out);
            String received = receivedUri.get();
            assertNotNull(received, "request never reached the server");
            assertFalse(received.contains("%252F"),
                    "signed URL was double-encoded by URI-template expansion: " + received);
            assertTrue(received.contains("key=bbbb1111-1111-1111-1111-111111111101%2Fmedia%2Ffile.pdf"),
                    "object key altered in transit: " + received);
            assertTrue(received.contains("sig=WCY0HKZR2TNs9nCRvPV-la2PQJ9jgmjS6N0nMKELOhU"),
                    "signature altered in transit: " + received);
        } finally {
            server.stop(0);
        }
    }
}
