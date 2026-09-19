package tz.elmkusoma.nursery.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "nursery_feelings_checkin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NurseryFeelingsCheckin extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "feeling", nullable = false)
    @Enumerated(EnumType.STRING)
    private Feeling feeling;

    @Column(name = "emoji")
    private String emoji;

    @Column(name = "note")
    private String note;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    public enum Feeling {
        HAPPY, SAD, EXCITED, CALM, WORRIED, ANGRY, PROUD, TIRED
    }
}
