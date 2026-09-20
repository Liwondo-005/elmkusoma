package tz.elmkusoma.parent.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class InitiatePaymentRequest {
    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "10000000", message = "Amount must not exceed 10,000,000 TZS")
    private BigDecimal amount;

    @NotBlank(message = "Service type is required")
    private String serviceType;

    private UUID serviceId;
    private String description;
}
