package tz.elmkusoma.liveclass.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Streams a recording from private object storage on behalf of an already-authorised caller.
 *
 * <p>Core deliberately owns no S3/B2 client, and this class does not add one. The recording
 * location persisted by the publication pipeline is always an http(s) URL rebuilt from the
 * configured egress endpoint, so a plain HTTP GET is sufficient - which is also what
 * {@link MediaProxyService} already does for media-library resources. Keeping the fetch on
 * HTTP means no second storage abstraction, no second credential store, and no bucket ever
 * becomes public.</p>
 *
 * <p>What this class guarantees:</p>
 * <ul>
 *   <li>The URL is never supplied by a client. Callers pass a persisted location, which is
 *       re-validated through {@link RecordingStorageLocation} on every single request.</li>
 *   <li>Redirects are disabled, so a hostile or rotated storage URL can never bounce the
 *       request to a host outside the allow-list.</li>
 *   <li>Connect and read timeouts bound how long a single request can hold a request thread
 *       and an upstream connection.</li>
 *   <li>Nothing is buffered. The upstream {@code InputStream} is piped straight into the
 *       servlet response, so an hour-long lesson costs a fixed amount of memory regardless
 *       of file size.</li>
 *   <li>{@code Accept-Encoding: identity} is forced, so byte offsets in a {@code Range}
 *       header stay meaningful and seeking works against a compressed transfer.</li>
 *   <li>Nothing is logged that could contain the object key, a signed query string, or an
 *       upstream message.</li>
 * </ul>
 */
@Service
@Slf4j
public class RecordingPlaybackService {

    /** Single-range only. Multi-range would need multipart/byteranges handling upstream. */
    private static final Pattern SINGLE_BYTE_RANGE =
            Pattern.compile("bytes=\\d*-\\d*", Pattern.CASE_INSENSITIVE);

    private static final List<String> FORWARDED_HEADERS = List.of(
            "Content-Type", "Content-Length", "Content-Range", "Accept-Ranges",
            "Last-Modified", "ETag");

    private final LiveKitConfig liveKitConfig;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public RecordingPlaybackService(LiveKitConfig liveKitConfig,
                                    @Value("${app.recording-playback.connect-timeout-ms:5000}") int connectTimeoutMs,
                                    @Value("${app.recording-playback.read-timeout-ms:60000}") int readTimeoutMs) {
        this.liveKitConfig = liveKitConfig;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * Re-validates a persisted recording location and returns the URL this process may fetch.
     *
     * @return empty when the location is blank, untrusted, or the egress endpoint/bucket is
     *         not configured - the caller maps that to a 404 or 503 without revealing why
     */
    public Optional<String> resolveFetchUrl(String storedLocation) {
        return RecordingStorageLocation.resolve(
                        storedLocation,
                        liveKitConfig.getEgress().getEndpoint(),
                        liveKitConfig.getEgress().getOutputBucket())
                .map(RecordingStorageLocation::fetchUrl);
    }

    /**
     * Proxies the bytes of an already-authorised recording, honouring HTTP Range so that
     * {@code <video>} seeking and resumption work.
     *
     * @param fetchUrl     a URL previously produced by {@link #resolveFetchUrl(String)}
     * @param rangeHeader  the caller's {@code Range} header, or null
     * @return the streaming response, or empty when storage is unreachable or answered with
     *         an error status - the caller maps that to a 502 with no upstream detail
     */
    public Optional<ResponseEntity<Resource>> stream(String fetchUrl, String rangeHeader) {
        HttpURLConnection connection = null;
        try {
            URI uri = new URI(fetchUrl);
            // Explicitly host/port based rather than the raw string, so a stored value can
            // never smuggle a different authority through URL parsing quirks.
            String file = uri.getRawPath() == null ? "" : uri.getRawPath();
            if (uri.getRawQuery() != null) {
                file = file + "?" + uri.getRawQuery();
            }
            URL url = new URL(uri.getScheme(), uri.getHost(), uri.getPort(), file);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding", "identity");
            if (isSingleRange(rangeHeader)) {
                connection.setRequestProperty("Range", rangeHeader.trim());
            }

            int status = connection.getResponseCode();
            // Redirects are disabled above, so a 3xx reaching here would otherwise be
            // replayed to the browser - an open redirect straight out of an authenticated
            // endpoint. Anything outside 2xx is treated as an upstream failure.
            if (status < 200 || status >= 300) {
                log.warn("Recording playback upstream rejected the request (status={})", status);
                connection.disconnect();
                return Optional.empty();
            }

            InputStream body = new ConnectionClosingInputStream(connection);
            HttpHeaders headers = new HttpHeaders();
            Map<String, List<String>> upstream = connection.getHeaderFields();
            // Matched case-insensitively on purpose: S3-compatible providers (B2 included)
            // emit lower-case header names, and a case-sensitive lookup would silently drop
            // Content-Type and Content-Range and break video seeking.
            if (upstream != null) {
                for (Map.Entry<String, List<String>> entry : upstream.entrySet()) {
                    String name = entry.getKey();
                    if (name == null || entry.getValue() == null) {
                        continue;
                    }
                    for (String forwarded : FORWARDED_HEADERS) {
                        if (forwarded.equalsIgnoreCase(name)) {
                            for (String value : entry.getValue()) {
                                headers.add(forwarded, value);
                            }
                            break;
                        }
                    }
                }
            }
            // Recordings are entitlement-gated and personalised; they must never land in a
            // shared cache, and the object key must never be inferred from a sniffer probe.
            headers.set(HttpHeaders.CACHE_CONTROL, "private, no-store");
            headers.set("X-Content-Type-Options", "nosniff");

            return Optional.of(ResponseEntity.status(status)
                    .headers(headers)
                    .body(new InputStreamResource(body)));
        } catch (Exception ex) {
            // The message can embed the URL (and therefore the object key and any signed
            // query), so only the exception type is recorded.
            log.warn("Recording playback failed: {}", ex.getClass().getSimpleName());
            if (connection != null) {
                connection.disconnect();
            }
            return Optional.empty();
        }
    }

    /**
     * Accepts only a single {@code bytes=a-b}, {@code bytes=a-} or {@code bytes=-b} range.
     * Anything else is dropped and the full object is served, which is always safe.
     */
    private static boolean isSingleRange(String rangeHeader) {
        if (rangeHeader == null) {
            return false;
        }
        String trimmed = rangeHeader.trim();
        return trimmed.length() <= 64 && SINGLE_BYTE_RANGE.matcher(trimmed).matches();
    }

    /**
     * Closes the underlying HTTP connection when the stream is closed, so a client that
     * aborts playback mid-file releases the upstream socket instead of leaking it.
     */
    private static final class ConnectionClosingInputStream extends FilterInputStream {

        private final HttpURLConnection connection;

        private ConnectionClosingInputStream(HttpURLConnection connection) throws IOException {
            super(connection.getInputStream());
            this.connection = connection;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                connection.disconnect();
            }
        }
    }
}
