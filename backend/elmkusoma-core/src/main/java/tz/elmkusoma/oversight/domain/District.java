package tz.elmkusoma.oversight.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

@Entity
@Table(name = "districts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "region_id", nullable = false)
    private java.util.UUID regionId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
