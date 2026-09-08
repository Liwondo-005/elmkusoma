package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private UUID institutionId;
    private Long totalStudents;
    private Long totalTeachers;
    private Long totalParents;
    private Long activeStudents;
    private Long certificatesIssued;
    private Long pendingImportJobs;
    private Map<String, Object> additionalStats;
}
