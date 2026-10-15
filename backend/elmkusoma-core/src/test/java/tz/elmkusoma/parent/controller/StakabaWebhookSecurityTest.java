package tz.elmkusoma.parent.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.payments.gateway.PaymentGatewayException;
import tz.elmkusoma.parent.service.PaymentGatewayService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for the Stakaba webhook (standalone MockMvc, no Spring
 * context/DB - fully hermetic). The documented payload has no signature, so
 * the endpoint never trusts it: this test pins that malformed events are
 * refused, unknown references are acknowledged-and-ignored, infrastructure
 * failures answer 5xx so the provider retries, and no path can fulfil a
 * payment without the service's verified pipeline being invoked.
 */
@ExtendWith(MockitoExtension.class)
class StakabaWebhookSecurityTest {

    @Mock
    private PaymentGatewayService paymentGatewayService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new StakabaWebhookController(paymentGatewayService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingEventOrReference_is400() throws Exception {
        mockMvc.perform(post("/v1/webhooks/stakaba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"transaction.success\"}"))
                .andExpect(status().isBadRequest());

        verify(paymentGatewayService, never()).handleStakabaEvent(any(), any(), any(), any());
    }

    @Test
    void unsupportedEventType_isAcknowledgedAndIgnored() throws Exception {
        mockMvc.perform(post("/v1/webhooks/stakaba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"transaction.refunded\","
                                + "\"internalReference\":\"M_X1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(false));

        verify(paymentGatewayService, never()).handleStakabaEvent(any(), any(), any(), any());
    }

    @Test
    void unknownReference_isAcknowledgedWithoutFulfilment() throws Exception {
        when(paymentGatewayService.handleStakabaEvent(eq("transaction.success"),
                eq("M_UNKNOWN"), any(), any()))
                .thenReturn(new PaymentGatewayService.WebhookOutcome(
                        null, null, false, false, "unknown_reference"));

        mockMvc.perform(post("/v1/webhooks/stakaba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"transaction.success\","
                                + "\"internalReference\":\"M_UNKNOWN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(false))
                .andExpect(jsonPath("$.data.reason").value("unknown_reference"));
    }

    @Test
    void verifiedSuccess_reportsFulfilment() throws Exception {
        when(paymentGatewayService.handleStakabaEvent(eq("transaction.success"),
                eq("M_OK"), any(), any()))
                .thenReturn(new PaymentGatewayService.WebhookOutcome(
                        "pay-1", "COMPLETED", true, true, null));

        mockMvc.perform(post("/v1/webhooks/stakaba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"transaction.success\","
                                + "\"internalReference\":\"M_OK\",\"status\":\"SUCCESS\","
                                + "\"grossAmount\":5000,\"currency\":\"TZS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.fulfilled").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void verificationInfrastructureFailure_is500SoProviderRetries() throws Exception {
        when(paymentGatewayService.handleStakabaEvent(eq("transaction.success"),
                eq("M_FLAKY"), any(), any()))
                .thenThrow(new PaymentGatewayException(
                        PaymentGatewayException.Code.NETWORK, "unreachable"));

        mockMvc.perform(post("/v1/webhooks/stakaba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"transaction.success\","
                                + "\"internalReference\":\"M_FLAKY\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void mismatchOutcome_isAcknowledgedButNotFulfilled() throws Exception {
        when(paymentGatewayService.handleStakabaEvent(eq("transaction.success"),
                eq("M_MISMATCH"), any(), any()))
                .thenReturn(new PaymentGatewayService.WebhookOutcome(
                        "pay-2", "PENDING", true, false, "verification_refused"));

        mockMvc.perform(post("/v1/webhooks/stakaba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"transaction.success\","
                                + "\"internalReference\":\"M_MISMATCH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.fulfilled").value(false));
    }

    @Test
    void failedEvent_isProcessedThroughTheSameVerifiedPipeline() throws Exception {
        when(paymentGatewayService.handleStakabaEvent(eq("transaction.failed"),
                eq("M_FAIL"), any(), any()))
                .thenReturn(new PaymentGatewayService.WebhookOutcome(
                        "pay-3", "FAILED", true, false, null));

        mockMvc.perform(post("/v1/webhooks/stakaba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"transaction.failed\","
                                + "\"internalReference\":\"M_FAIL\",\"status\":\"FAILED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.fulfilled").value(false));
    }
}
