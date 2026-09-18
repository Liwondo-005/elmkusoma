package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "theses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Thesis extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "research_project_id")
    private UUID researchProjectId;

    @Column(name = "supervisor_id")
    private UUID supervisorId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private ThesisStatus status = ThesisStatus.NOT_STARTED;

    @Column(name = "programme_id")
    private UUID programmeId;

    @Column(name = "submission_date")
    private LocalDate submissionDate;

    @Column(name = "defense_date")
    private LocalDate defenseDate;

    @Column(name = "final_grade", length = 20)
    private String finalGrade;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    @Column(name = "word_count")
    private Integer wordCount;
}
