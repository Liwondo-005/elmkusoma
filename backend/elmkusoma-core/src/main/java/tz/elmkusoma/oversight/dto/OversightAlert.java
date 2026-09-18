package tz.elmkusoma.oversight.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OversightAlert {
    private String id;
    private String type;
    private String message;
    private String severity;
    private String institutionName;
    private String institutionId;
    private LocalDateTime timestamp;
}