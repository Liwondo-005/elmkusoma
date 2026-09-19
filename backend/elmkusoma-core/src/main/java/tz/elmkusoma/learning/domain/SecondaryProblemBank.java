package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "secondary_problem_bank")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SecondaryProblemBank extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "problem_title", nullable = false)
    private String problemTitle;

    @Column(name = "problem_description", nullable = false, columnDefinition = "TEXT")
    private String problemDescription;

    @Column(name = "problem_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProblemType problemType;

    @Column(name = "options", columnDefinition = "TEXT")
    private String options;

    @Column(name = "correct_answer", nullable = false, columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "solution", columnDefinition = "TEXT")
    private String solution;

    @Column(name = "difficulty_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel = DifficultyLevel.BASIC;

    @Column(name = "marks")
    private Integer marks = 1;

    public enum ProblemType {
        MCQ, SHORT_ANSWER, LONG_ANSWER, NUMERICAL, TRUE_FALSE
    }

    public enum DifficultyLevel {
        BASIC, INTERMEDIATE, ADVANCED
    }
}
