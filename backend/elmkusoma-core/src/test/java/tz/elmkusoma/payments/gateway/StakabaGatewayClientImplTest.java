package tz.elmkusoma.payments.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Contract tests for the Stakaba HTTP adapter against a mocked server.
 * No real network calls; request/response shapes are pinned to the official
 * documentation (collection 201 + internalReference; documented error body
 * and status codes; integer whole-TZS amounts; exact network strings).
 */
class StakabaGatewayClientImplTest {

    private static final String BASE = "https://api.stakaba.com";
    private static final String KEY = "sk_test_dummy-key-for-contract-tests";

    private StakabaProperties properties;
    private MockRestServiceServer server;
    private StakabaGatewayClientImpl client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        properties = new StakabaProperties();
        properties.setBaseUrl(BASE);
        properties.setApiKey(KEY);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE).build();
        client = new StakabaGatewayClientImpl(properties, restClient, objectMapper);
    }

    @Test
    void initiateCollection_sendsDocumentedShape_andParsesInternalReference() {
        server.expect(requestTo(BASE + "/api/v1/payments/collection"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("x-api-key", KEY))
                .andExpect(jsonPath("$.grossAmount").value(5000))
                .andExpect(jsonPath("$.currency").value("TZS"))
                .andExpect(jsonPath("$.mobileNumber").value("255742000331"))
                .andExpect(jsonPath("$.network").value("Mpesa"))
                .andExpect(jsonPath("$.metadata.paymentId").value("pay-1"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"internalReference\":\"M_X7Y8Z9W0\"}"));

        StakabaGatewayClient.CollectionInitiated result = client.initiateCollection(
                new BigDecimal("5000"), "TZS", "255742000331", "Mpesa",
                Map.of("paymentId", "pay-1"));

        assertEquals("M_X7Y8Z9W0", result.internalReference());
        server.verify();
    }

    @Test
    void initiateCollection_amountMustBeWhole_beforeItReachesTheWire() {
        // 4999.50 would be rejected by the documented contract; the adapter's
        // longValueExact throws rather than silently truncating.
        assertThrows(ArithmeticException.class, () -> client.initiateCollection(
                new BigDecimal("4999.50"), "TZS", "255742000331", "Mpesa", Map.of()));
        server.verify();
    }

    @Test
    void initiateCollection_unauthorizedMapsToAuthenticationError() {
        server.expect(requestTo(BASE + "/api/v1/payments/collection"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid merchant API key\"}"));

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class,
                () -> client.initiateCollection(new BigDecimal("5000"), "TZS",
                        "255742000331", "Mpesa", Map.of()));
        assertEquals(PaymentGatewayException.Code.AUTHENTICATION, ex.getCode());
        // The provider's message is not echoed; the safe message must not leak the key.
        assertFalse(ex.getMessage().contains("sk_test"));
    }

    @Test
    void initiateCollection_validationErrorSurfacesSafeDetail() {
        server.expect(requestTo(BASE + "/api/v1/payments/collection"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"VALIDATION_ERROR\","
                                + "\"message\":\"network must be Mpesa, TigoPesa, AirtelMoney, or HaloPesa\"}"));

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class,
                () -> client.initiateCollection(new BigDecimal("5000"), "TZS",
                        "255742000331", "Mpesa", Map.of()));
        assertEquals(PaymentGatewayException.Code.VALIDATION, ex.getCode());
        assertTrue(ex.getMessage().contains("network must be"));
    }

    @Test
    void initiateCollection_providerErrorMapsToProviderCode() {
        server.expect(requestTo(BASE + "/api/v1/payments/collection"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"PROVIDER_ERROR\",\"message\":\"try later\"}"));

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class,
                () -> client.initiateCollection(new BigDecimal("5000"), "TZS",
                        "255742000331", "Mpesa", Map.of()));
        assertEquals(PaymentGatewayException.Code.PROVIDER, ex.getCode());
    }

    @Test
    void initiateCollection_malformedSuccessBodyIsNotTreatedAsInitiated() {
        server.expect(requestTo(BASE + "/api/v1/payments/collection"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class,
                () -> client.initiateCollection(new BigDecimal("5000"), "TZS",
                        "255742000331", "Mpesa", Map.of()));
        assertEquals(PaymentGatewayException.Code.MALFORMED_RESPONSE, ex.getCode());
    }

    @Test
    void getTransaction_parsesStatusAmountAndCurrency() {
        server.expect(requestTo(BASE + "/api/v1/transactions/M_X7Y8Z9W0"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("x-api-key", KEY))
                .andRespond(withSuccess(
                        "{\"internalReference\":\"M_X7Y8Z9W0\",\"status\":\"SUCCESS\","
                                + "\"grossAmount\":5000,\"currency\":\"TZS\","
                                + "\"providerReference\":\"MPESA12345\"}",
                        MediaType.APPLICATION_JSON));

        StakabaGatewayClient.GatewayTransaction tx =
                client.getTransaction("M_X7Y8Z9W0");

        assertTrue(tx.isSuccess());
        assertFalse(tx.isFailed());
        assertEquals(0, new BigDecimal("5000").compareTo(tx.grossAmount()));
        assertEquals("TZS", tx.currency());
        assertEquals("MPESA12345", tx.providerReference());
    }

    @Test
    void getTransaction_notFoundMapsToNotFoundCode() {
        server.expect(requestTo(BASE + "/api/v1/transactions/M_MISSING"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"NOT_FOUND\",\"message\":\"no such transaction\"}"));

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class,
                () -> client.getTransaction("M_MISSING"));
        assertEquals(PaymentGatewayException.Code.NOT_FOUND, ex.getCode());
    }

    @Test
    void getTransaction_pendingStatusIsNeitherSuccessNorFailed() {
        server.expect(requestTo(BASE + "/api/v1/transactions/M_PENDING"))
                .andRespond(withSuccess(
                        "{\"internalReference\":\"M_PENDING\",\"status\":\"PENDING\"}",
                        MediaType.APPLICATION_JSON));

        StakabaGatewayClient.GatewayTransaction tx = client.getTransaction("M_PENDING");
        assertFalse(tx.isSuccess());
        assertFalse(tx.isFailed());
    }

    @Test
    void getTransaction_transientServerErrorIsRetriedThenSucceeds() {
        server.expect(requestTo(BASE + "/api/v1/transactions/M_RETRY"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo(BASE + "/api/v1/transactions/M_RETRY"))
                .andRespond(withSuccess(
                        "{\"internalReference\":\"M_RETRY\",\"status\":\"SUCCESS\","
                                + "\"grossAmount\":1000,\"currency\":\"TZS\"}",
                        MediaType.APPLICATION_JSON));

        StakabaGatewayClient.GatewayTransaction tx = client.getTransaction("M_RETRY");
        assertTrue(tx.isSuccess());
        server.verify();
    }

    @Test
    void getTransaction_persistentFailureSurfacesProviderError() {
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3),
                        requestTo(BASE + "/api/v1/transactions/M_DOWN"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class,
                () -> client.getTransaction("M_DOWN"));
        assertEquals(PaymentGatewayException.Code.PROVIDER, ex.getCode());
        server.verify();
    }
}
