package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_class_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"live_class_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassParticipant extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private UUID liveClassId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role", nullable = false, length = 20)
    private String role = "LEARNER";

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "connection_id", length = 100)
    private String connectionId;

    @Column(name = "hand_raised_at")
    private LocalDateTime handRaisedAt;

    @Column(name = "hand_raise_order")
    private Integer handRaiseOrder;

    public static final String ROLE_LEARNER = "LEARNER";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_OBSERVER = "OBSERVER";
}