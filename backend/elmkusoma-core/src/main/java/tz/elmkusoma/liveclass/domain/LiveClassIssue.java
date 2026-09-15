package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

@Entity
@Table(name = "live_class_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveClassIssue extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private java.util.UUID liveClassId;

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(name = "issue_type", nullable = false)
    private String issueType;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "severity")
    @Builder.Default
    private String severity = "MEDIUM";

    @Column(name = "status")
    @Builder.Default
    private String status = "OPEN";
}
