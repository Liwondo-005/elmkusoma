package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_class_attendance_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassAttendanceDetail extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private UUID liveClassId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "total_seconds")
    private Integer totalSeconds = 0;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage = BigDecimal.ZERO;
}
