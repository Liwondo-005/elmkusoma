package tz.elmkusoma.parent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.PaymentReceipt;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.parent.repository.PaymentReceiptRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.service.ReceiptService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Receipt reads.
 *
 * <p>A receipt documents money that was actually received, so it is only ever returned for a
 * COMPLETED payment. Access is ownership-checked on the payment: knowing or guessing a receipt
 * number is not authorisation, and a parent may only read receipts for payments they made.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Receipts")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final PaymentRepository paymentRepository;
    private final ParentRepository parentRepository;

    @GetMapping("/v1/parents/payments/{paymentId}/receipt")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    @Operation(summary = "The receipt for one of your payments")
    public ResponseEntity<ApiResponse<PaymentReceipt>> forPayment(
            @PathVariable UUID paymentId, HttpServletRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        requireOwnsOrAdmin(request, payment.getParentId());

        PaymentReceipt receipt = receiptService.findByPayment(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No receipt has been issued for this payment"));
        return ResponseEntity.ok(ApiResponse.success(receipt));
    }

    @GetMapping("/v1/parents/receipts/{receiptNumber}")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    @Operation(summary = "Look up a receipt by its number")
    public ResponseEntity<ApiResponse<PaymentReceipt>> byNumber(
            @PathVariable String receiptNumber, HttpServletRequest request) {
        PaymentReceipt receipt = paymentReceiptRepository
                .findByReceiptNumberAndIsDeletedFalse(receiptNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));
        requireOwnsOrAdmin(request, receipt.getParentId());
        return ResponseEntity.ok(ApiResponse.success(receipt));
    }

    @GetMapping("/v1/parents/receipts")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    @Operation(summary = "All receipts you have been issued")
    public ResponseEntity<ApiResponse<List<PaymentReceipt>>> mine(HttpServletRequest request) {
        UUID userId = currentUserId(request);
        // Both identifier spaces, because a parent asking what they have bought expects both: the
        // dashboard payments are keyed on their user id and their event tickets on their parent
        // record id. Filtering on the first alone would return a list that looks complete while
        // quietly omitting every ticket they paid for.
        Set<UUID> ownerIds = new LinkedHashSet<>();
        ownerIds.add(userId);
        parentRepository.findByUserIdAndIsDeletedFalse(userId)
                .ifPresent(parent -> ownerIds.add(parent.getId()));
        return ResponseEntity.ok(ApiResponse.success(
                paymentReceiptRepository.findByParentIdInAndIsDeletedFalseOrderByPaidAtDesc(
                        ownerIds)));
    }

    private void requireOwnsOrAdmin(HttpServletRequest request, UUID ownerUserId) {
        UUID userId = currentUserId(request);
        if (request.isUserInRole("ADMIN")) {
            return;
        }
        if (ownerUserId != null) {
            if (userId.equals(ownerUserId)) {
                return;
            }
            // Two identifier spaces reach this column and neither alone answers "is this yours".
            // A payment raised from the parent dashboard records the parent's user id; an event
            // ticket records the parent record's own id. Comparing only against one of them made
            // a parent who had paid for a ticket a legitimate owner who was still refused their
            // own receipt, so the parent record is resolved and compared as well - the same
            // double check cancelPayment already performs for the same column.
            boolean ownsParentRecord = parentRepository.findByUserIdAndIsDeletedFalse(userId)
                    .map(parent -> parent.getId().equals(ownerUserId))
                    .orElse(false);
            if (ownsParentRecord) {
                return;
            }
        }
        // A payment with no parent record at all belongs to a staff or teacher ticket and is
        // readable by the platform admin and nobody else.
        throw new org.springframework.security.access.AccessDeniedException(
                "You may only read your own receipts");
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
