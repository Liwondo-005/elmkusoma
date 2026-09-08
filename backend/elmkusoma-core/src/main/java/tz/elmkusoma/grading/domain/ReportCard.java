package tz.elmkusoma.grading.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReportCard extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "term_id", nullable = false)
    private UUID termId;

    @Column(name = "grading_scale_id", nullable = false)
    private UUID gradingScaleId;

    @Column(name = "total_marks")
    private BigDecimal totalMarks;

    @Column(name = "average_mark")
    private BigDecimal averageMark;

    @Column(name = "overall_grade")
    private String overallGrade;

    @Column(name = "gpa")
    private BigDecimal gpa;

    @Column(name = "class_rank")
    private Integer classRank;

    @Column(name = "total_students_in_class")
    private Integer totalStudentsInClass;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportCardStatus status = ReportCardStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public enum ReportCardStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }
}