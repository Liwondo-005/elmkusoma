package tz.elmkusoma.administration.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ServiceCreateRequest {
    private String name;
    private String code;
    private String description;
    private String category;
    private Boolean requiresVerification;
    private Integer maxSeats;
    private BigDecimal monthlyPrice;
    private String currency;
    private Boolean isActive;
}
