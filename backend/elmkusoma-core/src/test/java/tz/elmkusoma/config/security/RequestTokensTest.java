package tz.elmkusoma.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The narrow contract of the cookie fallback.
 *
 * <p>The whole point of this class is what it <em>refuses</em> to do. Cookie authentication
 * exists so a browser can load a video element on the recording playback paths; every other
 * endpoint must keep behaving exactly as it did, header-only, so nothing about the CSRF
 * posture of the API changes.</p>
 */
class RequestTokensTest {

    private static final String[] PREFIXES = {"/v1/replays/", "/v1/live-session/classes/"};
    private static final String HEADER_TOKEN = "header-token";
    private static final String COOKIE_TOKEN = "cookie-token";

    private static MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServletPath(uri);
        request.setContextPath("");
        return request;
    }

    private static MockHttpServletRequest withCookie(MockHttpServletRequest request, String value) {
        request.setCookies(new jakarta.servlet.http.Cookie(RequestTokens.ACCESS_TOKEN_COOKIE, value));
        return request;
    }

    @Test
    void theAuthorizationHeaderIsAlwaysPreferred() {
        MockHttpServletRequest request = request("GET", "/v1/replays/abc/playback");
        request.addHeader("Authorization", "Bearer " + HEADER_TOKEN);
        withCookie(request, COOKIE_TOKEN);

        assertEquals(HEADER_TOKEN, RequestTokens.extractToken(request, PREFIXES));
    }

    @Test
    void theCookieAuthenticatesAPlaybackGet() {
        MockHttpServletRequest request =
                withCookie(request("GET", "/v1/replays/6f1c6f8e-0000-0000-0000-000000000001/playback"),
                        COOKIE_TOKEN);

        assertEquals(COOKIE_TOKEN, RequestTokens.extractToken(request, PREFIXES));
    }

    @Test
    void theCookieAuthenticatesALiveClassRecordingPlaybackGet() {
        MockHttpServletRequest request = withCookie(
                request("GET", "/v1/live-session/classes/6f1c6f8e-0000-0000-0000-000000000002/recording/playback"),
                COOKIE_TOKEN);

        assertEquals(COOKIE_TOKEN, RequestTokens.extractToken(request, PREFIXES));
    }

    @Test
    void theCookieIsIgnoredOutsideThePlaybackPaths() {
        for (String uri : new String[]{
                "/v1/replays/abc",
                "/v1/replays",
                "/v1/learner/replays/abc/playback",
                "/v1/replays/abc/playback/extra",
                "/v1/live-session/classes/abc/recording/download",
                "/v1/auth/login",
                "/v1/media/abc/download"}) {
            MockHttpServletRequest request = withCookie(request("GET", uri), COOKIE_TOKEN);
            assertNull(RequestTokens.extractToken(request, PREFIXES),
                    "the cookie must not authenticate " + uri);
        }
    }

    @Test
    void theCookieIsIgnoredForAnythingThatIsNotARead() {
        for (String method : new String[]{"POST", "PUT", "DELETE", "PATCH", "OPTIONS"}) {
            MockHttpServletRequest request =
                    withCookie(request(method, "/v1/replays/abc/playback"), COOKIE_TOKEN);
            assertNull(RequestTokens.extractToken(request, PREFIXES),
                    "the cookie must not authenticate a " + method);
        }

        MockHttpServletRequest head =
                withCookie(request("HEAD", "/v1/replays/abc/playback"), COOKIE_TOKEN);
        assertEquals(COOKIE_TOKEN, RequestTokens.extractToken(head, PREFIXES),
                "HEAD is a read and is safe to allow");
    }

    @Test
    void aPathThatOnlyLooksLikePlaybackIsNotEnough() {
        // The suffix alone must not open the fallback; it also has to sit under a known prefix.
        MockHttpServletRequest request =
                withCookie(request("GET", "/v1/something/playback"), COOKIE_TOKEN);
        assertNull(RequestTokens.extractToken(request, PREFIXES));
    }

    @Test
    void anEmptyPrefixListDisablesTheFallbackEntirely() {
        MockHttpServletRequest request =
                withCookie(request("GET", "/v1/replays/abc/playback"), COOKIE_TOKEN);
        assertNull(RequestTokens.extractToken(request));

        request.addHeader("Authorization", "Bearer " + HEADER_TOKEN);
        assertEquals(HEADER_TOKEN, RequestTokens.extractToken(request));
    }

    @Test
    void aNonBearerAuthorizationHeaderDoesNotCount() {
        MockHttpServletRequest request = request("GET", "/v1/replays/abc/playback");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        withCookie(request, COOKIE_TOKEN);

        assertEquals(COOKIE_TOKEN, RequestTokens.extractToken(request, PREFIXES));
    }

    @Test
    void noCredentialsAtAllResolvesToNull() {
        assertNull(RequestTokens.extractToken(request("GET", "/v1/replays/abc/playback"), PREFIXES));
        assertNull(RequestTokens.extractToken(
                request("GET", "/v1/live-session/classes/abc/recording/playback"), PREFIXES));
    }

    @Test
    void aBlankCookieValueIsIgnored() {
        MockHttpServletRequest request =
                withCookie(request("GET", "/v1/replays/abc/playback"), "  ");
        assertNull(RequestTokens.extractToken(request, PREFIXES));
    }
}
