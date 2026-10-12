package tz.elmkusoma.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Fails startup when the configured public frontend base URL could not produce a usable
 * account-recovery link.
 *
 * <p>{@code app.frontend-url} is concatenated with {@code /reset-password?token=…},
 * {@code /verify-email?token=…} and {@code /login} by {@code AuthServiceImpl} and
 * {@code PlatformAdminService}. A malformed or missing base URL does not fail while the mail
 * is being built - it produces a link the user cannot open, so it is caught here instead.</p>
 *
 * <p>Under the {@code prod} profile a loopback host is rejected outright: production must
 * never silently emit {@code http://localhost:3000} recovery links. Local development and
 * tests keep that default.</p>
 *
 * <p>Messages name the setting only; no other configuration value is ever echoed.</p>
 */
@Component
public class FrontendUrlValidator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(FrontendUrlValidator.class);

    /** Hosts that cannot receive an emailed link from outside the machine. */
    private static final Set<String> LOOPBACK_HOSTS =
            Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1", "[::1]");

    private final String frontendUrl;
    private final boolean production;

    public FrontendUrlValidator(@Value("${app.frontend-url:http://localhost:3000}") String frontendUrl,
                                Environment environment) {
        this.frontendUrl = frontendUrl;
        this.production = environment != null
                && environment.acceptsProfiles(Profiles.of("prod"));
    }

    @Override
    public void afterPropertiesSet() {
        String problem = problem(frontendUrl, production);
        if (problem != null) {
            throw new IllegalStateException(problem);
        }
        log.info("Frontend base URL for account-recovery links: {} (production={})",
                frontendUrl, String.valueOf(production));
    }

    /**
     * @param frontendUrl the configured base URL
     * @param production  whether the prod profile is active
     * @return a human-readable reason the value is unusable, or {@code null} when it is valid
     */
    static String problem(String frontendUrl, boolean production) {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            return "app.frontend-url is empty. Set FRONTEND_URL in backend/.env to the public "
                    + "frontend origin (for example https://elmkusoma.co.tz) with no trailing "
                    + "slash, so emailed recovery links are not built as relative URLs.";
        }

        String value = frontendUrl.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        boolean http = lower.startsWith("http://") || lower.startsWith("https://");
        if (!http) {
            return "app.frontend-url must be an absolute http(s) URL, but was configured as '"
                    + value + "'. Set FRONTEND_URL to the full origin including the scheme.";
        }

        if (value.endsWith("/")) {
            return "app.frontend-url must not end with '/', but was configured as '" + value
                    + "'. Recovery links are built as <base>/reset-password, so a trailing "
                    + "slash would produce a double slash in the link.";
        }

        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            return "app.frontend-url is not a parseable URL: '" + value + "'.";
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            return "app.frontend-url has no host component: '" + value
                    + "'. Expected an origin such as https://elmkusoma.co.tz.";
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return "app.frontend-url must be an origin only (no ?query or #fragment), but was "
                    + "configured as '" + value + "'.";
        }

        if (production && isLoopback(uri.getHost())) {
            return "app.frontend-url is '" + value + "' while the prod profile is active. "
                    + "Production recovery links would point at this machine and never reach "
                    + "the user. Set FRONTEND_URL to the real public frontend origin.";
        }
        return null;
    }

    private static boolean isLoopback(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        return LOOPBACK_HOSTS.contains(h) || h.endsWith(".localhost");
    }
}
