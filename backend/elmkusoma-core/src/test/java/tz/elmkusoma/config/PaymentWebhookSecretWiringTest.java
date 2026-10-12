package tz.elmkusoma.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@code payment.webhook.secret} was only readable through Spring's relaxed environment
 * binding, which consults real environment variables - never the system properties that the
 * {@code backend/.env} loader writes. A developer could therefore put the secret in
 * {@code backend/.env} and see it silently ignored, leaving the endpoint permanently
 * fail-closed with no indication why.
 *
 * <p>These assertions pin the explicit {@code application.yml} mapping. They also pin that the
 * default stays empty, because an empty secret is what makes
 * {@code PaymentWebhookController} reject every webhook.</p>
 */
class PaymentWebhookSecretWiringTest {

    private static final String SENTINEL = "sentinel-webhook-secret-value";

    private String previous;

    @BeforeEach
    void isolateTheRealEnvironment() {
        previous = System.getProperty("PAYMENT_WEBHOOK_SECRET");
        if (previous != null) {
            System.clearProperty("PAYMENT_WEBHOOK_SECRET");
        }
    }

    @AfterEach
    void restoreTheRealEnvironment() {
        if (previous == null) {
            System.clearProperty("PAYMENT_WEBHOOK_SECRET");
        } else {
            System.setProperty("PAYMENT_WEBHOOK_SECRET", previous);
        }
    }

    @Test
    void paymentWebhookSecret_envVarReachesThePropertyControllersRead() throws IOException {
        System.setProperty("PAYMENT_WEBHOOK_SECRET", SENTINEL);

        assertEquals(SENTINEL, environment().getProperty("payment.webhook.secret"),
                "PAYMENT_WEBHOOK_SECRET (the name backend/.env supplies) must drive "
                        + "payment.webhook.secret");
    }

    @Test
    void paymentWebhookSecret_defaultsToEmptySoWebhooksStayFailClosed() throws IOException {
        assertNotNull(environment().getProperty("payment.webhook.secret"),
                "application.yml must declare the property, not leave it to @Value defaults");
        assertEquals("", environment().getProperty("payment.webhook.secret"),
                "unset must remain empty - that is what makes the controller reject everything");
    }

    // ------------------------------------------------------------------ helpers

    private static StandardEnvironment environment() throws IOException {
        StandardEnvironment env = new StandardEnvironment();
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        for (PropertySource<?> source : sources) {
            env.getPropertySources().addLast(source);
        }
        return env;
    }
}
