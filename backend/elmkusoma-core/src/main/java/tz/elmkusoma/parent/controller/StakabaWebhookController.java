package tz.elmkusoma.parent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.payments.gateway.PaymentGatewayException;
import tz.elmkusoma.parent.service.PaymentGatewayService;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Inbound Stakaba webhook endpoint (documented payload shape from
 * {@code https://docs.stakaba.com/payments/webhooks.md}).
 *
 * <p><b>Authenticity note (documented limitation).</b> Stakaba's official
 * documentation specifies no webhook signature, HMAC header or mTLS scheme, so
 * this endpoint cannot authenticate a request by cryptographic proof - and
 * deliberately invents none. Instead it follows Stakaba's own mandated
 * verification step: every event is treated as an untrusted <em>hint</em>, and
 * {@link PaymentGatewayService#handleStakabaEvent} re-reads the transaction
 * from {@code GET /api/v1/transactions/{ref}} before any entitlement is
 * granted, additionally reconciling amount and currency against the locally
 * stored payment. A forged, replayed or inconsistent event can therefore never
 * mark a payment successful on its own. Registering a per-merchant shared
 * secret or IP allow-list with Stakaba, if offered outside the public docs,
 * remains an outstanding production hardening item.</p>
 *
 * <p>Responses: 200 acknowledges (including for duplicate, unknown or
 * already-final events - Stakaba retries up to 5 times on non-200, so only
 * genuinely transient verification failures answer 5xx to trigger a
 * redelivery); 400 for malformed payloads.</p>
 */
@RestController
@RequestMapping("/v1/webhooks/stakaba")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stakaba Webhooks", description = "Mobile-money gateway events (verified server-side before fulfilment)")
public class StakabaWebhookController {

    private final PaymentGatewayService paymentGatewayService;

    /** Documented Stakaba webhook payload (subset actually used). */
    public static class StakabaWebhookPayload {
        public String event;
        public String internalReference;
        public String status;
        public String type;
        public BigDecimal grossAmount;
        public String currency;
        public String network;
        public String providerReference;
        public Map<String, Object> metadata;
    }

    @PostMapping
    @Operation(summary = "Stakaba webhook - always re-verifies via the transaction API before fulfilment")
    public ResponseEntity<?> handleWebhook(@RequestBody(required = false) StakabaWebhookPayload payload) {
        if (payload == null || payload.event == null || payload.event.isBlank()
                || payload.internalReference == null || payload.internalReference.isBlank()) {
            log.warn("Stakaba webhook rejected: event and internalReference are required");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("event and internalReference are required"));
        }
        String event = payload.event.trim();
        if (!"transaction.success".equals(event) && !"transaction.failed".equals(event)) {
            // Only the two documented event types are processed; anything else
            // is acknowledged and ignored rather than guessed at.
            log.warn("Stakaba webhook ignored: unsupported event type {}", event);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "accepted", false,
                    "reason", "unsupported_event")));
        }

        PaymentGatewayService.WebhookOutcome outcome;
        try {
            outcome = paymentGatewayService.handleStakabaEvent(
                    event, payload.internalReference.trim(), payload.grossAmount, payload.currency);
        } catch (PaymentGatewayException ex) {
            // Mandatory verification could not run (provider unreachable,
            // rate-limited, auth failure...). 5xx makes Stakaba redeliver
            // per its documented retry policy; nothing was fulfilled.
            log.warn("Stakaba webhook verification unavailable: code={}", ex.getCode());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Verification temporarily unavailable"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }

        if (!outcome.processed()) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "accepted", false,
                    "reason", outcome.reason() == null ? "ignored" : outcome.reason())));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "accepted", true,
                "paymentId", outcome.paymentId() == null ? "" : outcome.paymentId(),
                "status", outcome.status() == null ? "" : outcome.status(),
                "fulfilled", outcome.fulfilled())));
    }
}
