package tz.elmkusoma.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

/**
 * A titled timestamp on a replay recording.
 *
 * <p>Hangs off {@link Replay} rather than {@code LiveClass}: the player renders the replay
 * row, and replay entitlement is already the rule that decides who may watch. Recording the
 * marker against the class instead would force the player to re-derive that entitlement.</p>
 *
 * <p>{@code positionSeconds} is the seek target and is unique per replay (V151), so two
 * markers can never collide at the same second.</p>
 */
@Entity
@Table(name = "replay_chapters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReplayChapter extends BaseEntity {

    @Column(name = "replay_id", nullable = false)
    private java.util.UUID replayId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** Seek target in seconds from the start of the recording. */
    @Column(name = "position_seconds", nullable = false)
    private Integer positionSeconds;
}
