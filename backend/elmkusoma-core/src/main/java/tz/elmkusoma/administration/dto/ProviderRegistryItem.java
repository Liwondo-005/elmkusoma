package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderRegistryItem {
    private UUID id;
    private String name;
    private String code;
    private String type;
    private String city;
    private String region;
    private Boolean isActive;
    private String status;
    private String verificationStatus;
    private Integer adminCount;
    private LocalDateTime createdAt;
}
