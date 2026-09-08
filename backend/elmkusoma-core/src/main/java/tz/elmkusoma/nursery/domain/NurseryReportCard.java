package tz.elmkusoma.nursery.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nursery_report_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseryReportCard extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "term_id", nullable = false)
    private UUID termId;

    @Column(name = "general_remarks")
    private String generalRemarks;

    @Column(name = "teacher_comments")
    private String teacherComments;

    @Column(name = "physical_development")
    private String physicalDevelopment;

    @Column(name = "cognitive_development")
    private String cognitiveDevelopment;

    @Column(name = "social_development")
    private String socialDevelopment;

    @Column(name = "emotional_development")
    private String emotionalDevelopment;

    @Column(name = "language_development")
    private String languageDevelopment;

    @Column(name = "areas_of_strength")
    private String areasOfStrength;

    @Column(name = "areas_for_improvement")
    private String areasForImprovement;

    @Column(name = "recommendations_for_parents")
    private String recommendationsForParents;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportCardStatus status = ReportCardStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "prepared_by", nullable = false)
    private UUID preparedBy;

    public enum ReportCardStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }
}