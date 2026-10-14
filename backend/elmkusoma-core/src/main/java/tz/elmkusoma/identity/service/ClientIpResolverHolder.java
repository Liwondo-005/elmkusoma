package tz.elmkusoma.identity.service;

import tz.elmkusoma.config.security.ClientIpResolver;

/**
 * Static bridge to the Spring-managed {@link ClientIpResolver}.
 *
 * <p>{@code LoginAttemptService.resolveClientIp} is called from a static resolution path in
 * {@code AuthServiceImpl} that runs before any request-scoped context exists, so it cannot simply
 * take an injected dependency. The bean publishes its instance here on construction so that path
 * still honours the deployment's trusted-proxy configuration instead of trusting a client-supplied
 * header.</p>
 *
 * <p>If the bean is absent (unit tests that construct the service directly, or a context that has
 * not created it yet), resolution falls back to the socket address, which a client cannot forge.
 * Failing closed toward the unspoofable value is the safe direction; the per-account limit is
 * unaffected either way.</p>
 */
final class ClientIpResolverHolder {

    private static volatile ClientIpResolver instance;

    private ClientIpResolverHolder() {
    }

    static void register(ClientIpResolver resolver) {
        instance = resolver;
    }

    static String resolve(jakarta.servlet.http.HttpServletRequest request) {
        ClientIpResolver resolver = instance;
        if (resolver != null) {
            return resolver.resolve(request);
        }
        String remote = request.getRemoteAddr();
        return remote != null && !remote.isBlank() ? remote : "unknown";
    }
}
