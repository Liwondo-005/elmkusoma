package tz.elmkusoma.administration.dto;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DelegationDecisionRequest {
    private UUID decidedBy;
    private String reason;
}
