package tz.elmkusoma.shared.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NaturalId;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "institutions", indexes = {
    @Index(name = "idx_institutions_code", columnList = "code", unique = true),
    @Index(name = "idx_institutions_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institution extends BaseEntity {

    @NaturalId
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "official_name")
    private String officialName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(nullable = false)
    private String address;

    @Column(name = "postal_code")
    private String postalCode;

    private String city;

    private String region;

    @Column(nullable = false)
    private String country;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstitutionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstitutionStatus status;

    public enum InstitutionType {
        NURSERY, PRIMARY, SECONDARY, COLLEGE, VETA, UNIVERSITY, MIXED
    }

    public enum InstitutionStatus {
        ACTIVE, SUSPENDED, ONBOARDED, PENDING_SETUP
    }
}
