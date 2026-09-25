package tz.elmkusoma.parent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.parent.service.ParentPaymentService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/webhooks/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhooks", description = "Gateway callbacks — verify transactions and grant entitlements")
public class PaymentWebhookController {

    private final PaymentRepository paymentRepository;
    private final ParentPaymentService paymentService;
    private final tz.elmkusoma.administration.service.PlatformIntegrationService integrationService;

    /** No default secret: an unset property must fail closed, never accept a guessable value. */
    @Value("${payment.webhook.secret:}")
    private String webhookSecret;

    public static class PaymentWebhookPayload {
        public UUID paymentId;
        public String providerReference;
        public String result; // SUCCESS | FAILED
        public String provider;
    }

    @PostMapping
    @Operation(summary = "Payment gateway webhook — SUCCESS grants entitlement, FAILED marks payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody PaymentWebhookPayload payload) {

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Payment webhook rejected: payment.webhook.secret is not configured (fail-closed)");
            integrationService.recordWebhook("PAYMENT", "rejected", "FAILED", "FAILED", "Webhook secret not configured");
            return ResponseEntity.status(503).body(ApiResponse.error("Payment webhook is not configured"));
        }
        if (secret == null || !webhookSecret.equals(secret)) {
            log.warn("Payment webhook rejected: invalid secret");
            integrationService.recordWebhook("PAYMENT", "rejected", "FAILED", "FAILED", "Invalid webhook secret");
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid webhook secret"));
        }
        if (payload == null || payload.paymentId == null || payload.result == null) {
            integrationService.recordWebhook("PAYMENT", "malformed", "FAILED", "FAILED", "paymentId and result are required");
            return ResponseEntity.badRequest().body(ApiResponse.error("paymentId and result are required"));
        }

        Payment payment = paymentRepository.findById(payload.paymentId)
                .orElse(null);
        if (payment == null) {
            integrationService.recordWebhook("PAYMENT", "payment_callback", "VERIFIED", "FAILED", "Payment not found");
            return ResponseEntity.status(404).body(ApiResponse.error("Payment not found"));
        }

        String result = payload.result.toUpperCase();
        if ("SUCCESS".equals(result)) {
            if ("COMPLETED".equals(payment.getStatus())) {
                log.info("Duplicate payment webhook for {}", payload.paymentId);
                integrationService.recordWebhook("PAYMENT", "payment_callback", "VERIFIED", "SUCCESS", "Duplicate callback");
                return ResponseEntity.ok(ApiResponse.success(Map.of(
                        "paymentId", payload.paymentId,
                        "status", "COMPLETED",
                        "duplicate", true)));
            }
            paymentService.verifyPayment(payload.paymentId,
                    payload.providerReference, null);
            log.info("Payment webhook: {} completed, entitlement granted", payload.paymentId);
            integrationService.recordWebhook("PAYMENT", "payment_callback", "VERIFIED", "SUCCESS", null);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "paymentId", payload.paymentId,
                    "status", "COMPLETED",
                    "entitlementGranted", true)));
        } else if ("FAILED".equals(result)) {
            if (!"COMPLETED".equals(payment.getStatus())) {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
            }
            log.info("Payment webhook: {} failed", payload.paymentId);
            integrationService.recordWebhook("PAYMENT", "payment_callback", "VERIFIED", "FAILED", "Gateway reported failure");
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "paymentId", payload.paymentId,
                    "status", payment.getStatus())));
        } else {
            integrationService.recordWebhook("PAYMENT", "payment_callback", "VERIFIED", "FAILED", "Invalid result value");
            return ResponseEntity.badRequest().body(ApiResponse.error("result must be SUCCESS or FAILED"));
        }
    }
}
