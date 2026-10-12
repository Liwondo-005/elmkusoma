package tz.elmkusoma.config.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Extracts the caller's access token from a request.
 *
 * <p>Both JWT filters historically accepted only the {@code Authorization: Bearer} header.
 * Browsers do not attach an {@code Authorization} header to sub-resource requests such as
 * {@code <video src=...>}, {@code <img>} or a plain link navigation, which made an
 * authenticated recording byte path impossible to express: the entitlement check would have
 * had to be skipped precisely on the request that carries the protected bytes.</p>
 *
 * <p>The frontend has always mirrored the access token into an {@code elmkusoma_access_token}
 * cookie ({@code path=/}, {@code SameSite=Lax}) alongside local storage. This class lets the
 * filters fall back to that cookie <em>and only on the recording playback paths</em>.</p>
 *
 * <p>The scoping is deliberate and is the reason this is not a blanket change to cookie
 * authentication:</p>
 * <ul>
 *   <li>Every other endpoint keeps its exact existing behaviour - header-only. Nothing about
 *       the authentication of a state-changing request changes, so the CSRF posture of the
 *       whole API is untouched.</li>
 *   <li>The playback endpoints are read-only. A cross-site attacker cannot use a GET that
 *       streams video to change any state, and {@code SameSite=Lax} already stops the cookie
 *       from being attached to a cross-site request in the first place.</li>
 *   <li>It is the <em>same</em> token, validated by the same {@link JwtTokenProvider}, with
 *       the same signature, expiry and security-version checks as the header form. No weaker
 *       credential, no new signing key, no second expiry policy.</li>
 * </ul>
 */
final class RequestTokens {

    static final String ACCESS_TOKEN_COOKIE = "elmkusoma_access_token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private RequestTokens() {
    }

    static String extractToken(HttpServletRequest request) {
        return fromAuthorizationHeader(request);
    }

    /**
     * @param playbackPathPrefixes exact path prefixes that may authenticate from the cookie;
     *                             an empty array disables the fallback entirely
     */
    static String extractToken(HttpServletRequest request, String... playbackPathPrefixes) {
        String token = fromAuthorizationHeader(request);
        if (StringUtils.hasText(token)) {
            return token;
        }
        if (playbackPathPrefixes.length == 0
                || !isReadOnlyGet(request)
                || !isPlaybackPath(request, playbackPathPrefixes)) {
            return null;
        }
        return fromCookie(request);
    }

    /**
     * The fallback exists so a browser can load a video element, which is always a read.
     * Restricting it to GET/HEAD means no state-changing verb anywhere in the application can
     * ever be reached through this path, whatever a future mapping looks like.
     */
    private static boolean isReadOnlyGet(HttpServletRequest request) {
        String method = request.getMethod();
        return "GET".equals(method) || "HEAD".equals(method);
    }

    private static String fromAuthorizationHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private static String fromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())
                    && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Matches a request path against the playback prefixes. A suffix of
     * {@code /playback} is required so that a lookalike path can never widen the fallback,
     * and the comparison is done per segment boundary.
     */
    private static boolean isPlaybackPath(HttpServletRequest request, String... prefixes) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        if (!path.endsWith("/playback")) {
            return false;
        }
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
