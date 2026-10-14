package tz.elmkusoma.parent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.RefundRequest;
import tz.elmkusoma.parent.dto.request.RefundRequestRequest;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.service.RefundService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Refund request and decision endpoints.
 *
 * <p>The two sides are deliberately separated. A parent may <b>request</b> a refund against their
 * own payment and may not decide one; an admin may decide. A role alone is not enough on either
 * side - the parent must actually own the payment - so manipulating a payment id in the path
 * gets nothing.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Refunds")
public class RefundController {

    private final RefundService refundService;
    private final PaymentRepository paymentRepository;
    private final ParentRepository parentRepository;

    // ------------------------------------------------------------------
    // Parent: request and track
    // ------------------------------------------------------------------

    @PostMapping("/v1/parents/payments/{paymentId}/refund-requests")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    @Operation(summary = "Request a refund against your own payment")
    public ResponseEntity<ApiResponse<RefundRequest>> requestRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequestRequest body,
            HttpServletRequest request) {
        UUID userId = currentUserId(request);
        Payment payment = requirePayment(paymentId);
        requireOwnsPayment(userId, payment);

        RefundRequest created = refundService.requestRefund(
                paymentId, userId, body.getAmount(), body.getReason(), body.getEvidenceUrl());
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping("/v1/parents/payments/{paymentId}/refund-requests")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    @Operation(summary = "List refund requests against your own payment")
    public ResponseEntity<ApiResponse<List<RefundRequest>>> listForPayment(
            @PathVariable UUID paymentId, HttpServletRequest request) {
        requireOwnsPayment(currentUserId(request), requirePayment(paymentId));
        return ResponseEntity.ok(ApiResponse.success(refundService.forPayment(paymentId)));
    }

    @GetMapping("/v1/parents/refund-requests/{refundId}")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    @Operation(summary = "Read one refund request you are party to")
    public ResponseEntity<ApiResponse<RefundRequest>> get(
            @PathVariable UUID refundId, HttpServletRequest request) {
        RefundRequest refund = refundService.find(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found"));
        Payment payment = requirePayment(refund.getPaymentId());
        UUID userId = currentUserId(request);
        boolean party = userId.equals(refund.getRequestedBy())
                || userId.equals(refund.getDecidedBy())
                || userId.equals(payment.getParentId());
        if (!party) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not a party to this refund request");
        }
        return ResponseEntity.ok(ApiResponse.success(refund));
    }

    // ------------------------------------------------------------------
    // Admin: decide and complete
    // ------------------------------------------------------------------

    @GetMapping("/v1/platform-admin/refunds/open")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Open refund requests awaiting a decision")
    public ResponseEntity<ApiResponse<List<RefundRequest>>> openRefunds() {
        return ResponseEntity.ok(ApiResponse.success(refundService.openRequests()));
    }

    @PostMapping("/v1/platform-admin/refunds/{refundId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundRequest>> submitForReview(
            @PathVariable UUID refundId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                refundService.submitForReview(refundId, currentUserId(request))));
    }

    @PostMapping("/v1/platform-admin/refunds/{refundId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a refund. Approval is a decision, not money returned.")
    public ResponseEntity<ApiResponse<RefundRequest>> approve(
            @PathVariable UUID refundId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        boolean revoke = body != null && Boolean.TRUE.equals(body.get("revokeEntitlement"));
        String note = body == null ? null : String.valueOf(body.get("note"));
        return ResponseEntity.ok(ApiResponse.success(
                refundService.approve(refundId, currentUserId(request), note, revoke)));
    }

    @PostMapping("/v1/platform-admin/refunds/{refundId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundRequest>> reject(
            @PathVariable UUID refundId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        String note = body == null ? null : String.valueOf(body.get("note"));
        return ResponseEntity.ok(ApiResponse.success(
                refundService.reject(refundId, currentUserId(request), note)));
    }

    @PostMapping("/v1/platform-admin/refunds/{refundId}/processing")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Record that a refund was dispatched to the gateway. Money still has not moved.")
    public ResponseEntity<ApiResponse<RefundRequest>> markProcessing(
            @PathVariable UUID refundId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        String reference = body == null ? null : body.get("refundReference");
        return ResponseEntity.ok(ApiResponse.success(
                refundService.markProcessing(refundId, currentUserId(request), reference)));
    }

    @PostMapping("/v1/platform-admin/refunds/{refundId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete a refund: the only action that represents money actually returned.")
    public ResponseEntity<ApiResponse<RefundRequest>> complete(
            @PathVariable UUID refundId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        String reference = body == null ? null : body.get("refundReference");
        return ResponseEntity.ok(ApiResponse.success(
                refundService.completeRefund(refundId, currentUserId(request), reference)));
    }

    @PostMapping("/v1/platform-admin/refunds/{refundId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundRequest>> cancel(
            @PathVariable UUID refundId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                refundService.cancel(refundId, currentUserId(request))));
    }

    // ------------------------------------------------------------------

    private Payment requirePayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    /**
     * The parent-child relationship is checked in the backend, never trusted from a selected
     * child id in the request. Owning the payment is what authorises the request.
     */
    private void requireOwnsPayment(UUID userId, Payment payment) {
        boolean owns = userId.equals(payment.getParentId())
                || parentRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                .anyMatch(p -> p.getId().equals(payment.getParentId()));
        if (!owns) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You may only request a refund against your own payment");
        }
    }

    private UUID currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof UUID uuid) {
            return uuid;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "An authenticated user is required");
    }
}
