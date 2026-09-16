package tz.elmkusoma.oversight.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopRegionStats {
    private String regionName;
    private String regionCode;
    private Long institutionCount;
    private Long teacherCount;
    private Long studentCount;
}
