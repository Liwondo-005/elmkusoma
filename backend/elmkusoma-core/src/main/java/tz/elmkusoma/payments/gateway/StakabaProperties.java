package tz.elmkusoma.payments.gateway;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Stakaba mobile-money gateway.
 *
 * <p>All values are supplied through environment variables (see
 * {@code backend/.env.example}); nothing here contains a real credential.
 * Sandbox and live share one base URL - the API key selects the environment -
 * so the {@code allow-live} interlock exists to keep a live key out of this
 * deployment by default.</p>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payment.stakaba")
public class StakabaProperties {

    /** Master switch. Disabled unless STAKABA_ENABLED=true. */
    private boolean enabled = false;

    /** Documented base URL; identical for sandbox and live. */
    private String baseUrl = "https://api.stakaba.com";

    /** Sandbox ({@code sk_test_...}) or live ({@code sk_live_...}) API key. */
    private String apiKey = "";

    private long connectTimeoutMs = 5000;

    private long readTimeoutMs = 15000;

    /**
     * Interlock against accidentally running with a live key. Never set this
     * to true without an approved Stakaba go-live and separate change control;
     * sandbox testing must not need it.
     */
    private boolean allowLive = false;

    /**
     * Send {@code Authorization: Bearer <api-key>} alongside {@code x-api-key}
     * on the transaction-status lookup, because Stakaba's own published
     * contract disagrees with itself about that endpoint: the OpenAPI spec
     * declares {@code security: [HTTPBearer]} and exposes no {@code x-api-key}
     * parameter for it, while the Authentication, Environments and Mobile
     * Money Collections guides all show {@code x-api-key}, and the spec itself
     * uses {@code x-api-key} (and no bearer) for the collection endpoint.
     *
     * <p>Both headers are therefore sent on that one read-only call so the
     * lookup succeeds whichever scheme the endpoint actually enforces. Nothing
     * is invented: both are documented by Stakaba. Set this to {@code false}
     * (STAKABA_SEND_BEARER_FALLBACK=false) to send only {@code x-api-key}.</p>
     */
    private boolean sendBearerFallback = true;

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean isLiveKey() {
        return apiKey != null && apiKey.trim().startsWith("sk_live_");
    }

    public boolean isSandboxKey() {
        return apiKey != null && apiKey.trim().startsWith("sk_test_");
    }
}
