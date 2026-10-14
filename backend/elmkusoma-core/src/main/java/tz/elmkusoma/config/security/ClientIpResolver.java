package tz.elmkusoma.config.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Resolves the client address for rate limiting and abuse controls.
 *
 * <p>{@code X-Forwarded-For} is a client-controlled header. Trusting it unconditionally means any
 * caller can vary it per request and get a fresh rate-limit bucket every time, which defeats the
 * control entirely. The header is therefore honoured only when the direct socket peer is a
 * configured trusted proxy; otherwise the socket address is used, because that one cannot be forged
 * by the client.</p>
 *
 * <p>This is the same rule {@link SendCodeRateLimitFilter} already applies, extracted so the
 * certificate verifier and the other per-IP controls cannot drift into disagreeing about who the
 * caller is.</p>
 *
 * <p>Trusted proxies are configured as comma-separated address prefixes, e.g.
 * {@code elmkusoma.security.trusted-proxies=10.0.0.,172.16.}. Empty by default, which means the
 * header is ignored.</p>
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    /** Guards against a hostile header being used as a map key of unbounded length. */
    private static final int MAX_FORWARDED_LENGTH = 45;

    private volatile List<String> trustedProxyPrefixes = List.of();

    @Value("${elmkusoma.security.trusted-proxies:}")
    private String trustedProxies;

    @PostConstruct
    void initTrustedProxies() {
        if (trustedProxies == null || trustedProxies.isBlank()) {
            trustedProxyPrefixes = List.of();
        } else {
            trustedProxyPrefixes = Arrays.stream(trustedProxies.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        // Empty is the safe default and is logged so a deployment that intended to trust a proxy
        // but never configured it is visible in startup output.
        log.info("client IP resolution trusts X-Forwarded-For only from: {}",
                trustedProxyPrefixes.isEmpty() ? "<none configured>" : trustedProxyPrefixes);

        // Published for the static login-IP resolution path, which cannot receive an injected
        // dependency. Reflective to keep this class free of a compile-time dependency on the
        // identity module.
        publishToStaticBridge();
    }

    /**
     * Registers this resolver with the static bridge used by the login path.
     *
     * <p>Done reflectively and tolerantly: if that bridge is absent (for example in a slice test
     * that does not include the identity package) the application must still start rather than
     * fail on an unrelated concern.</p>
     */
    private void publishToStaticBridge() {
        try {
            Class<?> bridge = Class.forName("tz.elmkusoma.identity.service.ClientIpResolverHolder");
            bridge.getDeclaredMethod("register", ClientIpResolver.class).invoke(null, this);
        } catch (ClassNotFoundException e) {
            log.debug("static client-IP bridge not present; static callers will use the socket address");
        } catch (ReflectiveOperationException e) {
            log.warn("could not register the static client-IP bridge; static callers will use the socket address", e);
        }
    }

    /**
     * The address to key per-caller controls on.
     *
     * <p>Never returns null. When the forwarded header is present but unusable, or the peer is not a
     * trusted proxy, the socket address is returned instead.</p>
     */
    public String resolve(HttpServletRequest request) {
        String remote = request == null ? null : request.getRemoteAddr();
        if (remote == null || remote.isBlank() || !isTrustedProxy(remote)) {
            return remote == null || remote.isBlank() ? "unknown" : remote;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // The first hop is the original client; the rest is chain noise added by intermediaries.
            int comma = forwarded.indexOf(',');
            String first = (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            if (!first.isEmpty() && first.length() <= MAX_FORWARDED_LENGTH) {
                return first;
            }
        }
        return remote;
    }

    /** True when the direct peer is a proxy this deployment has been told to believe. */
    public boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        for (String prefix : trustedProxyPrefixes) {
            if (!prefix.isBlank() && remoteAddr.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }
}
