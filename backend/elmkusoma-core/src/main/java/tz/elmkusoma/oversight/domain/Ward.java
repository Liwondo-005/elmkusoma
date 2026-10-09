package tz.elmkusoma.oversight.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Ward extends BaseEntity {

    @Column(name = "district_id", nullable = false)
    private java.util.UUID districtId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
