package tz.elmkusoma.oversight.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionResponse {
    private UUID id;
    private String name;
    private String code;
    private Boolean isActive;
    private Long institutionCount;
    private Long teacherCount;
    private Long studentCount;
}
