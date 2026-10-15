package tz.elmkusoma.payments.gateway;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Outbound gateway contract used by the payment orchestration layer.
 *
 * <p>Implementations must never throw raw transport/parse exceptions; every
 * failure surfaces as a {@link PaymentGatewayException} with a safe message.
 * A collection attempt that times out after the request may have reached the
 * gateway raises {@link PaymentGatewayException.Code#TIMEOUT_AMBIGUOUS} and
 * MUST NOT be retried blindly - each collection creates a new
 * {@code internalReference} on the Stakaba side.</p>
 */
public interface StakabaGatewayClient {

    /** Result of a successfully initiated collection. */
    record CollectionInitiated(String internalReference) {
    }

    /**
     * Transaction state as reported by the gateway status lookup.
     *
     * <p>{@code status} is the raw provider status string (upper-cased by the
     * implementation when recognisable). {@code SUCCESS} and {@code FAILED}
     * are treated as final; anything else is still in flight. Amount/currency
     * are null when the provider omitted them, and callers must refuse to
     * fulfil when a claimed-success transaction carries no amount.</p>
     */
    record GatewayTransaction(
            String internalReference,
            String status,
            BigDecimal grossAmount,
            String currency,
            String providerReference,
            Map<String, Object> metadata) {

        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }

        public boolean isFailed() {
            return "FAILED".equalsIgnoreCase(status);
        }
    }

    /**
     * Initiates a mobile-money collection (USSD prompt on the payer's phone).
     *
     * <p>Per the documented contract: {@code POST /api/v1/payments/collection}
     * with {@code x-api-key}, whole-number TZS {@code grossAmount},
     * {@code mobileNumber} in 255XXXXXXXXX form and an exact-case
     * {@code network}. A returned reference means the prompt was queued - it
     * is NOT proof that the customer paid.</p>
     *
     * @param grossAmount whole TZS amount, positive
     * @param currency    always TZS for mobile money
     * @param mobileNumber normalised 255XXXXXXXXX msisdn
     * @param network     exact documented network value
     * @param metadata    merchant data echoed back in webhooks; keys are short
     *                    and values must never contain secrets
     * @throws PaymentGatewayException on any failure, including
     *         TIMEOUT_AMBIGUOUS when the outcome cannot be determined
     */
    CollectionInitiated initiateCollection(BigDecimal grossAmount, String currency,
                                           String mobileNumber, String network,
                                           Map<String, String> metadata);

    /**
     * Looks up one transaction by the reference returned from a collection.
     *
     * <p>Per the documented contract: {@code GET /api/v1/transactions/{internalReference}}.
     * Safe to retry on transient 5xx/network errors (the docs allow exponential
     * backoff on 500/502); at most a small fixed number of attempts is made.</p>
     *
     * @throws PaymentGatewayException.Code#NOT_FOUND when the gateway reports 404
     * @throws PaymentGatewayException on auth, rate-limit, transport or parse failure
     */
    GatewayTransaction getTransaction(String internalReference);
}
