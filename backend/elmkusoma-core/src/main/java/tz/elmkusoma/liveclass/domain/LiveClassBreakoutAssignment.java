package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_class_breakout_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassBreakoutAssignment extends BaseEntity {

    @Column(name = "breakout_room_id", nullable = false)
    private UUID breakoutRoomId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
}
