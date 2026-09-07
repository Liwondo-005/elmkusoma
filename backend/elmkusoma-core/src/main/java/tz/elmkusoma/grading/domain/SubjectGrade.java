package tz.elmkusoma.grading.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subject_grades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectGrade extends BaseEntity {

    @Column(name = "report_card_id", nullable = false)
    private UUID reportCardId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "marks_obtained")
    private BigDecimal marksObtained;

    @Column(name = "grade")
    private String grade;

    @Column(name = "grade_points")
    private BigDecimal gradePoints;

    @Column(name = "teacher_remarks")
    private String teacherRemarks;

    @Column(name = "assessed_by")
    private UUID assessedBy;

    @Column(name = "assessed_at")
    private LocalDateTime assessedAt;
}