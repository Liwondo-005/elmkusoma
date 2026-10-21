package tz.elmkusoma.parent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentPaymentResponse {

    private BigDecimal outstanding;
    private BigDecimal paidThisTerm;
    private List<PaymentItem> recentPayments;
    private List<PaymentItem> pendingPayments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentItem {
        private String id;
        private BigDecimal amount;
        private String currency;
        private String description;
        private String serviceType;
        private String status;
        private LocalDateTime paidAt;
        private LocalDateTime createdAt;
        private String providerReference;
    }

    /**
     * One step of a payment's status timeline (Payment.md section 29).
     *
     * <p>Every field is a fact that was recorded when it happened: {@code at} is the stored
     * timestamp of the audit row or of the payment column itself, and the statuses are the
     * values that audit row carried. The service never infers a step that was not written
     * down, so a payment with no recorded history yields only its own creation moment rather
     * than a plausible-looking sequence.</p>
     *
     * <p>{@code source} names where the fact came from - {@code PAYMENT} for the payment row,
     * {@code AUDIT} for an audit entry - so a caller can tell a stored timestamp from a
     * recorded transition without having to trust the ordering.</p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimelineEvent {
        /** When this step happened, taken from the stored timestamp. */
        private LocalDateTime at;
        /** {@code CREATED}, {@code STATUS} or {@code SETTLED}. */
        private String kind;
        /** The status the payment was in before, or {@code null} when unknown. */
        private String fromStatus;
        /** The status after this step, or {@code null} when the row recorded none. */
        private String toStatus;
        /** {@code PAYMENT} or {@code AUDIT}. */
        private String source;
    }
}
