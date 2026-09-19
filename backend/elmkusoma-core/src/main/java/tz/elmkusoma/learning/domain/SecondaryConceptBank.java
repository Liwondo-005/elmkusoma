package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "secondary_concept_bank")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SecondaryConceptBank extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "concept_name", nullable = false)
    private String conceptName;

    @Column(name = "concept_description", nullable = false, columnDefinition = "TEXT")
    private String conceptDescription;

    @Column(name = "examples", columnDefinition = "TEXT")
    private String examples;

    @Column(name = "related_concepts")
    private String relatedConcepts;

    @Column(name = "difficulty_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel = DifficultyLevel.BASIC;

    @Column(name = "category")
    private String category;

    public enum DifficultyLevel {
        BASIC, INTERMEDIATE, ADVANCED
    }
}
