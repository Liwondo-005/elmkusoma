package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "secondary_error_bank")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SecondaryErrorBank extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "error_title", nullable = false)
    private String errorTitle;

    @Column(name = "error_description", nullable = false, columnDefinition = "TEXT")
    private String errorDescription;

    @Column(name = "incorrect_example", columnDefinition = "TEXT")
    private String incorrectExample;

    @Column(name = "correct_example", columnDefinition = "TEXT")
    private String correctExample;

    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "category")
    private String category;

    @Column(name = "frequency")
    @Enumerated(EnumType.STRING)
    private Frequency frequency = Frequency.COMMON;

    public enum Frequency {
        COMMON, OCCASIONAL, RARE
    }
}
