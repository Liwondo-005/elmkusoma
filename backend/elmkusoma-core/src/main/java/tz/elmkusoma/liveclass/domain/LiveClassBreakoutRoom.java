package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "live_class_breakout_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassBreakoutRoom extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private UUID liveClassId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "max_participants")
    private Integer maxParticipants = 10;

    @Column(name = "status", length = 20)
    private String status = "WAITING";
}
