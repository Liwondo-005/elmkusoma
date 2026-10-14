package tz.elmkusoma.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tz.elmkusoma.config.security.ClientIpResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * One rule for "who is calling", applied everywhere a per-IP control depends on it.
 *
 * <p>{@code X-Forwarded-For} is chosen by the caller. Trusting it unconditionally means a caller
 * can pick a new bucket per request, which silently disables the control. The header is therefore
 * only believed when the direct socket peer is a proxy the deployment has explicitly listed.</p>
 */
class ClientIpResolverTest {

    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientIpResolver();
    }

    private void configureTrustedProxies(String csv) {
        ReflectionTestUtils.setField(resolver, "trustedProxies", csv);
        ReflectionTestUtils.invokeMethod(resolver, "initTrustedProxies");
    }

    private HttpServletRequest request(String remoteAddr, String forwardedFor) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        if (forwardedFor != null) {
            when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
        }
        return request;
    }

    @Test
    @DisplayName("with no trusted proxies configured, a spoofed forwarded header is ignored")
    void spoofedHeaderIsIgnoredByDefault() {
        configureTrustedProxies("");

        assertEquals("9.9.9.9", resolver.resolve(request("9.9.9.9", "1.2.3.4")));
    }

    @Test
    @DisplayName("varying the header per request cannot escape the socket-address bucket")
    void varyingTheHeaderDoesNotProduceNewBuckets() {
        configureTrustedProxies("");

        // This is the attack: same real client, different claimed identity each time.
        assertEquals("9.9.9.9", resolver.resolve(request("9.9.9.9", "1.1.1.1")));
        assertEquals("9.9.9.9", resolver.resolve(request("9.9.9.9", "2.2.2.2")));
        assertEquals("9.9.9.9", resolver.resolve(request("9.9.9.9", "3.3.3.3")));
    }

    @Test
    @DisplayName("behind a configured proxy, the original client is recovered")
    void trustedProxyHeaderIsHonoured() {
        configureTrustedProxies("10.0.0.");

        // Remote is the proxy; the first forwarded hop is the real client.
        assertEquals("1.2.3.4", resolver.resolve(request("10.0.0.7", "1.2.3.4, 10.0.0.7")));
    }

    @Test
    @DisplayName("a trusted proxy address that sends no header falls back to the socket")
    void trustedProxyWithoutHeaderFallsBack() {
        configureTrustedProxies("10.0.0.");

        assertEquals("10.0.0.7", resolver.resolve(request("10.0.0.7", null)));
        assertEquals("10.0.0.7", resolver.resolve(request("10.0.0.7", "   ")));
    }

    @Test
    @DisplayName("an untrusted peer is refused even when the header looks like a real client")
    void untrustedPeerCannotClaimAClientIdentity() {
        configureTrustedProxies("10.0.0.");

        assertEquals("203.0.113.5", resolver.resolve(request("203.0.113.5", "1.2.3.4")));
    }

    @Test
    @DisplayName("an implausibly long header is rejected rather than used as a key")
    void oversizedHeaderIsRejected() {
        configureTrustedProxies("10.0.0.");

        String huge = "x".repeat(200);
        assertEquals("10.0.0.7", resolver.resolve(request("10.0.0.7", huge)));
    }

    @Test
    @DisplayName("multiple trusted proxy prefixes are all honoured")
    void multipleProxyPrefixes() {
        configureTrustedProxies("10.0.0., 172.16.");

        assertEquals("1.2.3.4", resolver.resolve(request("10.0.0.7", "1.2.3.4")));
        assertEquals("5.6.7.8", resolver.resolve(request("172.16.4.1", "5.6.7.8")));
        assertEquals("203.0.113.5", resolver.resolve(request("203.0.113.5", "1.2.3.4")));
    }

    @Test
    @DisplayName("resolution never returns null")
    void neverReturnsNull() {
        configureTrustedProxies("");

        assertEquals("unknown", resolver.resolve(request(null, null)));
        assertEquals("unknown", resolver.resolve(request("  ", "1.2.3.4")));
        assertEquals("unknown", resolver.resolve(null));
    }

    @Test
    @DisplayName("proxy trust is prefix based and case sensitive on the address")
    void isTrustedProxySemantics() {
        configureTrustedProxies("10.0.0.");

        assertTrue(resolver.isTrustedProxy("10.0.0.1"));
        assertTrue(resolver.isTrustedProxy("10.0.0.254"));
        assertFalse(resolver.isTrustedProxy("10.0.1.1"));
        assertFalse(resolver.isTrustedProxy("110.0.0.1"));
        assertFalse(resolver.isTrustedProxy(null));
        assertFalse(resolver.isTrustedProxy("  "));
    }
}
