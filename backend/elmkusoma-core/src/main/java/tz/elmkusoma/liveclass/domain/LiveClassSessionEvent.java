package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

@Entity
@Table(name = "live_class_session_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveClassSessionEvent extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private java.util.UUID liveClassId;

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_data")
    private String eventData;
}
