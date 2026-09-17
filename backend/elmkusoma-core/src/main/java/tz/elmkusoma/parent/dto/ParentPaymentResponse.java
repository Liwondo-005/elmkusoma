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
}
