package tz.elmkusoma.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.dto.ParentPaymentResponse;
import tz.elmkusoma.parent.dto.ParentPaymentResponse.PaymentItem;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentPaymentService {

    private final PaymentRepository paymentRepository;
    private final EntitlementRepository entitlementRepository;

    public ParentPaymentResponse getPayments(UUID parentId) {
        List<Payment> allPayments = paymentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(parentId);
        List<Payment> completedPayments = allPayments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .collect(Collectors.toList());

        BigDecimal totalPaid = completedPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> pendingPayments = allPayments.stream()
                .filter(p -> "PENDING".equals(p.getStatus()) || "PROCESSING".equals(p.getStatus()))
                .collect(Collectors.toList());

        return ParentPaymentResponse.builder()
                .outstanding(pendingPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .paidThisTerm(totalPaid)
                .recentPayments(completedPayments.stream().limit(10).map(this::toPaymentItem).collect(Collectors.toList()))
                .pendingPayments(pendingPayments.stream().map(this::toPaymentItem).collect(Collectors.toList()))
                .build();
    }

    public List<PaymentItem> getPaymentsByStudent(UUID studentId) {
        return paymentRepository.findByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId)
                .stream().map(this::toPaymentItem).collect(Collectors.toList());
    }

    @Transactional
    public Payment initiatePayment(UUID parentId, UUID studentId, UUID institutionId,
                                   BigDecimal amount, String serviceType, UUID serviceId,
                                   String description) {
        Payment payment = Payment.builder()
                .parentId(parentId)
                .studentId(studentId)
                .institutionId(institutionId)
                .amount(amount)
                .currency("TZS")
                .serviceType(serviceType)
                .serviceId(serviceId)
                .description(description)
                .status("PENDING")
                .build();
        payment = paymentRepository.save(payment);
        log.info("Payment initiated: {} for student {} amount {} {}",
                payment.getId(), studentId, amount, serviceType);
        return payment;
    }

    @Transactional
    public Payment verifyPayment(UUID paymentId, String providerReference) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if ("COMPLETED".equals(payment.getStatus())) {
            log.warn("Duplicate callback for payment {}", paymentId);
            return payment;
        }

        payment.setProviderReference(providerReference);
        payment.setProvider("manual");
        payment.setStatus("COMPLETED");
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        grantEntitlement(payment);
        log.info("Payment verified and entitlement granted: {}", paymentId);
        return payment;
    }

    private void grantEntitlement(Payment payment) {
        Entitlement existing = entitlementRepository
                .findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                        payment.getStudentId(), payment.getServiceType(), payment.getServiceId())
                .orElse(null);

        if (existing != null) {
            existing.setExpiresAt(LocalDateTime.now().plusDays(30));
            existing.setStatus("ACTIVE");
            existing.setPaymentId(payment.getId());
            entitlementRepository.save(existing);
        } else {
            Entitlement entitlement = Entitlement.builder()
                    .userId(payment.getParentId())
                    .studentId(payment.getStudentId())
                    .institutionId(payment.getInstitutionId())
                    .serviceType(payment.getServiceType())
                    .serviceId(payment.getServiceId())
                    .paymentId(payment.getId())
                    .status("ACTIVE")
                    .startsAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();
            entitlementRepository.save(entitlement);
        }
    }

    public boolean hasEntitlement(UUID studentId, String serviceType, UUID serviceId) {
        return entitlementRepository
                .existsByStudentIdAndServiceTypeAndServiceIdAndStatusAndIsDeletedFalse(
                        studentId, serviceType, serviceId, "ACTIVE");
    }

    private PaymentItem toPaymentItem(Payment p) {
        return PaymentItem.builder()
                .id(p.getId().toString())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .description(p.getDescription())
                .serviceType(p.getServiceType())
                .status(p.getStatus())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .providerReference(p.getProviderReference())
                .build();
    }
}
