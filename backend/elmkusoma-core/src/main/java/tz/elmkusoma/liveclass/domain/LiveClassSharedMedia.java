package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_class_shared_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassSharedMedia extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private UUID liveClassId;

    @Column(name = "shared_by", nullable = false)
    private UUID sharedBy;

    @Column(name = "media_type", nullable = false, length = 50)
    private String mediaType;

    @Column(name = "title", length = 300)
    private String title;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "shared_at")
    private LocalDateTime sharedAt;
}
