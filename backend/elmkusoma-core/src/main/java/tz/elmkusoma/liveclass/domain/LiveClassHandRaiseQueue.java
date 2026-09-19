package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_class_hand_raise_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassHandRaiseQueue extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private UUID liveClassId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "raised_at")
    private LocalDateTime raisedAt;

    @Column(name = "lowered_at")
    private LocalDateTime loweredAt;

    @Column(name = "position")
    private Integer position;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
