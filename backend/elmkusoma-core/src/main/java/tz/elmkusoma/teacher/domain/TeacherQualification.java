package tz.elmkusoma.teacher.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "teacher_qualifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherQualification extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "qualification_name", nullable = false)
    private String qualificationName;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Column(name = "field_of_study")
    private String fieldOfStudy;

    @Column(name = "year_obtained")
    private Integer yearObtained;

    @Column(name = "certificate_url")
    private String certificateUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    private Teacher teacher;
}
