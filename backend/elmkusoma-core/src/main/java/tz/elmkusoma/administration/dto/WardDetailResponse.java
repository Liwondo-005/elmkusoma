package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardDetailResponse {
    private UUID id;
    private String name;
    private String code;
    private Boolean isActive;
    private UUID districtId;
    private String districtName;
    private String districtCode;
    private UUID regionId;
    private String regionName;
    private String regionCode;
    private long institutionCount;
    private List<tz.elmkusoma.oversight.dto.InstitutionSummary> institutions;
}
