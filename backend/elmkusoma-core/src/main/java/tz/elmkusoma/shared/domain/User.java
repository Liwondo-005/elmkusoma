package tz.elmkusoma.shared.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "learning_level")
    @Enumerated(EnumType.STRING)
    private LearningLevel learningLevel;

    @Column(name = "secondary_stage")
    @Enumerated(EnumType.STRING)
    private SecondaryStage secondaryStage;

    @Column(name = "form")
    @Enumerated(EnumType.STRING)
    private Form form;

    @Column(name = "region_id")
    private java.util.UUID regionId;

    @Column(name = "district_id")
    private java.util.UUID districtId;

    @Column(name = "institution_id")
    private java.util.UUID institutionId;

    public Role getRole() {
        return role;
    }

    public enum Role {
        STUDENT,
        TEACHER,
        PARENT,
        OTHER_LEARNER,
        LEARNER,
        PROVIDER_ADMIN,
        PROVIDER_STAFF,
        ADMIN,
        INSTITUTION_ADMIN,
        NATIONAL_ADMIN,
        REGIONAL_ADMIN,
        DISTRICT_ADMIN,
        INSTRUCTOR
    }

    public enum LearningLevel {
        NURSERY,
        PRIMARY,
        SECONDARY,
        COLLEGE,
        UNIVERSITY
    }

    public enum SecondaryStage {
        O_LEVEL,
        A_LEVEL
    }

    public enum Form {
        FORM_1,
        FORM_2,
        FORM_3,
        FORM_4,
        FORM_5,
        FORM_6
    }

    public String getFullName() {
        if (middleName != null && !middleName.isBlank()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }
}
