package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_class_polls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassPoll extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private UUID liveClassId;

    @Column(name = "created_by", nullable = false)
    private UUID teacherId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "options", nullable = false, columnDefinition = "jsonb")
    private String options;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
