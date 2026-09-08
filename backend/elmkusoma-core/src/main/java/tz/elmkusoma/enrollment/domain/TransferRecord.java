package tz.elmkusoma.enrollment.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfer_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRecord extends BaseEntity {

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "from_class_group_id", nullable = false)
    private UUID fromClassGroupId;

    @Column(name = "to_class_group_id", nullable = false)
    private UUID toClassGroupId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "transferred_at", nullable = false)
    private LocalDateTime transferredAt;

    @Column(name = "transferred_by", nullable = false)
    private UUID transferredBy;
}
