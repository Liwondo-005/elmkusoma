package tz.elmkusoma.payments.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stakaba REST adapter (documented contract only - see
 * {@code https://docs.stakaba.com/payments/mobile-money-collections.md} and
 * {@code .../reference/error-handling.md}).
 *
 * <p>Contract facts this implementation is pinned to:</p>
 * <ul>
 *   <li>Base URL {@code https://api.stakaba.com} - same for sandbox and live;
 *       the {@code x-api-key} value selects the environment.</li>
 *   <li>Collection: {@code POST /api/v1/payments/collection} with
 *       {@code x-api-key} + JSON body; success is HTTP 201 with
 *       {@code {"internalReference": "..."}}. The reference only means the
 *       USSD prompt was queued - never that the payer paid.</li>
 *   <li>Status: {@code GET /api/v1/transactions/{internalReference}}.</li>
 *   <li>Errors: {@code {"error": CODE, "message": "..."}} with 400/401/403/404/
 *       422/429/500/502 semantics as documented. 500/502 are safe to retry
 *       with backoff; 400/401 are not. Each collection attempt mints a NEW
 *       internalReference, so a timed-out collection is never retried blindly
 *       from here.</li>
 * </ul>
 *
 * <p>No credentials, PINs or full request payloads are ever logged.</p>
 */
@Component
@Slf4j
public class StakabaGatewayClientImpl implements StakabaGatewayClient {

    private static final String COLLECTION_PATH = "/api/v1/payments/collection";
    private static final String TRANSACTION_PATH = "/api/v1/transactions/{ref}";
    /** Transient-failure retries for the read-only status lookup only. */
    private static final int STATUS_LOOKUP_ATTEMPTS = 3;

    private final StakabaProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * The Spring constructor. There are two constructors here, so Spring cannot choose one by
     * itself and would fall back to looking for a default constructor that does not exist -
     * which fails the whole application context, not just this bean. Marked {@code @Autowired}
     * so it is unambiguous which one the container uses.
     */
    @Autowired
    public StakabaGatewayClientImpl(StakabaProperties properties, ObjectMapper objectMapper) {
        this(properties, buildClient(properties), objectMapper);
    }

    /** Test-friendly constructor: inject a pre-built client (e.g. bound to a mock server). */
    StakabaGatewayClientImpl(StakabaProperties properties, RestClient restClient,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    private static RestClient buildClient(StakabaProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .withReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public CollectionInitiated initiateCollection(BigDecimal grossAmount, String currency,
                                                   String mobileNumber, String network,
                                                   Map<String, String> metadata) {
        // Whole-number TZS: operators reject decimals (documented). Sending a
        // long also guarantees the JSON body carries an integer, never 5000.00.
        long amount = grossAmount.longValueExact();
        Map<String, Object> body = Map.of(
                "grossAmount", amount,
                "currency", currency,
                "mobileNumber", mobileNumber,
                "network", network,
                "metadata", metadata == null ? Map.of() : metadata);

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(COLLECTION_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", properties.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            // A 5xx on a NON-idempotent collection is ambiguous: the prompt may
            // or may not have been queued. Never retried from this layer.
            throw mapHttpError(ex.getStatusCode(), ex.getResponseBodyAsString(), "collection");
        } catch (ResourceAccessException ex) {
            // Connect/read timeout or connection failure. If the request left
            // us, Stakaba may still have queued the prompt - ambiguous.
            log.warn("Stakaba collection transport failure (outcome unknown): {}",
                    ex.getClass().getSimpleName());
            throw new PaymentGatewayException(PaymentGatewayException.Code.TIMEOUT_AMBIGUOUS,
                    "The payment request could not be confirmed. Do not assume it failed - "
                            + "check whether a prompt arrived on the phone before retrying.", ex);
        }

        Object reference = response == null ? null : response.get("internalReference");
        if (reference == null || reference.toString().isBlank()) {
            throw new PaymentGatewayException(PaymentGatewayException.Code.MALFORMED_RESPONSE,
                    "The payment provider returned an unexpected response.");
        }
        log.info("Stakaba collection queued: internalReference={}", reference);
        return new CollectionInitiated(reference.toString());
    }

    @Override
    public GatewayTransaction getTransaction(String internalReference) {
        RuntimeException last = null;
        // The docs explicitly allow retries with backoff for 500/502; this is a
        // read-only lookup, so a small fixed number of attempts is safe.
        for (int attempt = 1; attempt <= STATUS_LOOKUP_ATTEMPTS; attempt++) {
            try {
                Map<String, Object> response = restClient.get()
                        .uri(TRANSACTION_PATH, internalReference)
                        .headers(headers -> {
                            headers.set("x-api-key", properties.getApiKey());
                            if (properties.isSendBearerFallback()) {
                                // Stakaba's spec declares HTTPBearer for this endpoint while
                                // its prose guides use x-api-key; send both documented schemes
                                // (see StakabaProperties#sendBearerFallback). Read-only call.
                                headers.set(HttpHeaders.AUTHORIZATION,
                                        "Bearer " + properties.getApiKey());
                            }
                        })
                        .retrieve()
                        .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                        });
                return parseTransaction(internalReference, response);
            } catch (HttpClientErrorException ex) {
                throw mapHttpError(ex.getStatusCode(), ex.getResponseBodyAsString(),
                        "transaction lookup");
            } catch (HttpServerErrorException ex) {
                last = new PaymentGatewayException(PaymentGatewayException.Code.PROVIDER,
                        "The payment provider is temporarily unavailable.", ex);
            } catch (ResourceAccessException ex) {
                last = new PaymentGatewayException(PaymentGatewayException.Code.NETWORK,
                        "Could not reach the payment provider.", ex);
            }
            if (attempt < STATUS_LOOKUP_ATTEMPTS) {
                try {
                    Thread.sleep(200L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw last;
                }
            }
        }
        throw last;
    }

    /**
     * Parses the transaction-status response.
     *
     * <p>Stakaba publishes this endpoint's response as an untyped
     * {@code map<string, any>} in its OpenAPI spec, so no field list can be
     * trusted and none is invented here. The parser therefore: (a) accepts the
     * flat object the webhook documentation shows, and tolerates the same
     * object wrapped in {@code data}/{@code transaction}; (b) reads the
     * amount from {@code grossAmount} and tolerates {@code amount}; and
     * (c) leaves a missing amount as {@code null} rather than guessing zero -
     * the caller refuses to fulfil a claimed success that carries no amount.</p>
     */
    private GatewayTransaction parseTransaction(String internalReference, Map<String, Object> body) {
        if (body == null) {
            throw new PaymentGatewayException(PaymentGatewayException.Code.MALFORMED_RESPONSE,
                    "The payment provider returned an unexpected response.");
        }
        Map<String, Object> fields = unwrap(body);
        Object status = fields.get("status");
        BigDecimal amount = null;
        Object rawAmount = fields.get("grossAmount") != null ? fields.get("grossAmount") : fields.get("amount");
        if (rawAmount instanceof Number number) {
            amount = new BigDecimal(number.toString());
        } else if (rawAmount instanceof String text && !text.isBlank()) {
            try {
                amount = new BigDecimal(text.trim());
            } catch (NumberFormatException ignored) {
                // Unparseable amount stays null: fulfilment will be refused.
            }
        }
        String providerReference = fields.get("providerReference") == null
                ? null : fields.get("providerReference").toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = fields.get("metadata") instanceof Map
                ? (Map<String, Object>) fields.get("metadata") : Map.of();
        Object ref = fields.getOrDefault("internalReference", internalReference);
        return new GatewayTransaction(
                ref == null ? internalReference : ref.toString(),
                status == null ? null : status.toString().toUpperCase(Locale.ROOT),
                amount,
                fields.get("currency") == null ? null : fields.get("currency").toString(),
                providerReference,
                metadata);
    }

    /** Flattens a {@code data}/{@code transaction} envelope when one is present. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> body) {
        if (body.containsKey("status")) {
            return body;
        }
        for (String envelope : new String[]{"data", "transaction"}) {
            if (body.get(envelope) instanceof Map<?, ?> inner) {
                return (Map<String, Object>) inner;
            }
        }
        return body;
    }

    private PaymentGatewayException mapHttpError(HttpStatusCode status, String body, String operation) {
        String providerMessage = extractSafeMessage(body);
        int code = status.value();
        if (code == 401 || code == 403) {
            log.error("Stakaba {} rejected: HTTP {} (check STAKABA_API_KEY)", operation, code);
            return new PaymentGatewayException(PaymentGatewayException.Code.AUTHENTICATION,
                    "The payment provider rejected the server credentials. "
                            + "Configuration needs attention; no money was moved.");
        }
        if (code == 404) {
            return new PaymentGatewayException(PaymentGatewayException.Code.NOT_FOUND,
                    "The payment provider does not know this transaction reference.");
        }
        if (code == 429) {
            return new PaymentGatewayException(PaymentGatewayException.Code.RATE_LIMITED,
                    "The payment provider is rate-limiting requests. Try again shortly.");
        }
        if (code == 400 || code == 422) {
            String detail = providerMessage == null
                    ? "The payment request was rejected as invalid." : providerMessage;
            return new PaymentGatewayException(PaymentGatewayException.Code.VALIDATION, detail);
        }
        if (code == 502) {
            return new PaymentGatewayException(PaymentGatewayException.Code.PROVIDER,
                    "The mobile money provider rejected the request. Please try again.");
        }
        log.error("Stakaba {} failed: HTTP {}", operation, code);
        return new PaymentGatewayException(PaymentGatewayException.Code.PROVIDER,
                "The payment provider is temporarily unavailable.");
    }

    /**
     * Pulls the documented {@code message} field (or the first FastAPI
     * validation {@code msg}) out of an error body. Anything unrecognised
     * yields null - the raw body is never surfaced or logged, because it may
     * echo request data.
     */
    private String extractSafeMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
            });
            Object message = parsed.get("message");
            if (message instanceof String text && !text.isBlank()) {
                return text.length() > 300 ? text.substring(0, 300) : text;
            }
            if (parsed.get("detail") instanceof List<?> details && !details.isEmpty()) {
                Object first = details.get(0);
                if (first instanceof Map<?, ?> detail) {
                    Object msg = detail.get("msg");
                    if (msg instanceof String text && !text.isBlank()) {
                        return text.length() > 300 ? text.substring(0, 300) : text;
                    }
                }
            }
        } catch (Exception ignored) {
            // Unparseable body: fall through to a generic message.
        }
        return null;
    }
}
