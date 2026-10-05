package tz.elmkusoma.parent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.administration.service.PlatformIntegrationService;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.parent.service.ParentPaymentService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Payment-gateway webhook security regression tests (standalone MockMvc, no
 * Spring context / DB — fully hermetic).
 *
 * <p>Contracts pinned here (verified against
 * {@code PaymentWebhookController} before writing):</p>
 * <ul>
 *   <li>missing/wrong {@code X-Webhook-Secret} -&gt; 401, no repository or
 *       service interaction (no state change);</li>
 *   <li>mismatched amount -&gt; 422, PENDING preserved, no completion;</li>
 *   <li>mismatched currency -&gt; 422, no completion;</li>
 *   <li>providerReference already used by ANOTHER payment -&gt; 409 replay
 *       rejection, no completion;</li>
 *   <li>same-payment retry after COMPLETED -&gt; idempotent 200
 *       ({@code duplicate=true}), service not invoked again;</li>
 *   <li>legacy payload without amount/currency -&gt; stored record stays
 *       authoritative, payment completes with entitlement.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PaymentWebhookSecurityTest {

    private static final String SECRET = "test-webhook-secret";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ParentPaymentService paymentService;
    @Mock
    private PlatformIntegrationService integrationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PaymentWebhookController controller =
                new PaymentWebhookController(paymentRepository, paymentService, integrationService);
        ReflectionTestUtils.setField(controller, "webhookSecret", SECRET);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Payment pendingPayment(UUID id) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setParentId(UUID.randomUUID());
        payment.setStudentId(UUID.randomUUID());
        payment.setInstitutionId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("TZS");
        payment.setServiceType("TUITION");
        payment.setStatus("PENDING");
        return payment;
    }

    private MvcResult postWebhook(Map<String, Object> payload, String secret) throws Exception {
        var req = post("/v1/webhooks/payments")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload));
        if (secret != null) {
            req.header("X-Webhook-Secret", secret);
        }
        return mockMvc.perform(req).andReturn();
    }

    @Test
    void missingSecret_is401WithNoStateChange() throws Exception {
        UUID paymentId = UUID.randomUUID();
        MvcResult result = postWebhook(Map.of(
                "paymentId", paymentId.toString(),
                "result", "SUCCESS",
                "providerReference", "ref-1"), null);

        assertEquals(401, result.getResponse().getStatus());
        verify(paymentRepository, never()).findById(any());
        verify(paymentService, never()).verifyPayment(any(), any(), any());
    }

    @Test
    void wrongSecret_is401WithNoStateChange() throws Exception {
        UUID paymentId = UUID.randomUUID();
        MvcResult result = postWebhook(Map.of(
                "paymentId", paymentId.toString(),
                "result", "SUCCESS",
                "providerReference", "ref-1"), "wrong-secret");

        assertEquals(401, result.getResponse().getStatus());
        verify(paymentRepository, never()).findById(any());
        verify(paymentService, never()).verifyPayment(any(), any(), any());
    }

    @Test
    void amountMismatch_is422AndPendingPreserved() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = pendingPayment(paymentId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        mockMvc.perform(post("/v1/webhooks/payments")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType("application/json")
                        .content("{\"paymentId\":\"" + paymentId + "\",\"result\":\"SUCCESS\","
                                + "\"providerReference\":\"ref-A\",\"amount\":1.00,\"currency\":\"TZS\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Amount does not match the initiated payment"));

        verify(paymentService, never()).verifyPayment(any(), any(), any());
        assertEquals("PENDING", payment.getStatus());
    }

    @Test
    void currencyMismatch_is422WithNoCompletion() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = pendingPayment(paymentId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        mockMvc.perform(post("/v1/webhooks/payments")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType("application/json")
                        .content("{\"paymentId\":\"" + paymentId + "\",\"result\":\"SUCCESS\","
                                + "\"providerReference\":\"ref-B\",\"amount\":100.00,\"currency\":\"USD\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Currency does not match the initiated payment"));

        verify(paymentService, never()).verifyPayment(any(), any(), any());
        assertEquals("PENDING", payment.getStatus());
    }

    @Test
    void providerReferenceReplayAcrossPayments_is409() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = pendingPayment(paymentId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByProviderReferenceAndIdNotAndIsDeletedFalse("ref-used", paymentId))
                .thenReturn(true);

        mockMvc.perform(post("/v1/webhooks/payments")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType("application/json")
                        .content("{\"paymentId\":\"" + paymentId + "\",\"result\":\"SUCCESS\","
                                + "\"providerReference\":\"ref-used\",\"amount\":100.00,\"currency\":\"TZS\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("providerReference already used by another payment"));

        verify(paymentService, never()).verifyPayment(any(), any(), any());
        assertEquals("PENDING", payment.getStatus());
    }

    @Test
    void samePaymentRetry_isIdempotentSuccess() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment completed = pendingPayment(paymentId);
        completed.setStatus("COMPLETED");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(completed));

        mockMvc.perform(post("/v1/webhooks/payments")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType("application/json")
                        .content("{\"paymentId\":\"" + paymentId + "\",\"result\":\"SUCCESS\","
                                + "\"providerReference\":\"ref-same\",\"amount\":100.00,\"currency\":\"TZS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.duplicate").value(true));

        // No second entitlement grant.
        verify(paymentService, never()).verifyPayment(any(), any(), any());
    }

    @Test
    void legacyPayloadWithoutAmount_completesAgainstStoredRecord() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = pendingPayment(paymentId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByProviderReferenceAndIdNotAndIsDeletedFalse("ref-legacy", paymentId))
                .thenReturn(false);

        Payment completed = pendingPayment(paymentId);
        completed.setStatus("COMPLETED");
        when(paymentService.verifyPayment(paymentId, "ref-legacy", null)).thenReturn(completed);

        mockMvc.perform(post("/v1/webhooks/payments")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType("application/json")
                        .content("{\"paymentId\":\"" + paymentId + "\",\"result\":\"SUCCESS\","
                                + "\"providerReference\":\"ref-legacy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.entitlementGranted").value(true));

        verify(paymentService).verifyPayment(paymentId, "ref-legacy", null);
    }
}
