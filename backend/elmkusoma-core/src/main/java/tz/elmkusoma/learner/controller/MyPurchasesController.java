package tz.elmkusoma.learner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A learner's own purchases and access.
 *
 * <p>Scoped entirely from the authenticated user: the student record is looked up from the
 * caller's own user id, never from a request parameter. There is therefore no id in the request
 * that a learner could change to read somebody else's spending - the class of bug that turns a
 * payment history into an enumeration of other people's purchases.</p>
 *
 * <p>Figures are reported as stored. A payment that is PENDING is presented as pending and is
 * never counted as paid, and access is shown from the entitlement's own status and expiry rather
 * than from the payment, because access is what the entitlement says - not what the payment
 * promised.</p>
 */
@RestController
@RequestMapping("/v1/my/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','LEARNER','OTHER_LEARNER')")
@Tag(name = "Learner - My Purchases")
public class MyPurchasesController {

    private final StudentRepository studentRepository;
    private final PaymentRepository paymentRepository;
    private final EntitlementRepository entitlementRepository;

    /** One of the caller's own payments. No payer identity is echoed back. */
    public record MyPayment(
            UUID paymentId,
            String receiptNumber,
            String serviceType,
            String status,
            BigDecimal amount,
            String currency,
            BigDecimal refundedAmount,
            String provider,
            LocalDateTime paidAt,
            LocalDateTime createdAt) {

        static MyPayment of(Payment p) {
            return new MyPayment(p.getId(), p.getReceiptNumber(), p.getServiceType(),
                    p.getStatus(), p.getAmount(), p.getCurrency(), p.getRefundedAmount(),
                    p.getProvider(), p.getPaidAt(), p.getCreatedAt());
        }
    }

    /** The caller's own access, with its live status rather than a promise. */
    public record MyAccess(
            UUID entitlementId,
            String serviceType,
            String status,
            LocalDateTime startsAt,
            LocalDateTime expiresAt,
            boolean currentlyValid) {

        static MyAccess of(Entitlement e) {
            boolean valid = "ACTIVE".equals(e.getStatus())
                    && (e.getStartsAt() == null || !e.getStartsAt().isAfter(LocalDateTime.now()))
                    && (e.getExpiresAt() == null || e.getExpiresAt().isAfter(LocalDateTime.now()));
            return new MyAccess(e.getId(), e.getServiceType(), e.getStatus(),
                    e.getStartsAt(), e.getExpiresAt(), valid);
        }
    }

    @GetMapping
    @Operation(summary = "The caller's own payment history")
    public ResponseEntity<ApiResponse<List<MyPayment>>> myPayments(HttpServletRequest request) {
        Student student = requireOwnStudent(request);
        List<MyPayment> payments = paymentRepository
                .findByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(student.getId())
                .stream()
                .map(MyPayment::of)
                .sorted(Comparator.comparing(
                        (MyPayment p) -> p.createdAt() == null ? LocalDateTime.MIN : p.createdAt())
                        .reversed())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/access")
    @Operation(summary = "The caller's own active and expired access")
    public ResponseEntity<ApiResponse<List<MyAccess>>> myAccess(HttpServletRequest request) {
        Student student = requireOwnStudent(request);
        List<MyAccess> access = entitlementRepository
                .findByStudentIdAndIsDeletedFalse(student.getId())
                .stream()
                .map(MyAccess::of)
                .sorted(Comparator.comparing(MyAccess::expiresAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(access));
    }

    /**
     * Resolves the caller to their own student record.
     *
     * <p>Fails closed: a user with no student record gets a 404 rather than an empty list,
     * because "you have no purchases" and "we could not find who you are" are different answers
     * and only the second one is worth reporting.</p>
     */
    private Student requireOwnStudent(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (!(userIdAttr instanceof UUID userId)) {
            throw new IllegalStateException("Authenticated user id is missing from the request");
        }
        return studentRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException(
                        "Student", "userId", userId));
    }
}