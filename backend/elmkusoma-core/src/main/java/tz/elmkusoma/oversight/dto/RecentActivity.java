package tz.elmkusoma.oversight.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentActivity {
    private String description;
    private String entityType;
    private java.time.LocalDateTime timestamp;
}
