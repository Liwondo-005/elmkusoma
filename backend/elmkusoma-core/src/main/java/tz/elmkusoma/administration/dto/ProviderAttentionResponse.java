package tz.elmkusoma.administration.dto;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderAttentionResponse {
    private UUID providerId;
    private String providerName;
    private String severity;
    private String category;
    private String title;
    private String description;
    private String actionUrl;
}
