package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DelegationExtendRequest {
    private LocalDateTime expiresAt;
    private String reason;
}
