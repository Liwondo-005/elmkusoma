package tz.elmkusoma.nfe.learner.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "nfe_learners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfeLearner extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "participant_number")
    private String participantNumber;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "organization")
    private String organization;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";
}
