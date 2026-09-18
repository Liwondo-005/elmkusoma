package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "research_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ResearchProject extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "research_question", columnDefinition = "TEXT")
    private String researchQuestion;

    @Column(name = "objectives", columnDefinition = "TEXT")
    private String objectives;

    @Column(name = "supervisor_id")
    private UUID supervisorId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private ResearchStatus status = ResearchStatus.IDEA;

    @Column(name = "programme_id")
    private UUID programmeId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "methodology", columnDefinition = "TEXT")
    private String methodology;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    @Column(name = "keywords", length = 500)
    private String keywords;
}
