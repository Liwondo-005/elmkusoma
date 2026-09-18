package tz.elmkusoma.oversight.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionSummary {
    private UUID id;
    private String name;
    private String code;
    private String type;
    private String districtName;
    private String regionName;
    private Long teacherCount;
    private Long studentCount;
    private Boolean isActive;
}
