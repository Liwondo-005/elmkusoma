package tz.elmkusoma.payments.gateway;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fail-fast startup validation for the Stakaba gateway configuration.
 *
 * <p>Runs only when the gateway is enabled; a disabled provider (the default)
 * needs no credentials and this validator stays silent, so existing
 * deployments and test contexts are unaffected. When enabled, missing or
 * malformed configuration aborts startup with a message that names the
 * environment variables but never prints a credential value.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StakabaConfigurationValidator {

    private final StakabaProperties properties;

    @PostConstruct
    void validate() {
        if (!properties.isEnabled()) {
            log.info("Stakaba payment gateway disabled (STAKABA_ENABLED!=true); "
                    + "legacy manual payment verification remains in effect");
            return;
        }
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank() || !baseUrl.startsWith("https://")) {
            throw new IllegalStateException("Stakaba gateway enabled but payment.stakaba.base-url "
                    + "is missing or not HTTPS. Set STAKABA_BASE_URL to "
                    + "https://api.stakaba.com (the documented base URL).");
        }
        if (!properties.hasApiKey()) {
            throw new IllegalStateException("Stakaba gateway enabled but no API key configured. "
                    + "Set STAKABA_API_KEY in backend/.env to your sandbox key (sk_test_...). "
                    + "Never commit the key or paste it into source files.");
        }
        if (properties.isLiveKey() && !properties.isAllowLive()) {
            // Sandbox-only deployment: a live key must never be accepted by accident.
            throw new IllegalStateException("Stakaba gateway refused to start: a LIVE key "
                    + "(sk_live_...) was supplied but payment.stakaba.allow-live is false. "
                    + "Use the sandbox key (sk_test_...) for testing. Live payments require "
                    + "an approved go-live and an explicit STAKABA_ALLOW_LIVE=true, which is "
                    + "not set automatically.");
        }
        if (!properties.isSandboxKey() && !properties.isLiveKey()) {
            throw new IllegalStateException("Stakaba gateway enabled but STAKABA_API_KEY does "
                    + "not look like a Stakaba key (expected sk_test_... or sk_live_...). "
                    + "Check the key copied from app.stakaba.com > Settings > API Keys.");
        }
        log.info("Stakaba payment gateway enabled: baseUrl={} keyMode={} (value never logged)",
                baseUrl, properties.isLiveKey() ? "LIVE" : "SANDBOX");
    }
}
