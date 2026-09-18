package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "career_profiles")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CareerProfile extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "career_objective", columnDefinition = "TEXT")
    private String careerObjective;

    @Column(name = "target_industry", length = 100)
    private String targetIndustry;

    @Column(name = "target_role", length = 150)
    private String targetRole;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "certifications", columnDefinition = "TEXT")
    private String certifications;

    @Column(name = "experience_summary", columnDefinition = "TEXT")
    private String experienceSummary;

    @Column(name = "cv_file_url", length = 500)
    private String cvFileUrl;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "portfolio_url", length = 500)
    private String portfolioUrl;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;
}
