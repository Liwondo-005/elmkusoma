package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "live_class_quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassQuiz extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private UUID liveClassId;

    @Column(name = "created_by", nullable = false)
    private UUID teacherId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "status", length = 20)
    private String status = "DRAFT";
}
