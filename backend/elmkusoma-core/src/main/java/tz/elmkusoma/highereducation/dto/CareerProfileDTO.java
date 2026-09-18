package tz.elmkusoma.highereducation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerProfileDTO {
    private UUID id;
    private UUID studentId;
    private String careerObjective;
    private String targetIndustry;
    private String targetRole;
    private String skills;
    private String certifications;
    private String experienceSummary;
    private String cvFileUrl;
    private String linkedinUrl;
    private String portfolioUrl;
    private Boolean isPublic;
    private UUID institutionId;
}
