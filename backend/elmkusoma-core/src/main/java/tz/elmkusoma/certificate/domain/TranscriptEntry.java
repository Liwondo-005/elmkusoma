package tz.elmkusoma.certificate.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transcript_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptEntry extends BaseEntity {

    @Column(name = "transcript_id", nullable = false)
    private UUID transcriptId;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "subject_code")
    private String subjectCode;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "grade")
    private String grade;

    @Column(name = "remarks")
    private String remarks;
}
