package tz.elmkusoma.nfe.program.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nfe_programs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfeProgram extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "program_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProgramType programType;

    @Column(name = "category")
    private String category;

    @Column(name = "target_audience")
    private String targetAudience;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Builder.Default
    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    public enum ProgramType {
        PROGRAM, COURSE, SEMINAR, WORKSHOP
    }
}
