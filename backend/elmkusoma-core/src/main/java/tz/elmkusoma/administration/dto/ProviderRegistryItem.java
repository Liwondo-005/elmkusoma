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

    /**
     * Ecosystem link (audit X-5): the registry previously aggregated only institutions +
     * verifications, so platform oversight could not see whether a provider actually delivers.
     * These counters are real institution-scoped counts from the nfe_* tables.
     */
private Integer programs;
private Integer learners;
private Integer sessions;
private Integer certificates;
private Boolean providerRecordPresent;
private Boolean providerVerified;

private LocalDateTime createdAt;
}
