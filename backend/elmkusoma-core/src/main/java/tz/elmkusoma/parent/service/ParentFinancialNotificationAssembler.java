package tz.elmkusoma.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tz.elmkusoma.parent.domain.ParentNotificationPreference;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.dto.response.ParentNotificationResponse;
import tz.elmkusoma.parent.repository.ParentNotificationPreferenceRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Builds the parent's financial notifications from the records that actually
 * happened, rather than storing a second copy of them.
 *
 * <p>The inbox this plugs into is synthesised on read: nothing is persisted, so a
 * "notification" is really a projection of a payment that is already recorded.
 * That is why this reads {@link Payment} directly. Deriving the inbox from the
 * payment table rather than from a notification table means a duplicated callback,
 * a retried webhook or a re-run of the reconciliation job cannot manufacture a
 * second, subtly different alert - there is only ever one payment to project.</p>
 *
 * <p>Three things this deliberately does not do:</p>
 *
 * <ul>
 *   <li><b>It does not persist anything.</b> No inbox table, no read/unread flags,
 *       no delivery infrastructure. The surrounding inbox already derives its
 *       unread count this way, and introducing storage here would fork that
 *       convention and immediately drift from it.</li>
 *   <li><b>It does not decide who may see what.</b> Authorisation is the caller's
 *       job and happens before this is reached; passing a {@code parentId} is
 *       itself the ownership check, because every query is scoped by it.</li>
 *   <li><b>It does not send anything.</b> Email, SMS and push are separate
 *       concerns governed by the same preference record, not something a read
 *       path may quietly trigger.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParentFinancialNotificationAssembler {

    private static final String TYPE_PAYMENT = "PAYMENT";
    private static final String TYPE_RECEIPT = "RECEIPT";
    private static final String TYPE_REFUND = "REFUND";
    private static final String PRIORITY_HIGH = "HIGH";
    private static final String PRIORITY_NORMAL = "NORMAL";

    private final PaymentRepository paymentRepository;
    private final ParentNotificationPreferenceRepository preferenceRepository;

    /**
     * Projects this parent's financial records into inbox items.
     *
     * @param parentId  the paying account; scopes every read, so one parent can
     *                  never see another's money even if a caller misuses this
     * @param studentId optional child filter, or {@code null} for all children
     * @param childName display name for the given child, or a general label
     */
    public List<ParentNotificationResponse.NotificationItem> assemble(
            UUID parentId, UUID studentId, String childName) {
        if (parentId == null) {
            return List.of();
        }
        if (!financialAlertsEnabled(parentId)) {
            return List.of();
        }
        String label = (childName == null || childName.isBlank()) ? "your child" : childName;

        List<Payment> payments = studentId == null
                ? paymentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(parentId)
                : paymentRepository
                        .findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(parentId).stream()
                        .filter(p -> studentId.equals(p.getStudentId()))
                        .toList();

        List<ParentNotificationResponse.NotificationItem> items = new ArrayList<>();
        for (Payment payment : payments) {
            addPaymentItem(items, payment, label);
            addReceiptItem(items, payment, label);
            addRefundItem(items, payment, label);
        }

        items.sort(Comparator
                .comparing((ParentNotificationResponse.NotificationItem i) ->
                        PRIORITY_HIGH.equals(i.getPriority()) ? 0 : 1)
                .thenComparing(ParentNotificationResponse.NotificationItem::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    /**
     * A completed payment is worth telling the payer about; a failed or pending
     * one is worth telling them about more, because it is money that did not move
     * as they expected.
     */
    private void addPaymentItem(List<ParentNotificationResponse.NotificationItem> items,
                                Payment payment, String label) {
        String status = payment.getStatus();
        if (status == null || "PENDING".equals(status)) {
            return;
        }
        boolean completed = "COMPLETED".equals(status);
        items.add(item(
                "payment-" + payment.getId(),
                TYPE_PAYMENT,
                completed ? "Payment Completed" : "Payment " + titleCase(status),
                completed
                        ? String.format("%s paid %s %s for %s", label, money(payment), payment.getCurrency(), service(payment))
                        : String.format("%s payment of %s %s is %s", label, money(payment), payment.getCurrency(), status.toLowerCase()),
                label,
                payment.getCreatedAt(),
                completed ? PRIORITY_NORMAL : PRIORITY_HIGH));
    }

    /**
     * Only a settled payment produces a receipt. The id is derived from the
     * payment, not from a generated value, so a receipt regenerated after a retry
     * collapses onto the same inbox item instead of appearing twice.
     */
    private void addReceiptItem(List<ParentNotificationResponse.NotificationItem> items,
                                Payment payment, String label) {
        if (!"COMPLETED".equals(payment.getStatus()) || payment.getReceiptNumber() == null) {
            return;
        }
        items.add(item(
                "receipt-" + payment.getId(),
                TYPE_RECEIPT,
                "Receipt Available",
                String.format("Receipt %s for %s %s is ready to view",
                        payment.getReceiptNumber(), money(payment), payment.getCurrency()),
                label,
                payment.getCreatedAt(),
                PRIORITY_NORMAL));
    }

    /**
     * A refund is reported separately from the payment it reverses, so a payer who
     * was charged and then refunded is not left with a paid-looking receipt and a
     * silent reversal.
     */
    private void addRefundItem(List<ParentNotificationResponse.NotificationItem> items,
                               Payment payment, String label) {
        if (!payment.isFullyRefunded()) {
            return;
        }
        items.add(item(
                "refund-" + payment.getId(),
                TYPE_REFUND,
                "Payment Refunded",
                String.format("%s was refunded for %s %s",
                        label, money(payment), payment.getCurrency()),
                label,
                payment.getCreatedAt(),
                PRIORITY_HIGH));
    }

    /**
     * Honours the parent's own preference. An absent record means the parent has
     * never customised anything, which matches the entity's own defaults, so
     * financial alerts are shown rather than silently dropped.
     */
    private boolean financialAlertsEnabled(UUID parentId) {
        try {
            ParentNotificationPreference preference = preferenceRepository.findByParentId(parentId)
                    .orElse(null);
            return preference == null
                    || !Boolean.FALSE.equals(preference.getFeeAlerts());
        } catch (RuntimeException ex) {
            log.warn("Could not read notification preference for parent {}; showing financial "
                    + "alerts using the default", parentId, ex);
            return true;
        }
    }

    private ParentNotificationResponse.NotificationItem item(
            String id, String type, String title, String detail,
            String childName, LocalDateTime createdAt, String priority) {
        return ParentNotificationResponse.NotificationItem.builder()
                .id(id)
                .type(type)
                .title(title)
                .detail(detail)
                .childName(childName)
                .createdAt(createdAt)
                .isRead(false)
                .priority(priority)
                .build();
    }

    private String money(Payment payment) {
        return payment.getAmount() == null ? "0.00" : payment.getAmount().toPlainString();
    }

    private String service(Payment payment) {
        return payment.getServiceType() == null ? "a service" : payment.getServiceType();
    }

    private String titleCase(String value) {
        return value.isEmpty() ? value
                : Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }
}