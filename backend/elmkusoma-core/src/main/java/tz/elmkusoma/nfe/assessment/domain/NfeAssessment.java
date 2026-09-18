package tz.elmkusoma.nfe.assessment.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nfe_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfeAssessment extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "program_id")
    private UUID programId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "assessment_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AssessmentType assessmentType;

    @Column(name = "total_marks")
    private Integer totalMarks;

    @Column(name = "pass_marks")
    private Integer passMarks;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Builder.Default
    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    public enum AssessmentType {
        QUIZ, EXAM, SURVEY, FEEDBACK
    }
}
