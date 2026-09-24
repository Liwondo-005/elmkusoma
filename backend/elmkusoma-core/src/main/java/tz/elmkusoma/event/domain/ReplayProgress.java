package tz.elmkusoma.event.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "replay_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uq_replay_progress_user", columnNames = {"replay_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReplayProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "replay_id", nullable = false)
    private UUID replayId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "position_seconds", nullable = false)
    private Integer positionSeconds = 0;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
