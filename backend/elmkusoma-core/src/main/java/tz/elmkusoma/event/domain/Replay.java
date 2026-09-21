package tz.elmkusoma.event.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "replays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Replay extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "live_session_id")
    private UUID liveSessionId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "recording_url")
    private String recordingUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "last_position_seconds", nullable = false)
    private Integer lastPositionSeconds = 0;
}
