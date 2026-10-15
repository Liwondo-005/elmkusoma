package tz.elmkusoma.payments.gateway;

/**
 * Failure raised by the outbound payment-gateway adapter.
 *
 * <p>Carries only a safe, user-presentable message and a coarse error code.
 * Provider response bodies, API keys and raw exception chains are never
 * copied into the message - they stay in server-side logs, and even there
 * only in sanitised form.</p>
 */
public class PaymentGatewayException extends RuntimeException {

    /** Coarse classification used by the HTTP layer to pick a status code. */
    public enum Code {
        /** Missing/invalid API key or permission - configuration problem, not user error. */
        AUTHENTICATION,
        /** Gateway rejected the request as invalid (bad amount, network, number...). */
        VALIDATION,
        /** Mobile-money operator/provider side failure. */
        PROVIDER,
        /** Gateway rate limit (HTTP 429). */
        RATE_LIMITED,
        /**
         * The outcome of a non-idempotent collection request is unknown
         * (connect/read timeout after the request may have been received).
         * The caller MUST NOT blindly retry; verify the original attempt's
         * status first, or let the payer start a fresh payment.
         */
        TIMEOUT_AMBIGUOUS,
        /** Could not reach the gateway at all (connection refused/DNS...). */
        NETWORK,
        /** Gateway answered with a body that does not match the documented contract. */
        MALFORMED_RESPONSE,
        /** Gateway says the referenced transaction does not exist. */
        NOT_FOUND,
        /**
         * Gateway transaction disagrees with the locally stored payment
         * (amount/currency/reference). Fulfilment is refused; the payment
         * needs human review.
         */
        MISMATCH
    }

    private final Code code;

    public PaymentGatewayException(Code code, String safeMessage) {
        super(safeMessage);
        this.code = code;
    }

    public PaymentGatewayException(Code code, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
