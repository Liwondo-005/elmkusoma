package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ServiceCreateRequest {
    @NotBlank(message = "Service name is required")
    private String name;

    @NotBlank(message = "Service code is required")
    private String code;

    private String description;

    @NotBlank(message = "Service category is required")
    private String category;

    private Boolean requiresVerification;
    private Integer maxSeats;

    @DecimalMin(value = "0.00", message = "Price must not be negative")
    private BigDecimal monthlyPrice;

    private String currency;
    private Boolean isActive;
}
