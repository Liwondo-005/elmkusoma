package tz.elmkusoma.liveclass.service;

import java.net.URI;
import java.util.Optional;

/**
 * Turns a <em>persisted</em> recording location into the single upstream URL the playback
 * proxy is allowed to fetch.
 *
 * <p>The recording location that reaches this class was written by {@code LiveKitService
 * .toPlayableRecordingUrl} from a LiveKit {@code FileInfo.location} value. That protocol
 * field is typed as a plain {@code string} with <em>no</em> documented scheme, so the
 * application must not assume it is an http URL, an {@code s3://} URI, or a bare object key
 * - and, more importantly, must not assume it is trustworthy. It originates from an
 * external service response and is stored for a long time.</p>
 *
 * <p>This class is therefore the security boundary between "a string we happened to store"
 * and "a URL this process will make an outbound HTTP request to". It fails closed:</p>
 *
 * <ul>
 *   <li><b>No client control.</b> The value always comes from the database, never from a
 *       request. A client can only ever address a replay or live-class id.</li>
 *   <li><b>The endpoint must be a real URL.</b> Configuration is parsed once up front, so an
 *       unparseable {@code livekit.egress.endpoint} can never be spliced into a fetch URL.</li>
 *   <li><b>Host allow-list.</b> An {@code http(s)} location is accepted only when its scheme,
 *       host and port match the <em>configured</em> egress endpoint exactly. A compromised or
 *       misbehaving LiveKit deployment cannot turn the application into an SSRF probe of
 *       {@code 169.254.169.254}, an internal service, or another customer's bucket.</li>
 *   <li><b>Bucket allow-list.</b> An {@code s3://} URI names its bucket explicitly, and that
 *       bucket must be the configured one. An http path must sit underneath
 *       {@code /<configuredBucket>/}. A bare key is simply prefixed with the configured
 *       bucket, so it can never address another one.</li>
 *   <li><b>No path traversal.</b> {@code ..} segments are rejected in the http path and in
 *       every key form, so a stored value can never escape the bucket prefix.</li>
 *   <li><b>No embedded credentials or fragments.</b> A {@code user:password@} authority or a
 *       {@code #fragment} rejects the value outright.</li>
 *   <li><b>No fabrication.</b> If nothing can be derived the answer is empty, and the caller
 *       reports "not configured" rather than inventing a host.</li>
 * </ul>
 *
 * <p>A query string on an accepted http(s) location <em>is</em> preserved and forwarded,
 * because S3-compatible providers put short-lived signature material there and dropping it
 * would make an otherwise valid recording unfetchable. It is only ever sent back to the
 * same allow-listed host and is never returned to a client, logged, or persisted.</p>
 *
 * <p>{@link #resolve(String, String, String)} returns empty for every rejected input; it
 * never throws, so a malformed stored value degrades to "recording unavailable" instead of
 * a 500 that leaks a stack trace.</p>
 */
public final class RecordingStorageLocation {

    /** Upper bound on a stored location; real S3 URLs and keys are far below this. */
    private static final int MAX_LOCATION_LENGTH = 2048;
    /** Upper bound on the object key portion. */
    private static final int MAX_KEY_LENGTH = 1024;
    /** Upper bound on a forwarded query string (signature material). */
    private static final int MAX_QUERY_LENGTH = 2048;

    private static final String SCHEME_S3 = "s3://";
    private static final String SCHEME_HTTP = "http://";
    private static final String SCHEME_HTTPS = "https://";
    /** {@link URI#getScheme()} returns the scheme without the {@code ://} separator. */
    private static final String NAME_HTTP = "http";
    private static final String NAME_HTTPS = "https";

    private final String fetchUrl;

    private RecordingStorageLocation(String fetchUrl) {
        this.fetchUrl = fetchUrl;
    }

    /** The single absolute http(s) URL this process is permitted to fetch. */
    public String fetchUrl() {
        return fetchUrl;
    }

    /**
     * @param storedLocation the persisted {@code recording_url} / {@code file_url} value
     * @param endpoint       the configured egress endpoint, e.g. {@code https://s3.us-east-005.backblazeb2.com}
     * @param bucket         the configured egress bucket
     * @return the validated fetch URL, or empty when the value is absent, malformed, or
     *         outside the configured endpoint/bucket
     */
    public static Optional<RecordingStorageLocation> resolve(String storedLocation,
                                                             String endpoint,
                                                             String bucket) {
        if (storedLocation == null) {
            return Optional.empty();
        }
        String value = storedLocation.trim();
        if (value.isEmpty() || value.length() > MAX_LOCATION_LENGTH) {
            return Optional.empty();
        }
        String safeBucket = bucket == null ? "" : bucket.trim();
        URI endpointUri = absoluteHttpUri(trimTrailingSlash(endpoint));
        if (safeBucket.isEmpty() || endpointUri == null) {
            // Nothing usable is configured, so nothing can be rebuilt safely. Never invent a host.
            return Optional.empty();
        }

        if (value.startsWith(SCHEME_HTTP) || value.startsWith(SCHEME_HTTPS)) {
            return fromHttpUrl(value, endpointUri, safeBucket);
        }
        if (value.startsWith(SCHEME_S3)) {
            return fromS3Uri(value.substring(SCHEME_S3.length()), endpointUri, safeBucket);
        }
        if (value.contains("://")) {
            // file:, ftp:, gcs:, ... - never fetchable by this application.
            return Optional.empty();
        }
        return fromBareKey(value, endpointUri, safeBucket);
    }

    // ------------------------------------------------------------------ http(s)

    private static Optional<RecordingStorageLocation> fromHttpUrl(String value,
                                                                  URI endpoint,
                                                                  String bucket) {
        URI actual;
        try {
            actual = URI.create(value);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        String authority = actual.getRawAuthority();
        if (authority == null || authority.isEmpty()) {
            return Optional.empty();
        }
        // Reject an embedded user:password@ authority outright rather than stripping it.
        if (authority.contains("@")) {
            return Optional.empty();
        }
        // A fragment is meaningless to a fetch and is a classic smuggling/desync vector.
        if (actual.getFragment() != null) {
            return Optional.empty();
        }
        String query = actual.getRawQuery();
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            return Optional.empty();
        }
        String host = authorityHost(authority);
        if (host == null || !host.equalsIgnoreCase(endpoint.getHost())) {
            return Optional.empty();
        }
        if (!actual.getScheme().equalsIgnoreCase(endpoint.getScheme())) {
            return Optional.empty();
        }
        if (effectivePort(actual) != effectivePort(endpoint)) {
            return Optional.empty();
        }

        String basePath = rawPath(endpoint);
        String prefix = basePath + "/" + bucket + "/";
        String path = actual.getRawPath() == null ? "" : actual.getRawPath();
        if (!path.startsWith(prefix)) {
            return Optional.empty();
        }
        String key = path.substring(prefix.length());
        if (!isSafeKey(key)) {
            return Optional.empty();
        }
        return Optional.of(new RecordingStorageLocation(rebuild(endpoint, bucket, key, query)));
    }

    // ------------------------------------------------------------------ s3://

    /**
     * An {@code s3://} URI names its bucket explicitly, so the declared bucket must be the
     * configured one. Treating {@code s3://other-tenant/private.mp4} as a plain key of this
     * bucket would quietly accept a location that addresses someone else's storage.
     */
    private static Optional<RecordingStorageLocation> fromS3Uri(String objectPath,
                                                                URI endpoint,
                                                                String bucket) {
        int slash = objectPath.indexOf('/');
        if (slash <= 0 || slash == objectPath.length() - 1) {
            return Optional.empty();
        }
        String declaredBucket = objectPath.substring(0, slash);
        if (!bucket.equals(declaredBucket)) {
            return Optional.empty();
        }
        String key = objectPath.substring(slash + 1);
        if (!isSafeKey(key)) {
            return Optional.empty();
        }
        return Optional.of(new RecordingStorageLocation(rebuild(endpoint, bucket, key, null)));
    }

    // ------------------------------------------------------------- bare keys

    private static Optional<RecordingStorageLocation> fromBareKey(String objectPath,
                                                                  URI endpoint,
                                                                  String bucket) {
        // Tolerate "bucket/key" as well as "key"; either way the key ends up inside the
        // configured bucket, so it can never address another one.
        String key = objectPath.startsWith(bucket + "/")
                ? objectPath.substring(bucket.length() + 1)
                : objectPath;
        if (!isSafeKey(key)) {
            return Optional.empty();
        }
        return Optional.of(new RecordingStorageLocation(rebuild(endpoint, bucket, key, null)));
    }

    // ------------------------------------------------------------------ helpers

    private static String rebuild(URI endpoint, String bucket, String key, String query) {
        StringBuilder fetch = new StringBuilder(key.length() + bucket.length() + 80);
        fetch.append(endpoint.getScheme()).append("://").append(endpoint.getHost());
        int port = endpoint.getPort();
        // Only ever write an explicit port when it is not the scheme default, so a stored
        // URL and a configured endpoint compare equal however each chose to spell it.
        if (port != -1 && port != defaultPort(endpoint.getScheme())) {
            fetch.append(':').append(port);
        }
        fetch.append(rawPath(endpoint)).append('/').append(bucket).append('/').append(key);
        if (query != null) {
            fetch.append('?').append(query);
        }
        return fetch.toString();
    }

    /** Parses an absolute {@code http}/{@code https} URL, or returns null. */
    private static URI absoluteHttpUri(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null
                || (!NAME_HTTP.equalsIgnoreCase(scheme) && !NAME_HTTPS.equalsIgnoreCase(scheme))) {
            return null;
        }
        return uri.getHost() == null || uri.getHost().isEmpty() ? null : uri;
    }

    /**
     * {@link URI#getHost()} is null for any authority it does not consider a valid hostname.
     * The host is derived from the raw authority instead, which is what the allow-list is
     * actually comparing.
     */
    private static String authorityHost(String authority) {
        int colon = authority.lastIndexOf(':');
        if (colon > -1 && authority.indexOf(']') < colon) {
            // Strip a :port, but never the port-looking colon inside an IPv6 literal.
            authority = authority.substring(0, colon);
        }
        if (authority.startsWith("[") && authority.endsWith("]")) {
            authority = authority.substring(1, authority.length() - 1);
        }
        return authority.isEmpty() ? null : authority;
    }

    private static String rawPath(URI uri) {
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * An object key is accepted only if it is a plausible S3 key and cannot climb out of the
     * bucket prefix once appended to a URL.
     */
    private static boolean isSafeKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            return false;
        }
        if (key.startsWith("/")) {
            return false;
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            // Control characters and any whitespace. A raw newline in a stored key would
            // otherwise be a header-injection primitive once the URL is handed to
            // HttpURLConnection.
            if (c <= 0x20 || c == 0x7f) {
                return false;
            }
        }
        for (String segment : key.split("/", -1)) {
            if ("..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() != -1 ? uri.getPort() : defaultPort(uri.getScheme());
    }

    private static int defaultPort(String scheme) {
        if (NAME_HTTPS.equalsIgnoreCase(scheme)) {
            return 443;
        }
        if (NAME_HTTP.equalsIgnoreCase(scheme)) {
            return 80;
        }
        return -1;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
