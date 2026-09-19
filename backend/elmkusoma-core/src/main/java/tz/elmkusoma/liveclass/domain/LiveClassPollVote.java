package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_class_poll_votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassPollVote extends BaseEntity {

    @Column(name = "poll_id", nullable = false)
    private UUID pollId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "option_index", nullable = false)
    private Integer optionIndex;

    @Column(name = "voted_at")
    private LocalDateTime votedAt;
}
